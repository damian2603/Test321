package com.byqo.connectivitygtnh.core;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Replaces the literal 30 passed to ReadTimeoutHandler in NetworkSystem's
 * remote TCP ChannelInitializer. The local in-memory initializer and client
 * connection timeout are left unchanged.
 */
public final class NetworkTimeoutTransformer implements IClassTransformer {

    private static final String TARGET_CLASS_PREFIX = "net.minecraft.network.NetworkSystem$";
    private static final String CHANNEL_INITIALIZER = "io/netty/channel/ChannelInitializer";
    private static final String READ_TIMEOUT_HANDLER = "io/netty/handler/timeout/ReadTimeoutHandler";
    private static final String CONFIG_CLASS = "com/byqo/connectivitygtnh/TimeoutConfig";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        String targetName = transformedName == null ? name : transformedName;
        if (targetName == null || !targetName.startsWith(TARGET_CLASS_PREFIX)) {
            return basicClass;
        }

        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);

            // NetworkSystem has more than one anonymous inner class. Match the
            // actual Netty initializer structurally rather than assuming $1.
            if (!CHANNEL_INITIALIZER.equals(classNode.superName)) {
                return basicClass;
            }

            int constructorMatches = 0;
            int patchedInstructions = 0;

            // ASM 5 (bundled with Forge 1.7.10) exposes ClassNode.methods as a raw List.
            for (Object methodObject : classNode.methods) {
                MethodNode method = (MethodNode) methodObject;
                AbstractInsnNode[] instructions = method.instructions.toArray();

                for (AbstractInsnNode instruction : instructions) {
                    if (!(instruction instanceof MethodInsnNode)) {
                        continue;
                    }

                    MethodInsnNode methodCall = (MethodInsnNode) instruction;
                    if (methodCall.getOpcode() != Opcodes.INVOKESPECIAL
                        || !READ_TIMEOUT_HANDLER.equals(methodCall.owner)
                        || !"<init>".equals(methodCall.name)
                        || !"(I)V".equals(methodCall.desc)) {
                        continue;
                    }

                    constructorMatches++;
                    AbstractInsnNode timeoutArgument = previousRealInstruction(instruction);

                    if (isIntegerConstant(timeoutArgument, 30)) {
                        method.instructions.set(
                            timeoutArgument,
                            new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                CONFIG_CLASS,
                                "getDisconnectTimeoutSeconds",
                                "()I",
                                false));
                        patchedInstructions++;
                    }
                }
            }

            // The other NetworkSystem ChannelInitializer is local-only and
            // does not install a ReadTimeoutHandler, so it is expected to have
            // no matching constructor.
            if (constructorMatches == 0) {
                return basicClass;
            }

            if (constructorMatches != 1 || patchedInstructions != 1) {
                System.err.println(
                    "[ConnectivityGTNH] WARNING: expected one ReadTimeoutHandler(int) "
                        + "constructor with a 30-second literal in "
                        + targetName
                        + ", found constructors="
                        + constructorMatches
                        + ", patched="
                        + patchedInstructions
                        + ". Leaving original bytecode unchanged.");
                return basicClass;
            }

            ClassWriter writer = new ClassWriter(reader, 0);
            classNode.accept(writer);

            System.out.println(
                "[ConnectivityGTNH] Patched " + targetName + " ReadTimeoutHandler: 30s -> configurable timeout");
            return writer.toByteArray();
        } catch (Throwable error) {
            System.err
                .println("[ConnectivityGTNH] ERROR while patching " + targetName + "; original bytecode will be used.");
            error.printStackTrace();
            return basicClass;
        }
    }

    private static AbstractInsnNode previousRealInstruction(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        return previous;
    }

    private static boolean isIntegerConstant(AbstractInsnNode instruction, int expected) {
        if (instruction instanceof IntInsnNode) {
            return ((IntInsnNode) instruction).operand == expected;
        }

        if (instruction instanceof LdcInsnNode) {
            Object constant = ((LdcInsnNode) instruction).cst;
            return constant instanceof Integer && ((Integer) constant).intValue() == expected;
        }

        return false;
    }
}
