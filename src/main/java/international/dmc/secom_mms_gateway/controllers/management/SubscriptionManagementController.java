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

package international.dmc.secom_mms_gateway.controllers.management;

import international.dmc.secom_mms_gateway.exceptions.SubscriptionException;
import international.dmc.secom_mms_gateway.model.Subscription;
import international.dmc.secom_mms_gateway.services.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
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
        } catch (SubscriptionException e) {
            return switch (e.getSubscriptionFailure()) {
                case SUBSCRIPTION_ALREADY_EXISTS -> ResponseEntity.status(HttpStatus.CONFLICT).build();
                case SECOM_CLIENT_CREATION_FAILED -> ResponseEntity.badRequest().build();
            };
        }
        return ResponseEntity.ok(newSubscription);
    }

    @GetMapping(
            value = "/subscription/mrn/{mrn}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Subscription> getSubscriptionByMrn(@PathVariable String mrn) {
        Subscription subscription = subscriptionService.getSubscriptionByMrn(mrn);
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(subscription);
    }

    @GetMapping(
            value = "/subscription/id/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Subscription> getSubscriptionById(@PathVariable String id) {
        Subscription subscription = subscriptionService.getSubscriptionById(id);
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
        Subscription subscription = subscriptionService.getSubscriptionByMrn(mrn);
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }
        boolean removed = subscriptionService.unsubscribeAndRemoveSubscription(mrn);
        if (!removed) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(
            value = "/subscription/id/{id}",
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<Subscription> deleteSubscriptionById(@PathVariable String id) {
        Subscription subscription = subscriptionService.getSubscriptionById(id);
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }
        boolean removed = subscriptionService.unsubscribeAndRemoveSubscription(subscription.getServiceMrn());
        if (!removed) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }
}
