package com.bettercontent.realisticplacementpreview.placementpreview;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.FluidState;
import xbigellx.rbp.internal.level.RBPLevel;
import xbigellx.rbp.internal.physics.BlockPhysicsHandler;
import xbigellx.rbp.internal.physics.BlockProcessingDetail;
import xbigellx.rbp.internal.physics.BlockProcessingOptions;
import xbigellx.rbp.internal.physics.ProcessedBlockOperation;
import xbigellx.realisticphysics.RealisticPhysics;
import xbigellx.realisticphysics.internal.level.LevelPhysics;
import xbigellx.realisticphysics.internal.level.RPDimensionType;
import xbigellx.realisticphysics.internal.level.RPLevelAccessor;
import xbigellx.realisticphysics.internal.level.block.BlockDefinition;
import xbigellx.realisticphysics.internal.level.block.RPBlockContext;
import xbigellx.realisticphysics.internal.level.chunk.ChunkPriority;
import xbigellx.realisticphysics.internal.level.chunk.RPChunkAccessor;
import xbigellx.realisticphysics.internal.level.chunk.RPChunkSectionAccessor;
import xbigellx.realisticphysics.internal.physics.task.PhysicsTask;
import xbigellx.realisticphysics.internal.physics.task.PriorityPhysicsTask;
import xbigellx.realisticphysics.internal.physics.task.TaskManagerAccessor;
import xbigellx.realisticphysics.internal.physics.task.TaskPriority;

final class RbpSupportPreviewAdapter {
    private static final BlockProcessingOptions OPTIONS =
            new BlockProcessingOptions(BlockProcessingDetail.NORMAL);

    private RbpSupportPreviewAdapter() {
    }

    static SupportPreviewVerdict evaluate(final ServerLevel level, final PlacementProposal proposal) {
        try {
            if (RealisticPhysics.physicsManager() == null
                    || RealisticPhysics.physicsManager().findLevelPhysics(level).isEmpty()) {
                return SupportPreviewVerdict.UNKNOWN;
            }
            final BlockPhysicsHandler handler = RealisticPhysics.physicsManager()
                    .findPhysicsHandler(BlockPhysicsHandler.class, level)
                    .orElse(null);
            if (handler == null || !(handler.getLevel() instanceof RBPLevel liveLevel)) {
                return SupportPreviewVerdict.UNKNOWN;
            }
            final OverlayLevel overlay = new OverlayLevel(liveLevel, proposal.states());
            final RBPLevel virtualLevel = new RBPLevel(level, overlay, liveLevel.physics());

            for (final Map.Entry<BlockPos, BlockState> entry : proposal.states().entrySet()) {
                final RPBlockContext context = overlay.context(entry.getKey());
                if (!context.hasBlockDefinition()) {
                    return SupportPreviewVerdict.UNMANAGED;
                }
                final AtomicBoolean crushed = new AtomicBoolean();
                final ProcessedBlockOperation operation = virtualLevel.physics().physicsEngine().processBlock(
                        virtualLevel,
                        context,
                        OPTIONS,
                        processed -> {
                            if (processed.operation() == ProcessedBlockOperation.CRUSH) {
                                crushed.set(true);
                            }
                        });
                if (operation == ProcessedBlockOperation.FALL
                        || operation == ProcessedBlockOperation.CRUSH
                        || crushed.get()) {
                    return SupportPreviewVerdict.WILL_FALL;
                }
            }
            return SupportPreviewVerdict.SUPPORTED;
        } catch (final RuntimeException | LinkageError exception) {
            return SupportPreviewVerdict.UNKNOWN;
        }
    }

    static final class OverlayLevel implements RPLevelAccessor {
        private final RBPLevel delegate;
        private final Map<BlockPos, BlockState> states;
        private final Map<ChunkPos, OverlayChunk> chunks = new HashMap<>();
        private final TaskManagerAccessor readOnlyTasks = new ReadOnlyTaskManager();

        OverlayLevel(final RBPLevel delegate, final Map<BlockPos, BlockState> states) {
            this.delegate = delegate;
            this.states = Map.copyOf(states);
        }

        RPBlockContext context(final BlockPos pos) {
            final BlockState state = getBlockState(pos);
            BlockDefinition definition = physics().fluidDefinitions().get(state);
            if (definition == null) {
                definition = physics().blockDefinitions().get(state);
            }
            return new RPBlockContext(pos.immutable(), state, definition);
        }

