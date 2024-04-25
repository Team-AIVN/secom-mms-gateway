package international.dmc.secom_mms_gateway.components;

import international.dmc.secom_mms_gateway.utils.KeystoreUtil;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.base.DigitalSignatureCertificate;
import org.grad.secom.core.base.SecomSignatureProvider;
import org.grad.secom.core.models.enums.DigitalSignatureAlgorithmEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Component
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
            return keystoreUtil.signData(payload);
        } catch (NoSuchAlgorithmException | SignatureException | CertificateException |
                 KeyStoreException | IOException | UnrecoverableEntryException | InvalidKeyException e) {
            log.error(e.getMessage());
            return new byte[0];
        }
    }

    @Override
    public boolean validateSignature(String signatureCertificate, DigitalSignatureAlgorithmEnum algorithm, byte[] signature, byte[] content) {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(signatureCertificate.getBytes());
            X509Certificate x509Certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(byteArrayInputStream);

            Signature sign = Signature.getInstance(algorithm.getValue());
            sign.initVerify(x509Certificate);
            sign.update(content);

            return sign.verify(signature);
        } catch (NoSuchAlgorithmException | CertificateException | InvalidKeyException | SignatureException e) {
            log.error(e.getMessage());
            return false;
        }
    }
}
