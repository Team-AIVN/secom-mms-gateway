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

package international.dmc.secom_mms_gateway.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.grad.secom.core.models.enums.SECOM_DataProductType;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataProductTypeParser {

    public static SECOM_DataProductType getDataProductType(String dataProductType) {
        return switch (dataProductType.toUpperCase()) {
            case "S57" -> SECOM_DataProductType.S57;
            case "S101" -> SECOM_DataProductType.S101;
            case "S102" -> SECOM_DataProductType.S102;
            case "S104" -> SECOM_DataProductType.S104;
            case "S111" -> SECOM_DataProductType.S111;
            case "S122" -> SECOM_DataProductType.S122;
            case "S123" -> SECOM_DataProductType.S123;
            case "S124" -> SECOM_DataProductType.S124;
            case "S125" -> SECOM_DataProductType.S125;
            case "S126" -> SECOM_DataProductType.S126;
            case "S127" -> SECOM_DataProductType.S127;
            case "S128" -> SECOM_DataProductType.S128;
            case "S129" -> SECOM_DataProductType.S129;
            case "S131" -> SECOM_DataProductType.S131;
            case "S201" -> SECOM_DataProductType.S201;
            case "S211" -> SECOM_DataProductType.S211;
            case "S212" -> SECOM_DataProductType.S212;
            case "S401" -> SECOM_DataProductType.S401;
            case "S402" -> SECOM_DataProductType.S402;
            case "S411" -> SECOM_DataProductType.S411;
            case "S412" -> SECOM_DataProductType.S412;
            case "S413" -> SECOM_DataProductType.S413;
            case "S414" -> SECOM_DataProductType.S414;
            case "S421" -> SECOM_DataProductType.S421;
            case "RTZ" -> SECOM_DataProductType.RTZ;
            case "EPC" -> SECOM_DataProductType.EPC;
            default -> SECOM_DataProductType.OTHER;
        };
    }
}
