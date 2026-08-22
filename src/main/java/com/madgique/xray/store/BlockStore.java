package com.madgique.xray.store;

import com.madgique.xray.reference.block.BlockData;
import com.madgique.xray.reference.block.SimpleBlockData;
import com.madgique.xray.utils.Utils;
import com.madgique.xray.XRay;
import com.madgique.xray.xray.Controller;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BlockStore {

    // Default blocks
    static final List<SimpleBlockData> DEFAULT_BLOCKS = new ArrayList<SimpleBlockData>() {
        {
            add( SimpleBlockData.firstOreInDictionary( "oreIron", "Iron Ore",             new int[]{170,117, 37}, false ));
            add( SimpleBlockData.firstOreInDictionary( "oreCoal", "Coal Ore",             new int[]{  0,  0,  0}, false ));
            add( SimpleBlockData.firstOreInDictionary( "oreGold", "Gold Ore",             new int[]{255,255,  0}, false ));
            add( SimpleBlockData.firstOreInDictionary( "oreRedstone", "Redstone Ore",     new int[]{255,  0,  0}, false ));
            add( SimpleBlockData.firstOreInDictionary( "oreDiamond", "Diamond Ore",       new int[]{136,136,255}, true  ));
            add( SimpleBlockData.firstOreInDictionary( "oreEmerald", "Emerald Ore",       new int[]{  0,136, 10}, true  ));
            add( SimpleBlockData.firstOreInDictionary( "oreQuartz","Nether Quart",        new int[]{ 30, 74,  0}, false ));
            add( SimpleBlockData.firstOreInDictionary( "oreLapis", "Lapis",               new int[]{  0,  0,255}, false ));
        }
    };

    private HashMap<Integer, BlockData> store = new HashMap<>();

    public void put(int key, BlockData data) {
        if (this.store.containsKey(key))
            return;

        this.store.put(key, data);
    }

    public HashMap<Integer, BlockData> getStore() {
        return this.store;
    }

    public void setStore(HashMap<Integer, BlockData> store) {
        this.store = store;
    }

    public void toggleDrawing(int key ) {
        if( !this.store.containsKey(key) )
            return;

        BlockData data = this.store.get(key);
        data.setDrawing(!data.isDrawing());
    }

    public static HashMap<Integer, BlockData> getFromSimpleBlockList(List<SimpleBlockData> simpleList)
    {
        HashMap<Integer, BlockData> blockData = new HashMap<>();

        for (SimpleBlockData e : simpleList) {
            IBlockState state = Block.getStateById(e.getStateId());
            if (state == null) {
                if( XRay.logger != null )
                    XRay.logger.info("Skipping saved entry '{}' stateId={} (state no longer exists)", e.getName(), e.getStateId());
                continue; // state no longer exists, skip it
            }

            if( !Utils.isBlockFromLoadedMod(state.getBlock()) ) {
                if( XRay.logger != null )
                    XRay.logger.info("Skipping saved entry '{}' stateId={} -> {} (ghost block from an unloaded mod)", e.getName(), e.getStateId(), state);
                continue; // ghost block injected for a world saved with more mods
            }

            if( XRay.logger != null )
                XRay.logger.info("Loaded '{}' stateId={} -> {}", e.getName(), e.getStateId(), state);

            blockData.put(
                    e.getStateId(),
                    new BlockData(
                            e.getName(),
                            e.getStateId(),
                            e.getColor(),
                            new ItemStack( state.getBlock(), 1, state.getBlock().getMetaFromState(state)),
                            e.isDrawing(),
                            e.getOrder()
                    )
            );
        }

        return blockData;
    }

}
