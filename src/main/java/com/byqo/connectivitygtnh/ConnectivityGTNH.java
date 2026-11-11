package com.byqo.connectivitygtnh;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = ConnectivityGTNH.MOD_ID,
    name = ConnectivityGTNH.MOD_NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*"
)
public final class ConnectivityGTNH {

    public static final String MOD_ID = "connectivitygtnh";
    public static final String MOD_NAME = "Connectivity GTNH";

    @Mod.EventHandler
    public void preInitialize(FMLPreInitializationEvent event) {
        TimeoutConfig.load(event.getSuggestedConfigurationFile());
    }
}
