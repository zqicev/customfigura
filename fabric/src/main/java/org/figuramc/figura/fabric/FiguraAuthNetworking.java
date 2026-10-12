package org.figuramc.figura.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.backend2.AuthHandler;
import org.figuramc.figura.backend2.NetworkStuff;

import java.nio.charset.StandardCharsets;

/**
 * Receives the one-time backend session token the server plugin pushes over the trusted
 * {@code figura:auth} plugin-messaging channel, stores it, and (re)authenticates the Figura
 * backend with it. Replaces the Mojang {@code joinServer} handshake.
 */
public final class FiguraAuthNetworking {

    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath("figura", "auth");

    /** Custom payload carrying the raw UTF-8 token bytes sent by the Bukkit plugin. */
    public record AuthPayload(String token) implements CustomPacketPayload {
        public static final Type<AuthPayload> TYPE = new Type<>(CHANNEL);

        public static final StreamCodec<FriendlyByteBuf, AuthPayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBytes(payload.token.getBytes(StandardCharsets.UTF_8)),
                buf -> {
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    return new AuthPayload(new String(bytes, StandardCharsets.UTF_8));
                }
        );

        @Override
        public Type<AuthPayload> type() {
            return TYPE;
        }
    }

    public static void init() {
        // Register the play-phase S2C payload type and a receiver for it.
        PayloadTypeRegistry.playS2C().register(AuthPayload.TYPE, AuthPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(AuthPayload.TYPE, (payload, context) -> {
            String token = payload.token();
            context.client().execute(() -> {
                if (token == null || token.isEmpty()) return;
                boolean changed = !token.equals(AuthHandler.pluginToken);
                AuthHandler.pluginToken = token;
                FiguraMod.LOGGER.info("Received Figura session token from server plugin (len=" + token.length() + ")");
                if (changed) {
                    // New token -> (re)connect with it. Flag "connecting" right away: the plugin
                    // repeats the token, and a repeat must not start a second connection.
                    NetworkStuff.backendStatus = 2;
                    NetworkStuff.reAuth();
                } else if (NetworkStuff.backendStatus == 1) {
                    NetworkStuff.auth();             // same token, idle -> connect
                }
            });
        });

        // Leaving the server: drop the token so it is never reused elsewhere, and disconnect.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            AuthHandler.pluginToken = null;
            NetworkStuff.disconnect(null);
        });

        FiguraMod.LOGGER.info("Registered " + CHANNEL + " plugin-messaging receiver");
    }
}
