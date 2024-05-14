package international.dmc.secom_mms_gateway.controllers.secom;

import international.dmc.secom_mms_gateway.mms.MMSAgent;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.interfaces.UploadSecomInterface;
import org.grad.secom.core.models.AcknowledgementObject;
import org.grad.secom.core.models.EnvelopeAckObject;
import org.grad.secom.core.models.EnvelopeUploadObject;
import org.grad.secom.core.models.RemoveSubscriptionObject;
import org.grad.secom.core.models.SubscriptionRequestObject;
import org.grad.secom.core.models.UploadObject;
import org.grad.secom.core.models.UploadResponseObject;
import org.grad.secom.core.models.enums.AckRequestEnum;
import org.grad.secom.core.models.enums.AckTypeEnum;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.grad.secom.springboot3.components.SecomClient;
import org.grad.secom.springboot3.components.SecomConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@Path("/")
@Validated
@Slf4j
public class UploadSecomController implements UploadSecomInterface {

    private static final int PAYLOAD_SIZE_LIMIT = 50 * (1 << 10); // 50 KiB

    @Value("${international.dmc.secom_mms_gateway.secom.serviceUrl}")
    private String secomServiceUrl;
    private final SecomConfigProperties secomConfigProperties;
    private SecomClient secomClient;
    private final MMSAgent mmsAgent;

    private UUID subscriptionIdentifier;

    @Autowired
    public UploadSecomController(SecomConfigProperties secomConfigProperties, MMSAgent mmsAgent) {
        this.secomConfigProperties = secomConfigProperties;
        this.mmsAgent = mmsAgent;
    }

    @PostConstruct
    public void init() throws UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        if (secomServiceUrl != null && !secomServiceUrl.isBlank()) {
            secomClient = new SecomClient(URI.create(secomServiceUrl).toURL(), secomConfigProperties);
            SubscriptionRequestObject subscriptionRequestObject = new SubscriptionRequestObject();
            subscriptionRequestObject.setContainerType(ContainerTypeEnum.S100_ExchangeSet);
            subscriptionRequestObject.setDataProductType(SECOM_DataProductType.S125);
            subscriptionRequestObject.setDataReference(UUID.fromString("c0a8fc1b-8da7-168e-818d-a7881c680000"));
            var subscriptionResponse = secomClient.subscription(subscriptionRequestObject);
            subscriptionResponse.ifPresent(sro -> {
                log.info(sro.getMessage());
                subscriptionIdentifier = sro.getSubscriptionIdentifier();
            });
        }
    }

    @PreDestroy
    public void preDestroy() {
        if (secomClient != null && subscriptionIdentifier != null) {
            RemoveSubscriptionObject removeSubscriptionObject = new RemoveSubscriptionObject();
            removeSubscriptionObject.setSubscriptionIdentifier(subscriptionIdentifier);
            var removeSubscriptionResponseObject = secomClient.removeSubscription(removeSubscriptionObject);
            removeSubscriptionResponseObject.ifPresent(rsro -> log.info(rsro.getMessage()));
        }
    }

    @Tag(name = "SECOM")
    @Override
    public UploadResponseObject upload(@Valid UploadObject uploadObject) {
        EnvelopeUploadObject envelope = uploadObject.getEnvelope();

        AckRequestEnum ackRequest = envelope.getAckRequest();
        if (secomClient != null && ackRequest != null && !ackRequest.equals(AckRequestEnum.NO_ACK_REQUESTED)) {
            EnvelopeAckObject envelopeAckObject = new EnvelopeAckObject();
            envelopeAckObject.setCreatedAt(LocalDateTime.now());
            envelopeAckObject.setTransactionIdentifier(envelope.getTransactionIdentifier());
            envelopeAckObject.setAckType(AckTypeEnum.DELIVERED_ACK);

            AcknowledgementObject acknowledgementObject = new AcknowledgementObject();
            acknowledgementObject.setEnvelope(envelopeAckObject);
            secomClient.acknowledgment(acknowledgementObject);
        }

        byte[] data = envelope.getData();
        try {
            data = Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            log.debug("The received data was not Base64 encoded: {}", e.getMessage());
        }
        if (data.length > PAYLOAD_SIZE_LIMIT && !envelope.getExchangeMetadata().getCompressionFlag()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(bos)) {
                zos.putNextEntry(new ZipEntry("data"));
                zos.write(data);
                zos.closeEntry();
                zos.finish();
                data = bos.toByteArray();
            } catch (IOException e) {
                log.error("Error while closing stream", e);
            }
        }
        if (data.length < PAYLOAD_SIZE_LIMIT) {
            try {
                mmsAgent.publishMessage(data);
            } catch (UnrecoverableEntryException | InvalidKeyException | CertificateException |
                     IOException | KeyStoreException | NoSuchAlgorithmException | SignatureException e) {
                log.error("Could not publish received dataset", e);
            }
        } else {
            log.warn("Payload size limit exceeded");
        }

        return new UploadResponseObject();
    }
}
