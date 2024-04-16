package international.dmc.secommmsgateway.controllers.secom;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.interfaces.UploadSecomInterface;
import org.grad.secom.core.models.UploadObject;
import org.grad.secom.core.models.UploadResponseObject;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.ws.rs.Path;

@Component
@Path("/")
@Validated
@Slf4j
public class UploadSecomController implements UploadSecomInterface {

    @Tag(name = "SECOM")
    @Override
    public UploadResponseObject upload(@Valid UploadObject uploadObject) {
        byte[] data = uploadObject.getEnvelope().getData();

        return null;
    }
}
