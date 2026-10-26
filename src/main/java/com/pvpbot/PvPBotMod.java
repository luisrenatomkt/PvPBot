package com.pvpbot;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

@Mod(modid = PvPBotMod.MODID, name = "PvP Bot Trainer", version = "1.1.0", acceptedMinecraftVersions = "[1.8.9]")
public class PvPBotMod {
    public static final String MODID = "pvpbot";

    @Mod.Instance(MODID)
    public static PvPBotMod instance;

    @SidedProxy(clientSide = "com.pvpbot.ClientProxy", serverSide = "com.pvpbot.CommonProxy")
    public static CommonProxy proxy;

    /** Padroes usados por /pvpbot spawn. */
    public static Difficulty defaultDifficulty = Difficulty.INTERMEDIARIO;
    public static String defaultArmor = "diamond";
    public static String defaultSword = "diamond";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        EntityRegistry.registerModEntity(EntityPvPBot.class, "PvPBot", 0, this, 64, 1, true);
        proxy.registerRenderers();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandPvPBot());
    }
}
