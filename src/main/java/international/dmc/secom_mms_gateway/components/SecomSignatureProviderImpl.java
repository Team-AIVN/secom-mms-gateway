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
