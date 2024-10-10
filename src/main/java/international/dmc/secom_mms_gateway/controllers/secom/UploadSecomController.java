package international.dmc.secom_mms_gateway.controllers.secom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import international.dmc.secom_mms_gateway.mms.MMSAgent;
import international.dmc.secom_mms_gateway.model.Subscription;
import international.dmc.secom_mms_gateway.services.SubscriptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.pki.CertificateHandler;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.grad.secom.core.interfaces.UploadSecomInterface;
import org.grad.secom.core.models.AcknowledgementObject;
import org.grad.secom.core.models.EnvelopeAckObject;
import org.grad.secom.core.models.EnvelopeUploadObject;
import org.grad.secom.core.models.UploadObject;
import org.grad.secom.core.models.UploadResponseObject;
import org.grad.secom.core.models.enums.AckRequestEnum;
import org.grad.secom.core.models.enums.AckTypeEnum;
import org.grad.secom.core.models.enums.SECOM_ResponseCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@Path("/")
@Validated
@Slf4j
public class UploadSecomController implements UploadSecomInterface {

    private static final int PAYLOAD_SIZE_LIMIT = 48 * (1 << 10); // 48 KiB

    private final SubscriptionService subscriptionService;

    private final MMSAgent mmsAgent;

    @Autowired
    public UploadSecomController(SubscriptionService subscriptionService, MMSAgent mmsAgent) {
        this.subscriptionService = subscriptionService;
        this.mmsAgent = mmsAgent;
    }

    @PreDestroy
    public void preDestroy() {
        subscriptionService.removeAllSubscriptions();
    }

    @Tag(name = "SECOM")
    @Override
    public UploadResponseObject upload(@Valid UploadObject uploadObject) {
        log.debug("Received upload object");
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            log.debug(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(uploadObject));
        } catch (JsonProcessingException e) {
            log.error("Could not serialize upload object", e);
        }
        EnvelopeUploadObject envelope = uploadObject.getEnvelope();

        String certificate = envelope.getEnvelopeSignatureCertificate();
        byte[] certificateBytes = Base64.getDecoder().decode(certificate);

        X509Certificate x509Certificate;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            x509Certificate = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certificateBytes));
        } catch (CertificateException e) {
            log.error("Could not parse certificate in upload object", e);
            UploadResponseObject uploadResponseObject = new UploadResponseObject();
            uploadResponseObject.setSECOM_ResponseCode(SECOM_ResponseCodeEnum.INVALID_CERTIFICATE);
            return uploadResponseObject;
        }

        String certDn = x509Certificate.getSubjectX500Principal().getName();
        RDN[] rdns = IETFUtils.rDNsFromString(certDn, BCStyle.INSTANCE);
        String uploaderMrn = CertificateHandler.getElement(rdns, BCStyle.UID);

        Subscription subscription = subscriptionService.getSubscription(uploaderMrn);
        if (subscription == null) {
            UploadResponseObject uploadResponseObject = new UploadResponseObject();
            uploadResponseObject.setSECOM_ResponseCode(SECOM_ResponseCodeEnum.MISSING_REQUIRED_DATA_FOR_SERVICE);
            uploadResponseObject.setResponseText("No subscription found");
            return uploadResponseObject;
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
        if (data.length <= PAYLOAD_SIZE_LIMIT) {
            try {
                mmsAgent.publishMessage(data, subscription.getMmsSubject());
            } catch (UnrecoverableEntryException | InvalidKeyException | CertificateException |
                     IOException | KeyStoreException | NoSuchAlgorithmException | SignatureException e) {
                log.error("Could not publish received dataset", e);
            }
        } else {
            log.warn("Payload size limit exceeded");
        }

        AckRequestEnum ackRequest = envelope.getAckRequest();
        if (ackRequest != null && !ackRequest.equals(AckRequestEnum.NO_ACK_REQUESTED)) {
            EnvelopeAckObject envelopeAckObject = new EnvelopeAckObject();
            envelopeAckObject.setCreatedAt(LocalDateTime.now());
            envelopeAckObject.setTransactionIdentifier(envelope.getTransactionIdentifier());
            envelopeAckObject.setAckType(AckTypeEnum.DELIVERED_ACK);

            AcknowledgementObject acknowledgementObject = new AcknowledgementObject();
            acknowledgementObject.setEnvelope(envelopeAckObject);
            try {
                subscription.getSecomClient().acknowledgment(acknowledgementObject);
            } catch (WebClientResponseException e) {
                log.error("Error while acknowledging", e);
                log.error(e.getResponseBodyAsString());
            }
        }

        return new UploadResponseObject();
    }
}
