package international.dmc.secom_mms_gateway.controllers.secom;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.grad.secom.core.interfaces.PingSecomInterface;
import org.grad.secom.core.models.PingResponseObject;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.Path;

@Component
@Path("/")
public class PingSecomController implements PingSecomInterface {

    @Tag(name = "SECOM")
    @Override
    public PingResponseObject ping() {
        return new PingResponseObject();
    }
}
