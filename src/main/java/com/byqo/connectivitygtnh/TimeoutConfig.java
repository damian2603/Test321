package com.byqo.connectivitygtnh;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runtime configuration read by the coremod-injected timeout accessor.
 */
public final class TimeoutConfig {

    private static final Logger LOG = LogManager.getLogger(ConnectivityGTNH.MOD_ID);
    private static final int DEFAULT_TIMEOUT_SECONDS = 180;
    private static volatile int disconnectTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    private TimeoutConfig() {}

    public static void load(File file) {
        Configuration configuration = new Configuration(file);
        configuration.load();

        disconnectTimeoutSeconds = configuration.getInt(
            "disconnectTimeoutSeconds",
            "network",
            DEFAULT_TIMEOUT_SECONDS,
            1,
            3600,
            "Server-side Netty read timeout, in seconds. Default: 180.");

        if (configuration.hasChanged()) {
            configuration.save();
        }

        LOG.info(
            "[ConnectivityGTNH] Server read timeout configured to {} seconds",
            Integer.valueOf(disconnectTimeoutSeconds));
    }

    /**
     * Called by transformed NetworkSystem$1 bytecode. Keep this method static
     * and free of side effects: Netty invokes it while creating a channel.
     */
    public static int getDisconnectTimeoutSeconds() {
        return disconnectTimeoutSeconds;
    }
}
