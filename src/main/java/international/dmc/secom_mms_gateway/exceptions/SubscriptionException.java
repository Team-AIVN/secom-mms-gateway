package international.dmc.secom_mms_gateway.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SubscriptionException extends Exception {
    private final SubscriptionFailure subscriptionFailure;
}
