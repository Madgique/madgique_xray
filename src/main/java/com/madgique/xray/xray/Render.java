package com.madgique.xray.xray;

import com.madgique.xray.Configuration;
import com.madgique.xray.reference.block.BlockInfo;
import com.madgique.xray.utils.Utils;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Render
{
    // Written from several threads (scan executor, network thread via checkBlock),
    // iterated by the render thread. CopyOnWriteArrayList hands every iteration a
    // stable snapshot, so a concurrent add/remove/swap can never throw a
    // ConcurrentModificationException. The scan swaps the whole reference instead
    // of mutating in place; volatile makes that swap visible at once.
    public static List<BlockInfo> ores = new CopyOnWriteArrayList<>();

    private static final int GL_FRONT_AND_BACK = 1032;
    private static final int GL_LINE = 6913;
    private static final int GL_FILL = 6914;
    private static final int GL_LINES = 1;
    private static final int GL_QUADS = 7;

	public static void drawOres( float playerX, float playerY, float playerZ )
	{
		final List<BlockInfo> snapshot = ores; // stable for the whole frame, even across a rescan

		if ( snapshot.isEmpty() )
			return; // nothing to draw, skip the GL state churn

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Profile.BLOCKS.apply(); // Sets GL state for block drawing

        buffer.setTranslation( -playerX, -playerY, -playerZ );

        // Pass 1: translucent fill so highlighted blocks are easier to spot at distance
        if ( Configuration.blockFillOpacity > 0 )
        {
            GlStateManager.disableCull();
            buffer.begin( GL_QUADS, DefaultVertexFormats.POSITION_COLOR );
            for ( BlockInfo b : snapshot )
                Utils.renderBlockFill( buffer, b, (int)( b.alpha * Configuration.blockFillOpacity ) );
            tessellator.draw();
            GlStateManager.enableCull();
        }

        // Pass 2: dark border under the colored outlines for contrast. The border
        // cube is drawn slightly larger than the block, so it stays visible even
        // when the driver clamps glLineWidth to a low value.
        if ( Configuration.blackOutlineBorder )
        {
            GlStateManager.glLineWidth( (float) Math.min( Configuration.outlineThickness, 14f ) );
            buffer.begin( GL_LINES, DefaultVertexFormats.POSITION_COLOR );
            for ( BlockInfo b : snapshot )
                Utils.renderBlockBorder( buffer, b, (int) b.alpha );
            tessellator.draw();
        }

        // Pass 3: colored outlines
        GlStateManager.glLineWidth( (float) Math.min( Configuration.outlineThickness, 14f ) );
        buffer.begin( GL_LINES, DefaultVertexFormats.POSITION_COLOR );
        for ( BlockInfo b : snapshot )
            Utils.renderBlockBounding( buffer, b, (int) b.alpha );
        tessellator.draw();

        buffer.setTranslation( 0, 0, 0 );

        Profile.BLOCKS.clean();
	}

    /**
     * OpenGL Profiles used for rendering blocks and entities
     */
    private enum Profile
    {
        BLOCKS {
            @Override
            public void apply()
            {
                GlStateManager.disableTexture2D();
                GlStateManager.disableDepth();
                GlStateManager.depthMask( false );
                GlStateManager.glPolygonMode( GL_FRONT_AND_BACK, GL_LINE );
                GlStateManager.blendFunc( GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA );
                GlStateManager.enableBlend();
                GlStateManager.glLineWidth( (float) Configuration.outlineThickness );
            }

            @Override
            public void clean()
            {
                GlStateManager.glPolygonMode( GL_FRONT_AND_BACK, GL_FILL );
                GlStateManager.disableBlend();
                GlStateManager.enableDepth();
                GlStateManager.depthMask( true );
                GlStateManager.enableTexture2D();
            }
        },
        // TODO:
        ENTITIES {
            @Override
            public void apply()
            {}

            @Override
            public void clean()
            {}
        };

        private Profile() {}
        public abstract void apply();
        public abstract void clean();
    }
}
