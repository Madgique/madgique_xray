package com.madgique.xray.xray;

import com.madgique.xray.Configuration;
import com.madgique.xray.XRay;
import com.madgique.xray.reference.block.BlockData;
import com.madgique.xray.reference.block.BlockInfo;
import com.madgique.xray.utils.WorldRegion;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class RenderEnqueue implements Runnable
{
	private final WorldRegion box;

	public RenderEnqueue(WorldRegion region )
	{
		box = region;
	}

	@Override
	public void run() // Our thread code for finding ores near the player.
	{
		blockFinder();
	}

	/**
	 * Use Controller.requestBlockFinder() to trigger a scan.
	 */
	private void blockFinder() {
        HashMap<Integer, BlockData> blocks = Controller.getBlockStore().getStore();

		if ( blocks.isEmpty() ) {
		    if( !Render.ores.isEmpty() )
		        Render.ores.clear();
            return; // no need to scan the region if there's nothing to find
        }

		final World world = XRay.mc.world;
		final List<BlockInfo> renderQueue = new ArrayList<>();

		int lowBoundX, highBoundX, lowBoundY, highBoundY, lowBoundZ, highBoundZ;

		// Used for cleaning up the searching process
		IBlockState currentState;
		BlockData blockData;

		// Loop on chunks (x, z)
		for ( int chunkX = box.minChunkX; chunkX <= box.maxChunkX; chunkX++ )
		{
			// Pre-compute the extend bounds on X
			int x = chunkX << 4; // lowest x coord of the chunk in block/world coordinates
			lowBoundX = (x < box.minX) ? box.minX - x : 0; // lower bound for x within the extend
			highBoundX = (x + 15 > box.maxX) ? box.maxX - x : 15;// and higher bound. Basically, we clamp it to fit the radius.

			for ( int chunkZ = box.minChunkZ; chunkZ <= box.maxChunkZ; chunkZ++ )
			{
				// Time to getStore the chunk (16x256x16) and split it into 16 vertical extends (16x16x16)
				Chunk chunk = world.getChunkFromChunkCoords( chunkX, chunkZ );
				if (!chunk.isLoaded()) {
					continue; // We won't find anything interesting in unloaded chunks
				}
				ExtendedBlockStorage[] extendsList = chunk.getBlockStorageArray();

				// Pre-compute the extend bounds on Z
				int z = chunkZ << 4;
				lowBoundZ = (z < box.minZ) ? box.minZ - z : 0;
				highBoundZ = (z + 15 > box.maxZ) ? box.maxZ - z : 15;

				// Loop on the extends around the player's layer (6 down, 2 up)
				for ( int curExtend = box.minChunkY; curExtend <= box.maxChunkY; curExtend++ )
				{
					ExtendedBlockStorage ebs = extendsList[curExtend];
					if (ebs == null) // happens quite often!
						continue;

					// Pre-compute the extend bounds on Y
					int y = curExtend << 4;
					lowBoundY = (y < box.minY) ? box.minY - y : 0;
					highBoundY = (y + 15 > box.maxY) ? box.maxY - y : 15;

					// Now that we have an extend, let's check all its blocks
					for ( int i = lowBoundX; i <= highBoundX; i++ ) {
						for ( int j = lowBoundY; j <= highBoundY; j++ ) {
							for ( int k = lowBoundZ; k <= highBoundZ; k++ ) {
								currentState = ebs.get(i, j, k);

								// Reject blacklisted blocks
								if( Controller.blackList.contains(currentState.getBlock()) )
									continue;

								// Exact state match only: each block variant (metadata) is a separate entry,
								// no fallback to the default state or variants would all highlight together.
								blockData = blocks.get( Block.getStateId(currentState) );
								if( blockData == null || !blockData.isDrawing() ) // fail safe
									continue;

								// Calculate distance from player to block. Fade out futher away blocks
								double alpha = !Configuration.shouldFade ? 255 : Math.max(0, ((Controller.getRadius() - XRay.mc.player.getDistance(x + i, y + j, z + k)) / Controller.getRadius() ) * 255);

								// Push the block to the render queue
								renderQueue.add(new BlockInfo(x + i, y + j, z + k, blockData.getColor().getColor(), alpha));
							}
						}
					}
				}
			}
		}
		final BlockPos playerPos = XRay.mc.player.getPosition();
		renderQueue.sort((t, t1) -> Double.compare(t1.distanceSq(playerPos), t.distanceSq(playerPos)));
		// Atomic reference swap: the render thread keeps iterating the previous
		// snapshot until it picks up this one, no in-place clear+addAll race.
		Render.ores = new CopyOnWriteArrayList<>( renderQueue );
	}

	/**
	 * Single-block version of blockFinder. Can safely be called directly
	 * for quick block check.
	 * @param pos the BlockPos to check
	 * @param state the current state of the block
	 * @param add true if the block was added to world, false if it was removed
	 */
	public static void checkBlock( BlockPos pos, IBlockState state, boolean add )
	{
		if ( !Controller.drawOres() || Controller.getBlockStore().getStore().isEmpty() )
		    return; // just pass

		// Exact state match only: see blockFinder()
		BlockData data = Controller.getBlockStore().getStore().get( Block.getStateId(state) );

		if ( data == null ) // it's not a block we are monitoring
			return;

	    if( !add )
	    {
            Render.ores.remove( new BlockInfo(pos, null, 0.0) );
            return;
        }

		double alpha = !Configuration.shouldFade ? 255 : Math.max(0, ((Controller.getRadius() - XRay.mc.player.getDistance(pos.getX(), pos.getY(), pos.getZ())) / Controller.getRadius() ) * 255);

        // the block was added to the world, let's add it to the drawing buffer
        Render.ores.add( new BlockInfo(pos, data.getColor().getColor(), alpha) );
	}
}
