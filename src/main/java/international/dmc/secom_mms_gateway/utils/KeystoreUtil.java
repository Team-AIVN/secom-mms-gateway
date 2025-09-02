/*
 * Copyright 2025 Digital Maritime Consultancy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package international.dmc.secom_mms_gateway.utils;

import lombok.extern.slf4j.Slf4j;
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
    private String mmsKeystorePath;
    @Value("${international.dmc.secom_mms_gateway.keystore.password}")
    private String mmsKeystorePassword;
    @Value("${international.dmc.secom_mms_gateway.mms.keypair.signing-algorithm:SHA384withECDSA}")
    private String mmsSignatureAlgorithm;
    @Value("${international.dmc.secom_mms_gateway.keystore.alias}")
    private String mmsKeyAlias;

    @Value("${secom.security.ssl.keystore}")
    private String secomKeystorePath;
    @Value("${secom.security.ssl.keystorePassword}")
    private String secomKeystorePassword;
    @Value("${secom.security.ssl.alias:1}")
    private String secomKeyAlias;

    @Value("${international.dmc.secom_mms_gateway.rootCA.path:root.pem}")
    private String rootCertificatePath;

    private final SecureRandom secureRandom = new SecureRandom();

    public byte[] signDataMMS(byte[] data) throws NoSuchAlgorithmException, CertificateException, KeyStoreException,
            IOException, UnrecoverableEntryException, InvalidKeyException, SignatureException {
        return signData(data, mmsSignatureAlgorithm, mmsKeystorePath, mmsKeystorePassword, mmsKeyAlias);
    }

    public byte[] signDataSecom(byte[] data, String secomSignatureAlgorithm) throws CertificateException,
            KeyStoreException, IOException, NoSuchAlgorithmException, UnrecoverableEntryException, InvalidKeyException,
            SignatureException {
        return signData(data, secomSignatureAlgorithm, secomKeystorePath, secomKeystorePassword, secomKeyAlias);
    }

    private byte[] signData(byte[] data, String signatureAlgorithm, String keyStorePath, String keystorePassword, String keystoreAlias) throws CertificateException, KeyStoreException,
            IOException, NoSuchAlgorithmException, UnrecoverableEntryException, InvalidKeyException,
            SignatureException {
        KeyStore keyStore = KeyStore.getInstance(new File(keyStorePath), keystorePassword.toCharArray());
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keystorePassword.toCharArray());
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(keystoreAlias, protectionParameter);

        Signature signature = Signature.getInstance(signatureAlgorithm);
        signature.initSign(privateKeyEntry.getPrivateKey(), secureRandom);
        signature.update(data);
        return signature.sign();
    }

    public KeyStore getMmsKeystore() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        return KeyStore.getInstance(new File(mmsKeystorePath), mmsKeystorePassword.toCharArray());
    }

    public char[] getMmsKeystorePassword() {
        return mmsKeystorePassword.toCharArray();
    }

    public KeyStore getSecomKeystore() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        return KeyStore.getInstance(new File(secomKeystorePath), secomKeystorePassword.toCharArray());
    }

    public X509Certificate getSigningSecomCertificate() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = getSecomKeystore();
        return (X509Certificate) keyStore.getCertificate(secomKeyAlias);
    }

    public X509Certificate getRootCertificate() {
        log.debug("Getting root certificate");
        try (FileInputStream fis = new FileInputStream(rootCertificatePath)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(fis);
        } catch (IOException | CertificateException e) {
            log.debug("Error getting root certificate", e);
            return null;
        }
    }
}
