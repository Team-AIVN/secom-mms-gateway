package international.dmc.secom_mms_gateway.services;

import international.dmc.secom_mms_gateway.model.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.base.SecomCertificateProvider;
import org.grad.secom.core.base.SecomSignatureProvider;
import org.grad.secom.core.models.RemoveSubscriptionObject;
import org.grad.secom.core.models.SubscriptionRequestObject;
import org.grad.secom.core.models.SubscriptionResponseObject;
import org.grad.secom.springboot3.components.SecomClient;
import org.grad.secom.springboot3.components.SecomConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SubscriptionService {
    private final Map<String, Subscription> subscriptions = new HashMap<>();

    private final SecomConfigProperties secomConfigProperties;
    private final SecomCertificateProvider secomCertificateProvider;
    private final SecomSignatureProvider secomSignatureProvider;

    @Autowired
    public SubscriptionService(SecomConfigProperties secomConfigProperties, SecomCertificateProvider secomCertificateProvider, SecomSignatureProvider secomSignatureProvider) {
        this.secomConfigProperties = secomConfigProperties;
        this.secomCertificateProvider = secomCertificateProvider;
        this.secomSignatureProvider = secomSignatureProvider;
    }

    public Subscription getSubscription(String serviceMrn) {
        return subscriptions.get(serviceMrn);
    }

    public List<Subscription> getAllSubscriptions() {
        return new ArrayList<>(subscriptions.values());
    }

    public Subscription addSubscription(Subscription subscription) throws UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        SecomClient secomClient = new SecomClient(URI.create(subscription.getServiceUrl()).toURL(), secomConfigProperties);
        secomClient.setCertificateProvider(secomCertificateProvider);
        secomClient.setSignatureProvider(secomSignatureProvider);
        SubscriptionRequestObject subscriptionRequest = new SubscriptionRequestObject();
        subscriptionRequest.setContainerType(subscription.getContainerType());
        subscriptionRequest.setDataProductType(subscription.getDataProductType());
        if (StringUtils.hasText(subscription.getDataReference())) {
            UUID dataReference = UUID.fromString(subscription.getDataReference());
            subscriptionRequest.setDataReference(dataReference);
        }
        Optional<SubscriptionResponseObject> subscriptionResponse = secomClient.subscription(subscriptionRequest);
        if (subscriptionResponse.isPresent() && subscriptionResponse.get().getSubscriptionIdentifier() != null) {
            subscription.setSecomClient(secomClient);
            subscription.setSubscriptionId(subscriptionResponse.get().getSubscriptionIdentifier());
            subscriptions.put(subscription.getServiceMrn(), subscription);
            return subscription;
        }
        return null;
    }

    public boolean removeSubscription(String serviceMrn) {
        Subscription subscription = subscriptions.get(serviceMrn);
        RemoveSubscriptionObject removeSubscriptionObject = new RemoveSubscriptionObject();
        removeSubscriptionObject.setSubscriptionIdentifier(subscription.getSubscriptionId());
        var response = subscription.getSecomClient().removeSubscription(removeSubscriptionObject);
        if (response.isEmpty()) {
            return false;
        }
        subscriptions.remove(serviceMrn);
        return true;
    }

    public void removeAllSubscriptions() {
        subscriptions.values().forEach(subscription -> removeSubscription(subscription.getServiceMrn()));
    }
}
