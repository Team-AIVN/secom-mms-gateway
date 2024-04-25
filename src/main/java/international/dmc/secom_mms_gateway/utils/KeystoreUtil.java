package international.dmc.secom_mms_gateway.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
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

@Slf4j
@Component
public class KeystoreUtil {

    @Value("${international.dmc.secom_mms_gateway.keystore.path}")
    private String keystorePath;
    @Value("${international.dmc.secom_mms_gateway.keystore.password}")
    private String keystorePassword;
    @Value("${international.dmc.secom_mms_gateway.secom.keypair.signing-algorithm:SHA3-384withECDSA}")
    private String signatureAlgorithm;
    @Value("${international.dmc.secom_mms_gateway.keystore.alias}")
    private String keyAlias;

    private final SecureRandom secureRandom = new SecureRandom();

    public byte[] signData(byte[] data) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, UnrecoverableEntryException, InvalidKeyException, SignatureException {
        KeyStore keyStore = KeyStore.getInstance(new File(keystorePath), keystorePassword.toCharArray());
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keystorePassword.toCharArray());
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(keyAlias, protectionParameter);

        Signature signature = Signature.getInstance(signatureAlgorithm);
        signature.update(data);
        signature.initSign(privateKeyEntry.getPrivateKey(), secureRandom);
        return signature.sign();
    }

    public KeyStore getKeystore() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        return KeyStore.getInstance(new File(keystorePath), keystorePassword.toCharArray());
    }

    public char[] getKeystorePassword() {
        return keystorePassword.toCharArray();
    }
}
