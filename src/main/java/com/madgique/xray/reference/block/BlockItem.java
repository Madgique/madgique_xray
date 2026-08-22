package com.madgique.xray.reference.block;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class BlockItem {

    private final IBlockState state;
    private final ItemStack item;
    private String displayName; // resolved lazily, see getDisplayName()

    public BlockItem(IBlockState state, ItemStack item) {
        this.state = state;
        this.item = item;
    }

    /**
     * Resolved on every call on purpose: FML remaps the numeric state ids when a
     * world is loaded, so an id captured too early (before joining the world)
     * would point to the wrong block once in game.
     */
    public int getStateId() {
        return Block.getStateId(state);
    }

    public IBlockState getBlockState() {
        return state;
    }

    public ItemStack getItemStack() {
        return item;
    }

    /**
     * Cached after the first successful resolution: the search list asks for it
     * on every keystroke. Some mods throw when asked for the name of a metadata
     * their item does not actually support (e.g. Thermal Cultivation's
     * BlockSoil); that must never take the GUI down, so fall back to the block's
     * registry name instead.
     */
    public String getDisplayName() {
        if( displayName != null )
            return displayName;

        try {
            displayName = item.getDisplayName();
            return displayName;
        } catch (RuntimeException e) {
            ResourceLocation name = state.getBlock().getRegistryName();
            return name != null ? name.toString() : "Unknown";
        }
    }
}
