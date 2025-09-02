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
            certificate = keystoreUtil.getSigningSecomCertificate();
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
