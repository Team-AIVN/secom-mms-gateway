package international.dmc.secom_mms_gateway.mms;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import international.dmc.secom_mms_gateway.utils.KeystoreUtil;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.mmtp.ApplicationMessage;
import net.maritimeconnectivity.mmtp.ApplicationMessageHeader;
import net.maritimeconnectivity.mmtp.Connect;
import net.maritimeconnectivity.mmtp.Disconnect;
import net.maritimeconnectivity.mmtp.MmtpMessage;
import net.maritimeconnectivity.mmtp.MsgType;
import net.maritimeconnectivity.mmtp.ProtocolMessage;
import net.maritimeconnectivity.mmtp.ProtocolMessageType;
import net.maritimeconnectivity.mmtp.ResponseEnum;
import net.maritimeconnectivity.mmtp.ResponseMessage;
import net.maritimeconnectivity.mmtp.Send;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class MMSAgent {

    @Value("${international.dmc.secom_mms_gateway.mms.mms-edgerouter.url}")
    private String edgeRouterURL;
    @Value("${international.dmc.secom_mms_gateway.mms.own-mrn}")
    private String ownMrn;

    private final KeystoreUtil keystoreUtil;
    private final AtomicReference<MmtpMessage> lastSentMessage = new AtomicReference<>();
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private WebSocketSession webSocketSession;

    @Autowired
    public MMSAgent(KeystoreUtil keystoreUtil) {
        this.keystoreUtil = keystoreUtil;
    }

    @PostConstruct
    public void init() throws URISyntaxException, ExecutionException, InterruptedException, NoSuchAlgorithmException,
            CertificateException, KeyStoreException, IOException, UnrecoverableKeyException, KeyManagementException {
        setupWebSocket();
    }

    @PreDestroy
    public void preDestroy() throws IOException, InterruptedException {
        shuttingDown.set(true);
        if (webSocketSession.isOpen()) {
            MmtpMessage disconnect = MmtpMessage.newBuilder()
                    .setMsgType(MsgType.PROTOCOL_MESSAGE)
                    .setUuid(UUID.randomUUID().toString())
                    .setProtocolMessage(ProtocolMessage.newBuilder()
                            .setProtocolMsgType(ProtocolMessageType.DISCONNECT_MESSAGE)
                            .setDisconnectMessage(Disconnect.newBuilder())
                    )
                    .build();
            sendMessage(disconnect);
            lastSentMessage.set(disconnect);
            Thread.sleep(2000);
        }
    }

    public void publishMessage(byte[] payload, String subject) throws UnrecoverableEntryException, CertificateException,
            SignatureException, NoSuchAlgorithmException, KeyStoreException, IOException, InvalidKeyException {
        long expires = Instant.now().plus(30, ChronoUnit.DAYS).getEpochSecond();
        byte[] signature = generateSignature(subject, expires, ownMrn, payload);

        MmtpMessage mmtpMessage = MmtpMessage.newBuilder()
                .setMsgType(MsgType.PROTOCOL_MESSAGE)
                .setUuid(UUID.randomUUID().toString())
                .setProtocolMessage(ProtocolMessage.newBuilder()
                        .setProtocolMsgType(ProtocolMessageType.SEND_MESSAGE)
                        .setSendMessage(Send.newBuilder()
                                .setApplicationMessage(ApplicationMessage.newBuilder()
                                        .setHeader(ApplicationMessageHeader.newBuilder()
                                                .setExpires(expires)
                                                .setBodySizeNumBytes(payload.length)
                                                .setSubject(subject)
                                                .setSender(ownMrn)
                                        )
                                        .setSignature(ByteString.copyFrom(signature))
                                        .setBody(ByteString.copyFrom(payload))
                                )
                        )
                )
                .build();
        sendMessage(mmtpMessage);
    }

    public void sendMessage(MmtpMessage mmtpMessage) throws IOException {
        byte[] bytes = mmtpMessage.toByteArray();
        webSocketSession.sendMessage(new BinaryMessage(bytes));
        lastSentMessage.set(mmtpMessage);
    }

    private void setupWebSocket() throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException,
            CertificateException, IOException, KeyManagementException, URISyntaxException, InterruptedException,
            ExecutionException {
        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystoreUtil.getMmsKeystore(), keystoreUtil.getMmsKeystorePassword());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        webSocketClient.setSslContext(sslContext);
        URI uri = new URI(edgeRouterURL);
        webSocketSession = webSocketClient.execute(new MMSWebsocketHandler(), null, uri).get();
        log.info("Connected to edge router {}", edgeRouterURL);
    }

    private byte[] generateSignature(String subject, long expires, String ownMrn, byte[] body)
            throws SignatureException, UnrecoverableEntryException, CertificateException, NoSuchAlgorithmException,
            KeyStoreException, IOException, InvalidKeyException {
        List<byte[]> byteArrays = new ArrayList<>();
        byteArrays.add(subject.getBytes());
        byteArrays.add(Long.toString(expires).getBytes());
        byteArrays.add(ownMrn.getBytes());
        byteArrays.add(Integer.toString(body.length).getBytes());
        byteArrays.add(body);
        byte[] bytesToBeSigned = new byte[0];
        for (byte[] bytes : byteArrays) {
            bytesToBeSigned = ArrayUtils.addAll(bytesToBeSigned, bytes);
        }

        return keystoreUtil.signDataMMS(bytesToBeSigned);
    }

    private class MMSWebsocketHandler extends BinaryWebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws IOException {
            MmtpMessage mmtpMessage = MmtpMessage.newBuilder()
                    .setUuid(UUID.randomUUID().toString())
                    .setMsgType(MsgType.PROTOCOL_MESSAGE)
                    .setProtocolMessage(ProtocolMessage.newBuilder()
                            .setProtocolMsgType(ProtocolMessageType.CONNECT_MESSAGE)
                            .setConnectMessage(Connect.newBuilder()
                                    .setOwnMrn(ownMrn)
                            )
                    )
                    .build();
            byte[] bytes = mmtpMessage.toByteArray();
            try {
                session.sendMessage(new BinaryMessage(bytes));
            } catch (IOException e) {
                log.error("Failed to send CONNECT message to Edge Router", e);
                session.close(CloseStatus.PROTOCOL_ERROR);
            }
            lastSentMessage.set(mmtpMessage);
        }

        @Override
        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
            try {
                MmtpMessage mmtpMessage = MmtpMessage.parseFrom(message.getPayload());
                if (mmtpMessage.hasProtocolMessage()) {
                    log.warn("Received a Protocol Message that we cannot handle right now");
                    return;
                }

                if (mmtpMessage.hasResponseMessage()) {
                    ResponseMessage responseMessage = mmtpMessage.getResponseMessage();
                    if (!responseMessage.getResponseToUuid().equals(lastSentMessage.get().getUuid())) {
                        log.error("Received a response to the wrong message");
                    } else if (responseMessage.getResponse() != ResponseEnum.GOOD && responseMessage.hasReasonText()) {
                        log.error("Received a response with an error: {}", responseMessage.getReasonText());
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                log.warn("Received a message that we could not parse");
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            if (!status.equalsCode(CloseStatus.NORMAL) && !shuttingDown.get()) {
                if (StringUtils.hasText(status.getReason())) {
                    log.error("The websocket was closed with code {} and reason {}", status.getCode(), status.getReason());
                } else {
                    log.error("The websocket was closed with code {}", status.getCode());
                }
                try {
                    session.close();
                } catch (IOException e) {
                    log.error("Failed to close websocket", e);
                }
                int retryCount = 0;
                while (retryCount < 10 && !shuttingDown.get()) {
                    try {
                        Thread.sleep(5000);
                        setupWebSocket();
                        return;
                    } catch (IOException | ExecutionException e) {
                        log.error("Couldn't connect to the websocket, trying again: {}", e.getMessage());
                        // If we fail, we just try again
                        retryCount++;
                    }
                }
                if (!shuttingDown.get()) {
                    log.error("Failed to connect to edge router after {} attempts", retryCount);
                    System.exit(1);
                }
            }
            session.close();
        }
    }
}
