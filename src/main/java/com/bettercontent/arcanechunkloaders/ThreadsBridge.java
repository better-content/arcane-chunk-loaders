package com.bettercontent.arcanechunkloaders;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Optional Threads bridge for one physical anchor identity. */
public final class ThreadsBridge {
    private ThreadsBridge() {}

    public static void placed(ServerPlayer player, UUID anchorId) { emit(player, "arcane_anchor", "placed", anchorId.toString()); }
    public static void ticketVerified(ServerPlayer player, UUID anchorId) { emit(player, "arcane_anchor", "ticket_verified", anchorId.toString()); }

    private static void emit(ServerPlayer player, String type, String value, String token) {
        try {
            Class<?> api = Class.forName("com.bettercontent.threads.api.ThreadSignals");
            api.getMethod("emit", ServerPlayer.class, String.class, String.class, String.class)
                    .invoke(null, player, type, value, token);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