        @Override
        public LevelPhysics physics() {
            return delegate.physics();
        }

        @Override
        public FluidState getFluidState(final BlockPos pos) {
            final BlockState state = states.get(pos);
            return state == null ? delegate.getFluidState(pos) : state.getFluidState();
        }

        @Override
        public BlockState getBlockState(final BlockPos pos) {
            final BlockState state = states.get(pos);
            return state == null ? delegate.getBlockState(pos) : state;
        }

        @Override
        public BlockEntity getBlockEntity(final BlockPos pos) {
            return states.containsKey(pos) ? null : delegate.getBlockEntity(pos);
        }

        @Override
        public RPBlockContext getBlockContext(final BlockPos pos) {
            return context(pos);
        }

        @Override
        public boolean chunkExists(final ChunkPos pos) {
            return delegate.chunkExists(pos);
        }

        @Override
        public RPChunkAccessor getChunk(final ChunkPos pos) {
            return chunks.computeIfAbsent(pos, key -> new OverlayChunk(delegate.getChunk(key), this));
        }

        @Override
        public RPChunkAccessor getChunk(final BlockPos pos) {
            return getChunk(new ChunkPos(pos));
        }

        @Override
        public TaskManagerAccessor taskManager() {
            return readOnlyTasks;
        }

        @Override
        public List<? extends Player> players() {
            return delegate.players();
        }

        @Override
        public RandomSource getRandom() {
            return delegate.getRandom();
        }

        @Override
        public RPDimensionType dimensionType() {
            return delegate.dimensionType();
        }

        @Override
        public long getGameTime() {
            return delegate.getGameTime();
        }

        @Override
        public int getMinSection() {
            return delegate.getMinSection();
        }

        @Override
        public int getMaxSection() {
            return delegate.getMaxSection();
        }

        @Override
        public int getSectionsCount() {
            return delegate.getSectionsCount();
        }

        @Override
        public boolean isUnloaded() {
            return delegate.isUnloaded();
        }
    }

    private record OverlayChunk(RPChunkAccessor delegate, OverlayLevel level) implements RPChunkAccessor {
        @Override
        public ChunkPos pos() {
            return delegate.pos();
        }

        @Override
        public BlockState getBlockState(final BlockPos pos) {
            return level.getBlockState(pos);
        }

        @Override
        public RPBlockContext getBlockContext(final BlockPos pos) {
            return level.context(pos);
        }

        @Override
        public int getSectionsCount() {
            return delegate.getSectionsCount();
        }

        @Override
        public int getMinSection() {
            return delegate.getMinSection();
        }

        @Override
        public int getMaxSection() {
            return delegate.getMaxSection();
        }

        @Override
        public RPChunkSectionAccessor getSection(final int index) {
            return delegate.getSection(index);
        }

        @Override
        public int getSectionIndexFromSectionY(final int sectionY) {
            return delegate.getSectionIndexFromSectionY(sectionY);
        }

        @Override
        public int getSectionYFromSectionIndex(final int index) {
            return delegate.getSectionYFromSectionIndex(index);
        }

        @Override
        public int getSectionIndex(final BlockPos pos) {
            return delegate.getSectionIndex(pos);
        }

        @Override
        public ChunkAccess getChunk() {
            return delegate.getChunk();
        }
    }

    private static final class ReadOnlyTaskManager implements TaskManagerAccessor {
        @Override
        public void addTask(final PhysicsTask task, final TaskPriority priority) {
            throw new IllegalStateException("Support preview attempted to schedule a physics task");
        }

        @Override
        public boolean containsTask(final PhysicsTask task) {
            return false;
        }

        @Override
        public PriorityPhysicsTask poll(final Player player, final ChunkPos pos, final boolean force) {
            return null;
        }

        @Override
        public PriorityPhysicsTask poll(final Player player, final ChunkPos pos) {
            return null;
        }

        @Override
        public int totalQueuedTasks(final ChunkPriority priority) {
            return 0;
        }

        @Override
        public int totalQueuedTasks(final ChunkPos pos) {
            return 0;
        }

        @Override
        public int totalQueuedTasks() {
            return 0;
        }
    }
}
