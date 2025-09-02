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

package international.dmc.secom_mms_gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.grad.secom.springboot3.components.SecomClient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Model object representing a subscription")
public class Subscription implements JsonSerializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @Column(name = "service_mrn", unique = true, nullable = false)
    @Schema(description = "The MRN of the SECOM service to subscribe to", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceMrn;

    @Column(name = "service_url", nullable = false)
    @Schema(description = "The URL of the SECOM service to subscribe to", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceUrl;

    @Column(name = "container_type")
    @Schema(description = "The container type to be used for the subscription")
    private ContainerTypeEnum containerType;

    @Column(name = "data_product_type")
    @Schema(description = "The data product type to be used for the subscription")
    private SECOM_DataProductType dataProductType;

    @Column(name = "data_reference")
    @Schema(description = "A data reference to be used for the subscription")
    private String dataReference;

    @Column(name = "subscription_id")
    @Schema(description = "The subscription ID given by the subscribed SECOM service", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID subscriptionId;

    @Column(name = "mms_subject")
    @Schema(description = "The MMS subject where messages received from the SECOM service shall be published to", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mmsSubject;

    @JsonIgnore
    @Transient
    private SecomClient secomClient;
}
