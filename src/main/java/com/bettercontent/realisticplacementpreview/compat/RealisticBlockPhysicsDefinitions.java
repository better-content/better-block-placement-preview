package com.bettercontent.realisticplacementpreview.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class RealisticBlockPhysicsDefinitions {
    private RealisticBlockPhysicsDefinitions() {
    }

    public static Set<String> registeredEntries(final Set<String> entries) {
        return entries.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(RealisticBlockPhysicsDefinitions::isRegisteredEntry)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static Set<BlockState> supportedStates(final Set<BlockState> states) {
        return states.stream()
                .filter(RealisticBlockPhysicsDefinitions::isSupportedState)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static boolean isSupportedState(final BlockState state) {
        return state != null
                && state.getBlock() != Blocks.AIR
                && !state.canBeReplaced()
                && !state.liquid()
                && !(state.getBlock() instanceof BushBlock)
                && !(state.getBlock() instanceof FlowerBlock);
    }

    private static boolean isRegisteredEntry(final String entry) {
        if (entry.startsWith("#")) {
            final ResourceLocation tag = ResourceLocation.tryParse(entry.substring(1));
            return tag != null && ForgeRegistries.BLOCKS.tags() != null
                    && ForgeRegistries.BLOCKS.tags().getTagNames().anyMatch(key -> key.location().equals(tag));
        }
        if (entry.startsWith("<") && entry.endsWith(">")) {
            final String namespace = entry.substring(1, entry.length() - 1);
            return ForgeRegistries.BLOCKS.getKeys().stream()
                    .anyMatch(key -> key.getNamespace().equals(namespace));
        }
        final ResourceLocation id = ResourceLocation.tryParse(entry);
        return id != null && ForgeRegistries.BLOCKS.containsKey(id);
    }
}
