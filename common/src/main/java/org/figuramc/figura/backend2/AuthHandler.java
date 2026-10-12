package org.figuramc.figura.backend2;

import org.figuramc.figura.FiguraMod;

/**
 * Authentication for the self-hosted backend.
 *
 * <p>Instead of the Mojang {@code joinServer} handshake, the client authenticates with a
 * one-time session token that the server plugin pushes over the trusted {@code figura:auth}
 * plugin-messaging channel when the player joins. The plugin maps that token back to the
 * player's server UUID, so a valid token <em>is</em> the identity &mdash; no Mojang, no
 * Ely.By, no UUID drift on offline servers.</p>
 */
public class AuthHandler {

    /** Token delivered by the server plugin (Fabric receiver). Cleared on disconnect. */
    public static volatile String pluginToken;

    public static void auth(boolean reAuth) {
        NetworkStuff.async(() -> {
            if (!reAuth && NetworkStuff.isConnected())
                return;

            String token = pluginToken;
            if (token == null || token.isEmpty()) {
                // Not on our server (or the token hasn't arrived yet). Stay idle - the
                // figura:auth receiver triggers (re)auth the moment a token shows up.
                FiguraMod.debug("No Figura session token yet - waiting for the server plugin (figura:auth)");
                NetworkStuff.backendStatus = 1;
                return;
            }

            FiguraMod.LOGGER.info("Authenticating with the " + FiguraMod.MOD_NAME + " backend using the server session token");
            NetworkStuff.backendStatus = 2;
            NetworkStuff.authSuccess(token);
        });
    }
}
