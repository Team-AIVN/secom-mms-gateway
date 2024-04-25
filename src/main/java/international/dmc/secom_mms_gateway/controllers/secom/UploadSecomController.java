package international.dmc.secom_mms_gateway.controllers.secom;

import international.dmc.secom_mms_gateway.mms.MMSAgent;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.interfaces.UploadSecomInterface;
import org.grad.secom.core.models.AcknowledgementObject;
import org.grad.secom.core.models.EnvelopeAckObject;
import org.grad.secom.core.models.EnvelopeUploadObject;
import org.grad.secom.core.models.UploadObject;
import org.grad.secom.core.models.UploadResponseObject;
import org.grad.secom.core.models.enums.AckRequestEnum;
import org.grad.secom.core.models.enums.AckTypeEnum;
import org.grad.secom.springboot3.components.SecomClient;
import org.grad.secom.springboot3.components.SecomConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;

@Component
@Path("/")
@Validated
@Slf4j
public class UploadSecomController implements UploadSecomInterface {

    @Value("${international.dmc.secom_mms_gateway.secom.serviceUrl}")
    private String secomServiceUrl;
    private final SecomConfigProperties secomConfigProperties;
    private SecomClient secomClient;
    private final MMSAgent mmsAgent;

    @Autowired
    public UploadSecomController(SecomConfigProperties secomConfigProperties, MMSAgent mmsAgent) {
        this.secomConfigProperties = secomConfigProperties;
        this.mmsAgent = mmsAgent;
    }

    @PostConstruct
    public void init() throws UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        if (secomServiceUrl != null && !secomServiceUrl.isBlank()) {
            secomClient = new SecomClient(URI.create(secomServiceUrl).toURL(), secomConfigProperties);
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
            mmsAgent.publishMessage(data);
        } catch (UnrecoverableEntryException | InvalidKeyException | CertificateException | NoSuchProviderException |
                 IOException | KeyStoreException | NoSuchAlgorithmException | SignatureException e) {
            log.error("Could not publish received dataset");
        }

        return new UploadResponseObject();
    }
}
