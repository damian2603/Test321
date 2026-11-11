plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

group = "com.byqo.connectivitygtnh"
version = "0.1.0"

dependencies {
    // Forge 1.7.10 supplies ASM at runtime; this is only needed to compile the tree API.
    compileOnly("org.ow2.asm:asm-tree:5.0.3")
}
