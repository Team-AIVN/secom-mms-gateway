package international.dmc.secom_mms_gateway.controllers.secom;

import international.dmc.secom_mms_gateway.services.SubscriptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.interfaces.SubscriptionNotificationSecomInterface;
import org.grad.secom.core.models.SubscriptionNotificationObject;
import org.grad.secom.core.models.SubscriptionNotificationResponseObject;
import org.grad.secom.core.models.enums.SubscriptionEventEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.ws.rs.Path;

@Component
@Path("/")
@Validated
@Slf4j
public class SubscriptionNotificationSecomController implements SubscriptionNotificationSecomInterface {

    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionNotificationSecomController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Tag(name = "SECOM")
    @Override
    public SubscriptionNotificationResponseObject subscriptionNotification(@Valid SubscriptionNotificationObject subscriptionNotificationObject) {
        var subscriptionIdentifier = subscriptionNotificationObject.getSubscriptionIdentifier();
        var subscriptionEventEnum = subscriptionNotificationObject.getEventEnum();
        if (subscriptionIdentifier != null && subscriptionEventEnum.equals(SubscriptionEventEnum.SUBSCRIPTION_REMOVED)) {
            var subscription = subscriptionService.getSubscriptionById(subscriptionIdentifier.toString());
            if (subscription != null) {
                subscriptionService.removeSubscription(subscription.getServiceMrn());
            }
        }
        return new SubscriptionNotificationResponseObject();
    }
}
