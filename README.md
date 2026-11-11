# Connectivity GTNH

A small server-side networking compatibility mod for **Minecraft 1.7.10 / Forge 10.13.4.1614**, targeting GTNH 2.9.x.

## What it does

The first release focuses on one issue: the server-side Netty `ReadTimeoutHandler` created by `NetworkSystem` uses a hard-coded 30-second timeout. This mod replaces that value with a configurable setting.

Default: **180 seconds**.

This is intended for connections that temporarily stop delivering inbound data. It does not improve TPS, repair a broken internet connection, or prevent disconnects caused by other errors.

## Installation

1. Download the JAR from the successful **Build** workflow artifact (or a GitHub Release once one is published).
2. Put the JAR in the dedicated server's `mods` folder.
3. Start the server once and edit `config/connectivitygtnh.cfg` if needed.
4. Restart the server.

Clients do **not** need this mod to connect to a server running it. It may also be installed client-side for an integrated server.

## Configuration

The generated Forge config contains:

```properties
network {
    I:disconnectTimeoutSeconds=180
}
```

- `disconnectTimeoutSeconds`: how many seconds the server waits for inbound data before Netty closes the connection.
- Allowed range: 1–3600 seconds.
- Default: 180 seconds.

The setting affects the server's accepted TCP connections. It does not change the timeout used by a client when connecting to a remote server.

## Verification

At server startup, look for log lines similar to:

```
[ConnectivityGTNH] Server read timeout configured to 180 seconds
[ConnectivityGTNH] Patched net.minecraft.network.NetworkSystem$N ReadTimeoutHandler: 30s -> configurable timeout
```

The transformer identifies the remote TCP channel initializer by its superclass and constructor call, rather than assuming an anonymous inner-class number. The local in-memory initializer is left alone.

If the transformer cannot find the expected constructor pattern, it logs a warning and leaves the original bytecode untouched rather than risking a broken server startup.

## Build

Requires JDK 25 and the included Gradle wrapper.

```bash
./gradlew build
```

The CI workflow builds the mod and uploads the JAR as an artifact.

## GTNH 2.9.0 server smoke test

After the build, GitHub Actions downloads the official **GTNH 2.9.0 Java 17–26 server pack**, installs the reobfuscated Connectivity GTNH JAR, and boots the server on Java 21. The test succeeds only if the server reaches `Done` and the log confirms that Connectivity GTNH replaced the server-side Netty `ReadTimeoutHandler`.

The test also installs the latest `NeverEnoughBandwidthLegacy` release when it is version **1.0.3 or newer**, which is the version range documented as compatible with GTNH 2.9.0. On a manual `workflow_dispatch` run, the `test_with_neb` input can be disabled to isolate Connectivity GTNH.

This is a server startup / mod-loading smoke test. It does not connect a Minecraft client or simulate a real network interruption, so it does not prove the reconnect/timeout behaviour end to end. Logs and crash reports are uploaded as a workflow artifact for troubleshooting.

## Implementation notes

This is a focused reimplementation of Connectivity's configurable play disconnect timeout, not a full port of all Connectivity features. It uses a Forge 1.7.10 coremod transformer and ASM to modify only the remote TCP channel initializer inside `NetworkSystem`. It deliberately does not alter packet-size limits, compression, login policy, or packet handling.
