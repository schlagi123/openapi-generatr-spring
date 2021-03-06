/*
 * Copyright 2019 the original authors
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

package com.github.hauner.openapi.spring.converter.mapping

/**
 * thrown when the DataTypeConverter finds an ambiguous data type mapping.
 *
 * @author Martin Hauner
 */
class AmbiguousTypeMappingException extends RuntimeException {

    List<TypeMapping> typeMappings

    AmbiguousTypeMappingException(List<TypeMapping> typeMappings) {
        super()
        this.typeMappings = typeMappings
    }

    @Override
    String getMessage () {
        def from = typeMappings.first ()
        def msg = "ambiguous type mapping:\n"
        msg += "  from: ${from.fullSourceType}\n"
        typeMappings.each {
            msg += "  to: ${it.targetTypeName}"
        }
        msg
    }

}
