package international.dmc.secom_mms_gateway.controllers.secom;

import international.dmc.secom_mms_gateway.utils.DataProductTypeParser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.interfaces.CapabilitySecomInterface;
import org.grad.secom.core.models.CapabilityObject;
import org.grad.secom.core.models.CapabilityResponseObject;
import org.grad.secom.core.models.ImplementedInterfaces;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.ws.rs.Path;
import java.util.Collections;

@Component
@Path("/")
@Validated
@Slf4j
public class CapabilitySecomController implements CapabilitySecomInterface {

    @Value("${international.dmc.secom_mms_gateway.secom.dataProductType:OTHER}")
    private String secomDataProductType;

    @Tag(name = "SECOM")
    @Override
    public CapabilityResponseObject capability() {
        ImplementedInterfaces implementedInterfaces = new ImplementedInterfaces();
        implementedInterfaces.setUpload(true);

        CapabilityObject capabilityObject = new CapabilityObject();
        capabilityObject.setImplementedInterfaces(implementedInterfaces);
        capabilityObject.setServiceVersion("1.0.0");
        capabilityObject.setContainerType(ContainerTypeEnum.S100_DataSet);
        capabilityObject.setDataProductType(DataProductTypeParser.getDataProductType(secomDataProductType));

        CapabilityResponseObject capabilityResponseObject = new CapabilityResponseObject();
        capabilityResponseObject.setCapability(Collections.singletonList(capabilityObject));

        return capabilityResponseObject;
    }
}
