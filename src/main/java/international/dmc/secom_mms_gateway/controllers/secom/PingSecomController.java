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
