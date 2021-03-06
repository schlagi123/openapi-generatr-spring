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

package com.github.hauner.openapi.spring.converter

import com.github.hauner.openapi.spring.converter.mapping.AmbiguousTypeMappingException
import com.github.hauner.openapi.spring.converter.mapping.EndpointTypeMapping
import com.github.hauner.openapi.spring.converter.mapping.ParameterTypeMapping
import com.github.hauner.openapi.spring.converter.mapping.ResponseTypeMapping
import com.github.hauner.openapi.spring.converter.mapping.TypeMapping
import com.github.hauner.openapi.spring.model.Api
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.hauner.openapi.spring.support.OpenApiParser.parse

class DataTypeConverterObjectTypeMappingSpec extends Specification {

    void "converts named schemas to java type via global type mapping" () {
        def openApi = parse ("""\
openapi: 3.0.2
info:
  title: API
  version: 1.0.0

paths:
  /page:
    get:
      parameters:
        - in: query
          name: pageable
          required: false
          schema:
            \$ref: '#/components/schemas/Pageable'
      responses:
        '200':
          description: none
          content:
            application/json:
              schema:
                \$ref: '#/components/schemas/StringPage'

components:
  schemas:

    Pageable:
      description: minimal Pageable query parameters
      type: object
      properties:
        page:
          type: integer
        size:
          type: integer

    Page:
      description: minimal Page response without content property
      type: object
      properties:
        number:
          type: integer
        size:
          type: integer

    StringContent:
      description: specific content List of the Page response
      type: object
      properties:
        content:
          type: array
          items:
            type: string

    StringPage:
      description: typed Page
      type: object
      allOf:
        - \$ref: '#/components/schemas/Page'
        - \$ref: '#/components/schemas/StringContent'
""")

        when:
        def options = new ApiOptions(
            packageName: 'pkg',
            typeMappings: [
                new TypeMapping (
                    sourceTypeName: 'Pageable',
                    targetTypeName: 'org.springframework.data.domain.Pageable'),
                new TypeMapping (
                    sourceTypeName: 'StringPage',
                    targetTypeName: 'org.springframework.data.domain.Page',
                    genericTypeNames: ['java.lang.String'])
            ])
        Api api = new ApiConverter (options).convert (openApi)

        then:
        def itf = api.interfaces.first ()
        def ep = itf.endpoints.first ()
        def parameter = ep.parameters.first ()
        def response = ep.response
        parameter.dataType.packageName == 'org.springframework.data.domain'
        parameter.dataType.name == 'Pageable'
        response.responseType.packageName == 'org.springframework.data.domain'
        response.responseType.name == 'Page<String>'
    }

    void "throws when there are multiple global mappings for a named schema" () {
        def openApi = parse ("""\
openapi: 3.0.2
info:
  title: API
  version: 1.0.0

paths:
  /page:
    get:
      parameters:
        - in: query
          name: pageable
          required: false
          schema:
            \$ref: '#/components/schemas/Pageable'
      responses:
        '204':
          description: none

components:
  schemas:

    Pageable:
      description: minimal Pageable query parameters
      type: object
      properties:
        page:
          type: integer
        size:
          type: integer
""")

        when:
        def options = new ApiOptions(
            packageName: 'pkg',
            typeMappings: [
                new TypeMapping (
                    sourceTypeName: 'Pageable',
                    targetTypeName: 'org.springframework.data.domain.Pageable'),
                new TypeMapping (
                    sourceTypeName: 'Pageable',
                    targetTypeName: 'org.springframework.data.domain.Pageable')
            ])
        new ApiConverter (options).convert (openApi)

        then:
        def e = thrown (AmbiguousTypeMappingException)
        e.typeMappings == options.typeMappings
    }

    void "converts named schemas to java type via endpoint type mapping" () {
        def openApi = parse ("""\
openapi: 3.0.2
info:
  title: API
  version: 1.0.0

paths:
  /foobar:
    get:
      parameters:
        - in: query
          name: foo
          required: false
          schema:
            \$ref: '#/components/schemas/Foo'
      responses:
        '200':
          description: none
          content:
            application/json:
              schema:
                \$ref: '#/components/schemas/Bar'

components:
  schemas:

    Foo:
      description: minimal query parameter object
      type: object
      properties:
        foo:
          type: string

    Bar:
      description: minimal response object
      type: object
      properties:
        bar:
          type: string

""")

        when:
        def options = new ApiOptions(
            packageName: 'pkg',
            typeMappings: [
                new EndpointTypeMapping (path: '/foobar',
                    typeMappings: [
                        new TypeMapping (
                            sourceTypeName: 'Foo',
                            targetTypeName: 'someA.ObjectA'),
                        new TypeMapping (
                            sourceTypeName: 'Bar',
                            targetTypeName: 'someB.ObjectB')])
            ])
        Api api = new ApiConverter (options).convert (openApi)

        then:
        def itf = api.interfaces.first ()
        def ep = itf.endpoints.first ()
        def parameter = ep.parameters.first ()
        def response = ep.response
        parameter.dataType.packageName == 'someA'
        parameter.dataType.name == 'ObjectA'
        response.responseType.packageName == 'someB'
        response.responseType.name == 'ObjectB'
    }

