package international.dmc.secom_mms_gateway.controllers.secom;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.interfaces.SubscriptionNotificationSecomInterface;
import org.grad.secom.core.models.SubscriptionNotificationObject;
import org.grad.secom.core.models.SubscriptionNotificationResponseObject;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.ws.rs.Path;

@Component
@Path("/")
@Validated
@Slf4j
public class SubscriptionNotificationSecomController implements SubscriptionNotificationSecomInterface {

    @Tag(name = "SECOM")
    @Override
    public SubscriptionNotificationResponseObject subscriptionNotification(@Valid SubscriptionNotificationObject subscriptionNotificationObject) {
        return new SubscriptionNotificationResponseObject();
    }
}
