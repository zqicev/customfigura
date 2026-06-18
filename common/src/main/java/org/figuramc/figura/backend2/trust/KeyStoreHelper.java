package org.figuramc.figura.backend2.trust;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketError;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.backend2.websocket.FiguraWebSocketAdapter;

import java.io.IOException;

/**
 * Creates the backend WebSocket.
 *
 * <p>The self-hosted backend speaks plain {@code ws://} (no TLS), so this no longer loads
 * {@code figurakeystore.jks} or merges any certificates &mdash; it just opens an ordinary
 * unsecured socket. The {@link FiguraWebSocketAdapter} (auth, message handling) is unchanged.
 * The method name is kept so {@code NetworkStuff} needs no change.</p>
 */
public class KeyStoreHelper {

    public static WebSocket websocketWithBackendCertificates(String token) throws WebSocketException {
        FiguraMod.LOGGER.info("Initializing plain (ws://) websocket to the custom backend");
        try {
            WebSocketFactory wsFactory = new WebSocketFactory();
            WebSocket socket = wsFactory.createSocket(FiguraWebSocketAdapter.getBackendAddress());
            socket.addListener(new FiguraWebSocketAdapter(token));
            socket.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
            return socket;
        } catch (IOException e) {
            FiguraMod.LOGGER.error("Failed to create the backend websocket!", e);
            NetworkStuff.disconnect("Failed to create the backend websocket :c");
        }
        throw new WebSocketException(WebSocketError.SOCKET_CONNECT_ERROR);
    }
}
