package com.madgique.xray;

import com.madgique.xray.gui.GuiOverlay;
import com.madgique.xray.keybinding.InputEvent;
import com.madgique.xray.keybinding.KeyBindings;
import com.madgique.xray.reference.Reference;
import com.madgique.xray.reference.block.BlockData;
import com.madgique.xray.reference.block.SimpleBlockData;
import com.madgique.xray.store.BlockStore;
import com.madgique.xray.store.GameBlockStore;
import com.madgique.xray.store.JsonStore;
import com.madgique.xray.xray.Controller;
import com.madgique.xray.xray.Events;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;

@Mod(
		modid= Reference.MOD_ID,
		name= Reference.MOD_NAME,
		version=Reference.MOD_VERSION,
		updateJSON= Reference.UPDATE_JSON,

		clientSideOnly = true
)

@Mod.EventBusSubscriber(
		modid = Reference.MOD_ID
)

@SideOnly(Side.CLIENT)
public class XRay
{
	// This contains all of the games blocks to allow us to reference them
	// when needed. This allows us to avoid continually rebuilding
	public static GameBlockStore gameBlockStore = new GameBlockStore();

	public static Minecraft mc = Minecraft.getMinecraft();
	public static JsonStore blockStore = new JsonStore();

	public static Logger logger;

	@Instance(Reference.MOD_ID)
	public static XRay instance;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		logger = event.getModLog();
		logger.debug(I18n.format("xray.debug.init"));

		KeyBindings.setup();

		MinecraftForge.EVENT_BUS.register( new InputEvent() );
		MinecraftForge.EVENT_BUS.register( new Events() );
		MinecraftForge.EVENT_BUS.register( new GuiOverlay() );

		MinecraftForge.EVENT_BUS.register( this );

		List<SimpleBlockData> data = blockStore.read();
		if( data.isEmpty() )
			return;

		HashMap<Integer, BlockData> map = BlockStore.getFromSimpleBlockList(data);
		Controller.getBlockStore().setStore(map);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		gameBlockStore.populate();
	}

	@EventHandler
	public void onExit(FMLServerStoppingEvent event)
	{
		Controller.shutdownExecutor();
	}

	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if (event.getModID().equals(Reference.MOD_ID))
		{
			ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
		}
	}
}
