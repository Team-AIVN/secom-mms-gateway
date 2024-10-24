package international.dmc.secom_mms_gateway.controllers.management;

import international.dmc.secom_mms_gateway.model.Subscription;
import international.dmc.secom_mms_gateway.services.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

@RestController
@RequestMapping(value = "management")
public class SubscriptionManagementController {

    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionManagementController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping(
            value = "/subscription",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Subscription> createNewSubscription(@RequestBody Subscription subscription) {
        Subscription newSubscription;
        try {
            newSubscription = subscriptionService.addSubscription(subscription);
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | IOException |
                 CertificateException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(newSubscription);
    }

    @GetMapping(
            value = "/subscription/mrn/{mrn}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Subscription> getSubscriptionByMrn(@PathVariable String mrn) {
        Subscription subscription = subscriptionService.getSubscription(mrn);
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(subscription);
    }

    @GetMapping(
            value = "/subscriptions",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<Subscription>> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionService.getAllSubscriptions();
        return ResponseEntity.ok(subscriptions);
    }

    @DeleteMapping(
            value = "/subscription/mrn/{mrn}",
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<Void> deleteSubscriptionByMrn(@PathVariable String mrn) {
        Subscription subscription = subscriptionService.getSubscription(mrn);
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }
        boolean removed = subscriptionService.removeSubscription(mrn);
        if (!removed) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }
}
