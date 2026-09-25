package com.bettercontent.realisticplacementpreview.mixin.rbp;

import com.bettercontent.realisticplacementpreview.compat.RealisticBlockPhysicsDefinitions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Mixin(targets = "xbigellx.rbp.internal.level.block.RBPBlockDefinitionCatalogFactory", remap = false)
public abstract class BlockDefinitionCatalogFactoryMixin {
    @WrapOperation(
            method = "constructBlockDefinitions(Lxbigellx/rbp/internal/config/WorldDefinitionConfig$Model;Ljava/util/HashMap;Ljava/util/Set;)Lxbigellx/realisticphysics/internal/level/block/BlockDefinitionCatalog;",
            at = @At(
                    value = "INVOKE",
                    target = "Lxbigellx/realisticphysics/internal/util/BlockResolver;resolveStates(Ljava/util/Set;)Ljava/util/Set;",
                    remap = false
            ),
            require = 1,
            remap = false
    )
    private Set<BlockState> betterContentFixes$resolveUsableBlocks(
            final Set<String> entries,
            final Operation<Set<BlockState>> original
    ) {
        final Set<String> registeredEntries = RealisticBlockPhysicsDefinitions.registeredEntries(entries);
        return RealisticBlockPhysicsDefinitions.supportedStates(original.call(registeredEntries));
    }
}
