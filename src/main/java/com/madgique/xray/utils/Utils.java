package com.madgique.xray.utils;

import com.madgique.xray.reference.block.BlockInfo;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.util.Locale;

/**
 * Created by MiKeY on 29/12/17.
 */
public class Utils {

    /**
     * Shortcut to display a message to the player
     *
     * @param player Minecraft Player
     * @param message String Message
     */
    public static void sendMessage(EntityPlayerSP player, String message) {
        player.sendMessage( new TextComponentString(message) );
    }

    /**
     * Lazy function to auto fill some pars for getStateForPlacement
     *
     * @param world - mc world
     * @param player - mc player
     * @param stack - ItemStack
     * @return IBlockState from {@link Block#getStateForPlacement(World, BlockPos, EnumFacing, float, float, float, int, EntityLivingBase, EnumHand)}
     */
    public static IBlockState getStateFromPlacement(World world, EntityLivingBase player, ItemStack stack) {
        return Block.getBlockFromItem(stack.getItem()).getStateForPlacement(
            world, player.getPosition(), EnumFacing.NORTH, 0.1f, 0.1f, 0.1f, stack.getMetadata(), player, player.getActiveHand()
        );
    }

    public static int clampColor(int c)
    {
        return c < 0 ? 0 : c > 255 ? 255 : c;
    }

    /**
     * Parses an HTML colour such as {@code "#ff0000"} or {@code "ff0000"}.
     *
     * @param input the raw text, e.g. typed or pasted in a GUI text field
     * @return {@code {red, green, blue}} channels, or null when the text is not
     *         a complete 6-digit colour (fine while the user is still typing)
     */
    public static int[] parseHexColor(String input) {
        if( input == null )
            return null;

        String text = input.trim().toLowerCase(Locale.ROOT);
        if( text.startsWith("#") )
            text = text.substring(1);

        if( text.length() != 6 )
            return null;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean valid = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if( !valid )
                return null;
        }

        int rgb = Integer.parseInt(text, 16);
        return new int[] { (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF };
    }

    /**
     * Formats an RGB triplet as an HTML colour string, e.g. {@code "#ff0000"}.
     */
    public static String formatHexColor(int red, int green, int blue) {
        return String.format(Locale.ROOT, "#%02x%02x%02x",
            clampColor(red), clampColor(green), clampColor(blue));
    }

    /**
     * When a world saved with more mods than the current instance is loaded, FML
     * injects ghost blocks to preserve the numeric ids of the missing ones. These
     * blocks carry the registry name of the removed mod and must never show up in
     * any block list, as adding them would track a block that cannot exist.
     *
     * @param block the block to check
     * @return true if the block's domain belongs to Minecraft or a loaded mod
     */
    public static boolean isBlockFromLoadedMod(Block block) {
        ResourceLocation name = block.getRegistryName();
        if( name == null )
            return false;

        if( "minecraft".equals(name.getResourceDomain()) )
            return true;

        for (ModContainer mod : Loader.instance().getActiveModList()) {
            if( mod.getModId().equals(name.getResourceDomain()) )
                return true;
        }

        return false;
    }

    /**
     * Renders a bounding box around a specific block, using the block's own color.
     *
     * @param buffer render buffer
     * @param b Block Information
     * @param opacity Opacity of the outlines
     */
    public static void renderBlockBounding(BufferBuilder buffer, BlockInfo b, int opacity) {
        renderBlockBounding( buffer, b, b.color[0], b.color[1], b.color[2], opacity, 0f );
    }

    /**
     * Renders a bounding box slightly larger than the block itself, so the dark
     * border drawn underneath the colored outlines stays visible even when the
     * driver clamps {@code glLineWidth} to a low value.
     *
     * @param buffer render buffer
     * @param b Block Information
     * @param opacity Opacity of the outlines
     */
    public static void renderBlockBorder(BufferBuilder buffer, BlockInfo b, int opacity) {
        renderBlockBounding( buffer, b, 0, 0, 0, opacity, BORDER_EXPAND );
    }

    /**
     * How much each side of the dark border cube extends past the block, in blocks.
     */
    private static final float BORDER_EXPAND = 0.05f;

    /**
     * Renders a bounding box around a specific block with an explicit color,
     * optionally expanded past the block edges.
     * Could be done better and should use {@link net.minecraft.util.math.AxisAlignedBB#AxisAlignedBB(BlockPos)}
     * logically...
     *
     * @param buffer render buffer
     * @param b Block Information
     * @param red red channel of the lines
     * @param green green channel of the lines
     * @param blue blue channel of the lines
     * @param opacity Opacity of the outlines
     * @param expand How far the cube extends past each block edge, in blocks
     */
    private static void renderBlockBounding(BufferBuilder buffer, BlockInfo b, int red, int green, int blue, int opacity, float expand) {
        if( b == null )
            return;

        final float size = 1.0f + expand * 2f;
        final float x = b.getX() - expand;
        final float y = b.getY() - expand;
        final float z = b.getZ() - expand;
        // TOP
        buffer.pos(x, y + size, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y + size, z).color(red, green, blue, opacity).endVertex();

        // BOTTOM
        buffer.pos(x + size, y, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y, z).color(red, green, blue, opacity).endVertex();

        // Edge 1
        buffer.pos(x + size, y, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + size).color(red, green, blue, opacity).endVertex();

        // Edge 2
        buffer.pos(x + size, y, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z).color(red, green, blue, opacity).endVertex();

        // Edge 3
        buffer.pos(x, y, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y + size, z + size).color(red, green, blue, opacity).endVertex();

        // Edge 4
        buffer.pos(x, y, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y + size, z).color(red, green, blue, opacity).endVertex();
    }

    /**
     * Renders the six faces of a block as translucent quads.
     * Face culling is expected to be disabled while this is drawn (see Render.Profile.BLOCKS).
     *
     * @param buffer render buffer
     * @param b Block Information
     * @param opacity Opacity of the fill
     */
    public static void renderBlockFill(BufferBuilder buffer, BlockInfo b, int opacity) {
        if( b == null )
            return;

        final float size = 1.0f;
        int red = b.color[0];
        int green = b.color[1];
        int blue = b.color[2];
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();

        // TOP
        buffer.pos(x, y + size, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y + size, z + size).color(red, green, blue, opacity).endVertex();

        // BOTTOM
        buffer.pos(x, y, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y, z).color(red, green, blue, opacity).endVertex();

        // NORTH (z)
        buffer.pos(x, y, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y + size, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y, z).color(red, green, blue, opacity).endVertex();

        // SOUTH (z + size)
        buffer.pos(x, y, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y + size, z + size).color(red, green, blue, opacity).endVertex();

        // WEST (x)
        buffer.pos(x, y, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y + size, z).color(red, green, blue, opacity).endVertex();

        // EAST (x + size)
        buffer.pos(x, y, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y + size, z + size).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + size, y, z + size).color(red, green, blue, opacity).endVertex();
    }
}
