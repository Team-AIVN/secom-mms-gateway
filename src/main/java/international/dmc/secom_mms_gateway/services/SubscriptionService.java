package international.dmc.secom_mms_gateway.services;

import international.dmc.secom_mms_gateway.exceptions.SubscriptionException;
import international.dmc.secom_mms_gateway.exceptions.SubscriptionFailure;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SubscriptionService {
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
        if (subscription != null && populateSecomClient(subscription)) {
            return subscription;
        }
        return null;
    }

    public Subscription getSubscriptionById(String subscriptionId) {
        UUID uuid = UUID.fromString(subscriptionId);
        var subscription = subscriptionRepository.getSubscriptionBySubscriptionId(uuid);
        if (subscription != null && populateSecomClient(subscription)) {
            return subscription;
        }
        return null;
    }

    public List<Subscription> getAllSubscriptions() {
        return (List<Subscription>) subscriptionRepository.findAll();
    }

    public Subscription addSubscription(Subscription subscription) throws SubscriptionException {
        if (subscriptionRepository.existsByServiceMrn(subscription.getServiceMrn())) {
            throw new SubscriptionException(SubscriptionFailure.SUBSCRIPTION_ALREADY_EXISTS);
        }
        if (!populateSecomClient(subscription)) {
            throw new SubscriptionException(SubscriptionFailure.SECOM_CLIENT_CREATION_FAILED);
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

        Date now = new Date();
        subscription.setCreatedAt(now);
        subscription.setUpdatedAt(now);

        subscriptionRepository.save(subscription);
        return subscription;
    }

    public boolean unsubscribeAndRemoveSubscription(String serviceMrn) {
        if (!unsubscribeFromService(serviceMrn)) return false;
        subscriptionRepository.deleteByServiceMrn(serviceMrn);
        return true;
    }

    public void removeSubscription(String serviceMrn) {
        subscriptionRepository.deleteByServiceMrn(serviceMrn);
        secomClients.remove(serviceMrn);
    }

    private boolean unsubscribeFromService(String serviceMrn) {
        Subscription subscription = getSubscriptionByMrn(serviceMrn);
        if (subscription != null) {
            RemoveSubscriptionObject removeSubscriptionObject = new RemoveSubscriptionObject();
            removeSubscriptionObject.setSubscriptionIdentifier(subscription.getSubscriptionId());
            var response = subscription.getSecomClient().removeSubscription(removeSubscriptionObject);
            if (response.isPresent()) {
                removeSubscription(serviceMrn);
                return true;
            }
        }
        return false;
    }

    private boolean populateSecomClient(Subscription subscription) {
        var serviceMrn = subscription.getServiceMrn();
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
