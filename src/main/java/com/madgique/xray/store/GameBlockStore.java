package com.madgique.xray.store;

import com.madgique.xray.XRay;
import com.madgique.xray.reference.block.BlockItem;
import com.madgique.xray.utils.Utils;
import com.madgique.xray.xray.Controller;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;

public class GameBlockStore {

    private ArrayList<BlockItem> store = new ArrayList<>();

    /**
     * This method is used to fill the store as we do not intend to update this after
     * it has been populated, it's a singleton by nature but we still need some
     * amount of control over when it is populated.
     */
    public void populate()
    {
        // Avoid doing the logic again unless repopulate is called
        if( this.store.size() != 0 )
            return;

        for ( Block block : ForgeRegistries.BLOCKS ) {

            if( Controller.blackList.contains(block) )
                continue;

            // Skip ghost blocks injected by FML to preserve ids from worlds
            // saved with mods that are no longer loaded (e.g. old modpacks)
            if( !Utils.isBlockFromLoadedMod(block) )
                continue;

            // Blocks without an ItemBlock cannot be picked or rendered in the GUI list
            Item item = Item.getItemFromBlock(block);
            if( !(item instanceof ItemBlock) )
                continue;

            // One entry per exact state so every variant (metadata and properties)
            // can be tracked individually, matching the runtime stateId lookup.
            for ( IBlockState state : block.getBlockState().getValidStates() ) {
                ItemStack stack = new ItemStack(item, 1, block.getMetaFromState(state));

                // Some mods throw when asked for the name of a metadata their item
                // does not actually support (e.g. Thermal Cultivation's BlockSoil);
                // such variants have no real display name, so drop them entirely.
                try {
                    stack.getDisplayName();
                } catch (RuntimeException e) {
                    continue;
                }

                store.add(new BlockItem(state, stack));
            }
        }

        if( XRay.logger != null )
            XRay.logger.info("Populated game block store with {} states", store.size());
    }

    public void repopulate()
    {
        this.store.clear();
        this.populate();
    }

    public ArrayList<BlockItem> getStore() {
        return this.store;
    }
}
