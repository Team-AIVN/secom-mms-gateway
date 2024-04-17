package international.dmc.secommmsgateway.websocket;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.mmtp.Connect;
import net.maritimeconnectivity.mmtp.MmtpMessage;
import net.maritimeconnectivity.mmtp.MsgType;
import net.maritimeconnectivity.mmtp.ProtocolMessage;
import net.maritimeconnectivity.mmtp.ProtocolMessageType;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class MMSWebSocketClient {

    private final StandardWebSocketClient webSocketClient;
    private final WebSocketSession webSocketSession;

    private final AtomicReference<MmtpMessage> lastSentMessage;

    public MMSWebSocketClient(String edgeRouterURL) throws URISyntaxException, InterruptedException, ExecutionException {
        lastSentMessage = new AtomicReference<>();
        webSocketClient = new StandardWebSocketClient();
        URI uri = new URI(edgeRouterURL);
        webSocketSession = webSocketClient.execute(new MMSWebsocketHandler(), null, uri).get();
    }

    public void sendMessage(MmtpMessage mmtpMessage) throws IOException {
        byte[] bytes = mmtpMessage.toByteArray();
        webSocketSession.sendMessage(new BinaryMessage(bytes));
        lastSentMessage.set(mmtpMessage);
    }

    private class MMSWebsocketHandler extends BinaryWebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            super.afterConnectionEstablished(session);
            MmtpMessage mmtpMessage = MmtpMessage.newBuilder()
                    .setUuid(UUID.randomUUID().toString())
                    .setMsgType(MsgType.PROTOCOL_MESSAGE)
                    .setProtocolMessage(ProtocolMessage.newBuilder()
                            .setProtocolMsgType(ProtocolMessageType.CONNECT_MESSAGE)
                            .setConnectMessage(Connect.newBuilder()
                                    .setOwnMrn("MY_MRN")
                                    .build())
                            .build())
                    .build();
            byte[] bytes = mmtpMessage.toByteArray();
            session.sendMessage(new BinaryMessage(bytes));
            lastSentMessage.set(mmtpMessage);
        }

        @Override
        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
            super.handleBinaryMessage(session, message);
            MmtpMessage mmtpMessage = MmtpMessage.parseFrom(message.getPayload());
            if (mmtpMessage.hasProtocolMessage()) {
                log.warn("Received a Protocol Message that we cannot handle right now");
                return;
            }

            if (!mmtpMessage.getUuid().equals(lastSentMessage.get().getUuid())) {

            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            super.afterConnectionClosed(session, status);
        }
    }
}
