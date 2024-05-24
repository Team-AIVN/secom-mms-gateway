package international.dmc.secom_mms_gateway.utils;

import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.exceptions.SecomGenericException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Slf4j
@Component
public class KeystoreUtil {

    @Value("${international.dmc.secom_mms_gateway.keystore.path}")
    private String keystorePath;
    @Value("${international.dmc.secom_mms_gateway.keystore.password}")
    private String keystorePassword;
    @Value("${international.dmc.secom_mms_gateway.mms.keypair.signing-algorithm:SHA384withECDSA}")
    private String signatureAlgorithm;
    @Value("${international.dmc.secom_mms_gateway.keystore.alias}")
    private String keyAlias;

    @Value("${international.dmc.secom_mms_gateway.rootCA.path}")
    private String rootCertificatePath;

    private final SecureRandom secureRandom = new SecureRandom();

    public byte[] signData(byte[] data) throws NoSuchAlgorithmException, CertificateException, KeyStoreException,
            IOException, UnrecoverableEntryException, InvalidKeyException, SignatureException {
        KeyStore keyStore = KeyStore.getInstance(new File(keystorePath), keystorePassword.toCharArray());
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keystorePassword.toCharArray());
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(keyAlias, protectionParameter);

        Signature signature = Signature.getInstance(signatureAlgorithm);
        signature.initSign(privateKeyEntry.getPrivateKey(), secureRandom);
        signature.update(data);
        return signature.sign();
    }

    public KeyStore getKeystore() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        return KeyStore.getInstance(new File(keystorePath), keystorePassword.toCharArray());
    }

    public char[] getKeystorePassword() {
        return keystorePassword.toCharArray();
    }

    public X509Certificate getSigningCertificate() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = getKeystore();
        return (X509Certificate) keyStore.getCertificate(keyAlias);
    }

    public X509Certificate getRootCertificate() {
        try (FileInputStream fis = new FileInputStream(rootCertificatePath)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(fis);
        } catch (IOException | CertificateException e) {
            throw new SecomGenericException("Was unable to load root certificate");
        }
    }
}
