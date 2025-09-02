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

package international.dmc.secom_mms_gateway.repositories;

import international.dmc.secom_mms_gateway.model.Subscription;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends PagingAndSortingRepository<Subscription, Long>, CrudRepository<Subscription, Long> {
    Subscription getSubscriptionByServiceMrn(String serviceMrn);

    Subscription getSubscriptionBySubscriptionId(UUID subscriptionId);

    void deleteByServiceMrn(String serviceMrn);

    boolean existsByServiceMrn(String serviceMrn);
}
