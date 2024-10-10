package international.dmc.secom_mms_gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.springboot3.components.SecomClient;

import java.util.UUID;

@Data
@NoArgsConstructor
public class Subscription implements JsonSerializable {
    private String serviceMrn;
    private String serviceUrl;
    private ContainerTypeEnum containerType;
    private String dataProductType;
    private String dataReference;
    private UUID subscriptionId;
    private String mmsSubject;
    @JsonIgnore
    private SecomClient secomClient;
}
