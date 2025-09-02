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
