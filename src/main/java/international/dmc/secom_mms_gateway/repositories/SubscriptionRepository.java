package international.dmc.secom_mms_gateway.repositories;

import international.dmc.secom_mms_gateway.model.Subscription;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends PagingAndSortingRepository<Subscription, Long>, CrudRepository<Subscription, Long> {
    Subscription getSubscriptionByServiceMrn(String serviceMrn);

    Subscription getSubscriptionBySubscriptionId(UUID subscriptionId);

    List<Subscription> getAll();
}
