package international.dmc.secom_mms_gateway.services;

import international.dmc.secom_mms_gateway.model.Subscription;
import international.dmc.secom_mms_gateway.repositories.SubscriptionRepository;
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

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SubscriptionService {
    private final Map<String, Subscription> subscriptions = new HashMap<>();
    private final SubscriptionRepository subscriptionRepository;

    private final SecomConfigProperties secomConfigProperties;
    private final SecomCertificateProvider secomCertificateProvider;
    private final SecomSignatureProvider secomSignatureProvider;
    private final Map<String, SecomClient> secomClients = new ConcurrentHashMap<>();

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository, SecomConfigProperties secomConfigProperties, SecomCertificateProvider secomCertificateProvider, SecomSignatureProvider secomSignatureProvider) {
        this.subscriptionRepository = subscriptionRepository;
        this.secomConfigProperties = secomConfigProperties;
        this.secomCertificateProvider = secomCertificateProvider;
        this.secomSignatureProvider = secomSignatureProvider;
    }

    public Subscription getSubscriptionByMrn(String serviceMrn) {
        var subscription = subscriptionRepository.getSubscriptionByServiceMrn(serviceMrn);
        if (subscription != null && populateSecomClient(serviceMrn, subscription)) {
            return subscription;
        }
        return null;
    }

    public Subscription getSubscriptionById(String subscriptionId) {
        UUID uuid = UUID.fromString(subscriptionId);
        var subscription = subscriptionRepository.getSubscriptionBySubscriptionId(uuid);
        if (subscription != null && populateSecomClient(subscription.getServiceMrn(), subscription)) {
            return subscription;
        }
        return null;
    }

    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.getAll();
    }

    public Subscription addSubscription(Subscription subscription) {
        if (!populateSecomClient(subscription.getServiceMrn(), subscription)) {
            return null;
        }
        var secomClient = subscription.getSecomClient();
        SubscriptionRequestObject subscriptionRequest = new SubscriptionRequestObject();
        subscriptionRequest.setContainerType(subscription.getContainerType());
        subscriptionRequest.setDataProductType(subscription.getDataProductType());
        if (StringUtils.hasText(subscription.getDataReference())) {
            UUID dataReference = UUID.fromString(subscription.getDataReference());
            subscriptionRequest.setDataReference(dataReference);
        }
        subscription.setSecomClient(secomClient);
        Optional<SubscriptionResponseObject> subscriptionResponse = secomClient.subscription(subscriptionRequest);
        if (subscriptionResponse.isPresent() && subscriptionResponse.get().getSubscriptionIdentifier() != null) {
            subscription.setSubscriptionId(subscriptionResponse.get().getSubscriptionIdentifier());
        }
        subscriptions.put(subscription.getServiceMrn(), subscription);
        return subscription;
    }

    public boolean unsubscribeAndRemoveSubscription(String serviceMrn) {
        if (!unsubscribeFromService(serviceMrn)) return false;
        subscriptions.remove(serviceMrn);
        return true;
    }

    public void removeSubscription(String serviceMrn) {
        subscriptions.remove(serviceMrn);
    }

    public void removeAllSubscriptions() {
        subscriptions.values().forEach(subscription -> unsubscribeAndRemoveSubscription(subscription.getServiceMrn()));
    }

    private boolean unsubscribeFromService(String serviceMrn) {
        Subscription subscription = getSubscriptionByMrn(serviceMrn);
        if (subscription != null) {
            RemoveSubscriptionObject removeSubscriptionObject = new RemoveSubscriptionObject();
            removeSubscriptionObject.setSubscriptionIdentifier(subscription.getSubscriptionId());
            var response = subscription.getSecomClient().removeSubscription(removeSubscriptionObject);
            return response.isPresent();
        }
        return false;
    }

    private boolean populateSecomClient(String serviceMrn, Subscription subscription) {
        var secomClient = secomClients.computeIfAbsent(serviceMrn, k -> {
            try {
                var tmp = new SecomClient(URI.create(subscription.getServiceUrl()).toURL(), secomConfigProperties);
                tmp.setCertificateProvider(secomCertificateProvider);
                tmp.setSignatureProvider(secomSignatureProvider);
                return tmp;
            } catch (Exception e) {
                log.error("Error creating SECOM client", e);
                return null;
            }
        });
        if (secomClient != null) {
            secomClients.put(serviceMrn, secomClient);
            subscription.setSecomClient(secomClient);
            return true;
        }
        return false;
    }
}
