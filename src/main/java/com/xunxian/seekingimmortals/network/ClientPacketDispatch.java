package com.xunxian.seekingimmortals.network;

/**
 * Reflective bridge so common packet classes never put client Screen types in their
 * constant pool. Dedicated servers load network/* during CONSTRUCT; any CONSTANT_Class
 * pointing at Screen (even inside lambdas) can fail RuntimeDistCleaner.
 */
public final class ClientPacketDispatch {
    private static final String HANDLERS = "com.xunxian.seekingimmortals.client.ClientPacketHandlers";

    private ClientPacketDispatch() {}

    public static void invoke(String methodName, Object packet) {
        try {
            Class<?> handlers = Class.forName(HANDLERS);
            handlers.getMethod(methodName, packet.getClass()).invoke(null, packet);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("client packet dispatch failed: " + methodName, exception);
        }
    }
}
