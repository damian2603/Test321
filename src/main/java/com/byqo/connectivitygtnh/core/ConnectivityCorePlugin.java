package com.byqo.connectivitygtnh.core;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("ConnectivityGTNH")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({
    "com.byqo.connectivitygtnh.core"
})
public final class ConnectivityCorePlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
            "com.byqo.connectivitygtnh.core.NetworkTimeoutTransformer"
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // No launch-time configuration is needed. Forge config is loaded in preInit.
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
