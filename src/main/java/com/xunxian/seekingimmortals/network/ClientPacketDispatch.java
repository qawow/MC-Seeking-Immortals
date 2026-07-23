package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;

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
            // 优雅降级：处理器缺失/签名漂移时记录日志而非崩溃客户端，避免收包即闪退
            SeekingImmortalsMod.LOGGER.warn("Client packet dispatch failed for {} ({}): {}",
                    methodName, packet.getClass().getSimpleName(), exception.toString());
        }
    }
}
