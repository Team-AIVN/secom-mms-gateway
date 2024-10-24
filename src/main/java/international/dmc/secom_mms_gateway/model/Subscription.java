package international.dmc.secom_mms_gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.grad.secom.springboot3.components.SecomClient;

import java.util.UUID;

@Data
@NoArgsConstructor
@Schema(description = "Model object representing a subscription")
public class Subscription implements JsonSerializable {
    @Schema(description = "The MRN of the SECOM service to subscribe to", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceMrn;
    @Schema(description = "The URL of the SECOM service to subscribe to", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceUrl;
    @Schema(description = "The container type to be used for the subscription")
    private ContainerTypeEnum containerType;
    @Schema(description = "The data product type to be used for the subscription")
    private SECOM_DataProductType dataProductType;
    @Schema(description = "A data reference to be used for the subscription")
    private String dataReference;
    @Schema(description = "The subscription ID given by the subscribed SECOM service", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID subscriptionId;
    @Schema(description = "The MMS subject where messages received from the SECOM service shall be published to", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mmsSubject;
    @JsonIgnore
    private SecomClient secomClient;
}