    @Unroll
    void "converts object parameter schema to java type via #type" () {
        def openApi = parse ("""\
openapi: 3.0.2
info:
  title: API
  version: 1.0.0

paths:
  /foobar:
    get:
      parameters:
        - in: query
          name: foobar
          required: false
          schema:
            type: object
            properties:
              foo:
                type: integer
              bar:
                type: integer
      responses:
        '204':
          description: empty
""")

        when:
        def options = new ApiOptions(packageName: 'pkg', typeMappings: mappings)
        Api api = new ApiConverter (options).convert (openApi)

        then:
        def itf = api.interfaces.first ()
        def ep = itf.endpoints.first ()
        ep.parameters.first ().dataType.name == 'TargetClass'

        where:
        type << [
            'endpoint parameter mapping',
            'global parameter mapping'
        ]

        mappings << [
            [
                new EndpointTypeMapping (path: '/foobar',
                    typeMappings: [
                        new ParameterTypeMapping (
                            parameterName: 'foobar',
                            mapping: new TypeMapping (
                                targetTypeName: 'pkg.TargetClass')
                        )
                    ])
            ], [
                new ParameterTypeMapping (
                    parameterName: 'foobar',
                    mapping: new TypeMapping (
                        targetTypeName: 'pkg.TargetClass')
                )
            ]
        ]
    }

    @Unroll
    void "converts object response schema to java type via #type" () {
        def openApi = parse ("""\
openapi: 3.0.2
info:
  title: API
  version: 1.0.0

paths:
  /object:
    get:
      responses:
        '200':
          content:
            application/vnd.any:
              schema:
                type: object
                properties:
                  prop:
                    type: string
          description: none              
""")

        when:
        def options = new ApiOptions(
            packageName: 'pkg',
            typeMappings: mappings)
        Api api = new ApiConverter (options).convert (openApi)

        then:
        def itf = api.interfaces.first ()
        def ep = itf.endpoints.first ()
        ep.response.responseType.name == 'TargetClass<String>'
        ep.response.responseType.imports == ['pkg.TargetClass', 'java.lang.String'] as Set

        where:
        type << [
            'endpoint response mapping',
            'global response mapping',
            'endpoint response mapping over endpoint type mapping',
            'endpoint type mapping'
        ]

        mappings << [
            [
                new EndpointTypeMapping (path: '/object',
                    typeMappings: [
                        new ResponseTypeMapping (
                            contentType: 'application/vnd.any',
                            mapping: new TypeMapping (
                                sourceTypeName: 'object',
                                targetTypeName: 'pkg.TargetClass',
                                genericTypeNames: ['java.lang.String'])
                        )
                    ]
                )
            ], [
                new ResponseTypeMapping (
                    contentType: 'application/vnd.any',
                    mapping: new TypeMapping (
                        sourceTypeName: 'object',
                        targetTypeName: 'pkg.TargetClass',
                        genericTypeNames: ['java.lang.String'])
                )
            ], [
                new EndpointTypeMapping (path: '/object',
                    typeMappings: [
                        new ResponseTypeMapping (
                            contentType: 'application/vnd.any',
                            mapping: new TypeMapping (
                                sourceTypeName: 'object',
                                targetTypeName: 'pkg.TargetClass',
                                genericTypeNames: ['java.lang.String'])
                        ),
                        new TypeMapping (
                            sourceTypeName: 'ObjectResponse200',
                            targetTypeName: 'pkg.TargetClassType',
                            genericTypeNames: ['java.lang.StringType'])
                    ]
                )
            ], [
                new EndpointTypeMapping (path: '/object',
                    typeMappings: [
                    new TypeMapping (
                        sourceTypeName: 'ObjectResponse200',
                        targetTypeName: 'pkg.TargetClass',
                        genericTypeNames: ['java.lang.String'])
                    ]
                )
            ]
        ]
    }

    void "converts query param object schema to Map<> set via mapping" () {
        def openApi = parse ("""\
openapi: 3.0.2
info:
  title: API
  version: 1.0.0

paths:
  /endpoint-map:
    get:
      parameters:
        - name: props
          description: query, map from single property
          in: query
          required: false
          schema:
            \$ref: '#/components/schemas/Props'
      responses:
        '204':
          description: empty
          
components:

  schemas:

    Props:
      type: object
      properties:
        prop1:
          type: string
        prop2:
          type: string
""")

        when:
        def options = new ApiOptions(
            packageName: 'pkg',
            typeMappings: [
                new EndpointTypeMapping (path: '/endpoint-map',
                    typeMappings: [
                        new TypeMapping (
                            sourceTypeName: 'Props',
                            targetTypeName: 'java.util.Map',
                            genericTypeNames: ['java.lang.String', 'java.lang.String'])
                        ])
            ])
        Api api = new ApiConverter (options).convert (openApi)

        then:
        def itf = api.interfaces.first ()
        def ep = itf.endpoints.first ()
        def p = ep.parameters.first ()
        p.dataType.name == 'Map<String, String>'
    }

    void "converts query param object schema to MultiValueMap<> set via mapping" () {
        def openApi = parse ("""\
openapi: 3.0.2
info:
  title: API
  version: 1.0.0

paths:
  /endpoint-map:
    get:
      parameters:
        - name: props
          description: query, map from single property
          in: query
          required: false
          schema:
            \$ref: '#/components/schemas/Props'
      responses:
        '204':
          description: empty
          
components:

  schemas:

    Props:
      type: object
      properties:
        prop1:
          type: string
        prop2:
          type: string
""")

        when:
        def options = new ApiOptions(
            packageName: 'pkg',
            typeMappings: [
                new EndpointTypeMapping (path: '/endpoint-map',
                    typeMappings: [
                        new TypeMapping (
                            sourceTypeName: 'Props',
                            targetTypeName: 'org.springframework.util.MultiValueMap',
                            genericTypeNames: ['java.lang.String', 'java.lang.String'])
                        ])
            ])
        Api api = new ApiConverter (options).convert (openApi)

        then:
        def itf = api.interfaces.first ()
        def ep = itf.endpoints.first ()
        def p = ep.parameters.first ()
        p.dataType.name == 'MultiValueMap<String, String>'
    }

}
