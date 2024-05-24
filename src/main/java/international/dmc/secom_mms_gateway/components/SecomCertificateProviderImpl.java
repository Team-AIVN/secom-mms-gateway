package international.dmc.secom_mms_gateway.components;

import international.dmc.secom_mms_gateway.utils.KeystoreUtil;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.base.DigitalSignatureCertificate;
import org.grad.secom.core.base.SecomCertificateProvider;
import org.grad.secom.core.exceptions.SecomGenericException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Slf4j
@Component("certificateProvider")
public class SecomCertificateProviderImpl implements SecomCertificateProvider {
    @Value("${spring.application.name}")
    private String applicationName;

    private final KeystoreUtil keystoreUtil;

    @Autowired
    public SecomCertificateProviderImpl(KeystoreUtil keystoreUtil) {
        this.keystoreUtil = keystoreUtil;
    }

    @Override
    public DigitalSignatureCertificate getDigitalSignatureCertificate() {
        log.debug("getDigitalSignatureCertificate");
        X509Certificate certificate;
        try {
            certificate = keystoreUtil.getSigningCertificate();
        } catch (CertificateException | NoSuchAlgorithmException | IOException | KeyStoreException e) {
            log.error("Was not able to get signing certificate", e);
            throw new SecomGenericException("Was not able to get Digital Signature Certificate");
        }

        DigitalSignatureCertificate digitalSignatureCertificate = new DigitalSignatureCertificate();
        digitalSignatureCertificate.setCertificateAlias(applicationName);
        digitalSignatureCertificate.setCertificate(certificate);
        digitalSignatureCertificate.setPublicKey(certificate.getPublicKey());
        X509Certificate rootCertificate = keystoreUtil.getRootCertificate();
        if (rootCertificate == null) {
            log.error("Root certificate is null");
        } else {
            digitalSignatureCertificate.setRootCertificate(rootCertificate);
        }

        return digitalSignatureCertificate;
    }
}
