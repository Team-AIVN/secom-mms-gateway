package international.dmc.secom_mms_gateway.components;

import international.dmc.secom_mms_gateway.utils.KeystoreUtil;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.base.DigitalSignatureCertificate;
import org.grad.secom.core.base.SecomSignatureProvider;
import org.grad.secom.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.grad.secom.core.utils.SecomPemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.HexFormat;

@Component("signatureProvider")
@Slf4j
public class SecomSignatureProviderImpl implements SecomSignatureProvider {

    @Value("${international.dmc.secom_mms_gateway.secom.keypair.signing-algorithm:SHA3-384withECDSA}")
    private String defaultSigningAlgorithm;

    private final KeystoreUtil keystoreUtil;

    @Autowired
    public SecomSignatureProviderImpl(KeystoreUtil keystoreUtil) {
        this.keystoreUtil = keystoreUtil;
    }

    @Override
    public DigitalSignatureAlgorithmEnum getSignatureAlgorithm() {
        return DigitalSignatureAlgorithmEnum.fromValue(defaultSigningAlgorithm);
    }

    @Override
    public byte[] generateSignature(DigitalSignatureCertificate signatureCertificate, DigitalSignatureAlgorithmEnum algorithm, byte[] payload) {
        try {
            byte[] signature = keystoreUtil.signDataSecom(payload, defaultSigningAlgorithm);
            log.debug(HexFormat.of().formatHex(signature));
            return signature;
        } catch (NoSuchAlgorithmException | SignatureException | CertificateException |
                 KeyStoreException | IOException | UnrecoverableEntryException | InvalidKeyException e) {
            log.error("Signature generation failed:", e);
            return new byte[0];
        }
    }

    @Override
    public boolean validateSignature(String signatureCertificate, DigitalSignatureAlgorithmEnum algorithm, byte[] signature, byte[] content) {
        try {
            Signature sign = Signature.getInstance(algorithm.getValue());
            sign.initVerify(SecomPemUtils.getCertFromPem(signatureCertificate));
            sign.update(content);

            return sign.verify(signature);
        } catch (NoSuchAlgorithmException | CertificateException | InvalidKeyException | SignatureException e) {
            log.error("Signature verification failed:", e);
            return false;
        }
    }
}
