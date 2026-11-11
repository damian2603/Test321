package com.byqo.connectivitygtnh.core;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * Replaces the literal 30 passed to ReadTimeoutHandler in the dedicated
 * server's TCP channel initializer. This leaves the client-side timeout alone.
 */
public final class NetworkTimeoutTransformer implements IClassTransformer {

    private static final String TARGET_CLASS = "net.minecraft.network.NetworkSystem$1";
    private static final String READ_TIMEOUT_HANDLER =
        "io/netty/handler/timeout/ReadTimeoutHandler";
    private static final String CONFIG_CLASS =
        "com/byqo/connectivitygtnh/TimeoutConfig";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        String targetName = transformedName == null ? name : transformedName;
        if (!TARGET_CLASS.equals(targetName)) {
            return basicClass;
        }

        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);

            int constructorMatches = 0;
            int patchedInstructions = 0;

            for (MethodNode method : classNode.methods) {
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
                                false
                            )
                        );
                        patchedInstructions++;
                    }
                }
            }

            if (constructorMatches != 1 || patchedInstructions != 1) {
                System.err.println(
                    "[ConnectivityGTNH] WARNING: expected one ReadTimeoutHandler(int) "
                    + "constructor with a 30-second literal in " + TARGET_CLASS
                    + ", found constructors=" + constructorMatches
                    + ", patched=" + patchedInstructions
                    + ". Leaving original bytecode unchanged."
                );
                return basicClass;
            }

            ClassWriter writer = new ClassWriter(reader, 0);
            classNode.accept(writer);

            System.out.println(
                "[ConnectivityGTNH] Patched NetworkSystem$1 ReadTimeoutHandler: "
                + "30s -> configurable timeout"
            );
            return writer.toByteArray();
        } catch (Throwable error) {
            System.err.println(
                "[ConnectivityGTNH] ERROR while patching " + TARGET_CLASS
                + "; original bytecode will be used."
            );
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
            return constant instanceof Integer
                && ((Integer) constant).intValue() == expected;
        }

        return false;
    }
}
