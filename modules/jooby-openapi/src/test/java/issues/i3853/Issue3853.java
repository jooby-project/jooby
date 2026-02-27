/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3853;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import io.swagger.v3.oas.models.SpecVersion;

public class Issue3853 {
  @OpenAPITest(value = App3853.class)
  public void shouldDetectProjectAnnotation(OpenAPIResult result) {
    assertThat(result.toYaml())
        .isEqualToIgnoringNewLines(
            """
            openapi: 3.0.1
            info:
              title: 3853 API
              description: 3853 API description
              version: "1.0"
            paths:
              /3853/{id}:
                get:
                  operationId: findUser
                  parameters:
                  - name: id
                    in: path
                    required: true
                    schema:
                      type: string
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/U3853_heab3f"
              /3853:
                get:
                  operationId: findUsers
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            type: array
                            items:
                              $ref: "#/components/schemas/U3853_heab3f"
              /3853/optional:
                get:
                  operationId: findUserIdOnly
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/U3853_2l7"
              /3853/full-address/{id}:
                get:
                  operationId: userIdWithFullAddress
                  parameters:
                  - name: id
                    in: path
                    required: true
                    schema:
                      type: string
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/U3853_ff52rt"
              /3853/partial-address/{id}:
                get:
                  operationId: userIdWithAddressCity
                  parameters:
                  - name: id
                    in: path
                    required: true
                    schema:
                      type: string
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/U3853_xsv0o"
            components:
              schemas:
                U3853:
                  type: object
                  properties:
                    id:
                      type: string
                      description: Retrieves the unique identifier for the user.
                    name:
                      type: string
                      description: Retrieves the name of the user.
                    address:
                      $ref: "#/components/schemas/A3853"
                    roles:
                      type: array
                      description: Retrieves the list of roles associated with the user.
                      items:
                        $ref: "#/components/schemas/R3853"
                    meta:
                      type: object
                      additionalProperties:
                        type: string
                      description: Retrieves the metadata associated with the user.
                  description: "Represents a user entity identified by an ID and name, with associated\\
                    \\ address details, roles, and metadata. This class is immutable, ensuring\\
                    \\ the integrity of its fields."
                L3853:
                  type: object
                  properties:
                    lat:
                      type: number
                      format: double
                    lon:
                      type: number
                      format: double
                R3853:
                  type: object
                  properties:
                    name:
                      type: string
                    level:
                      type: integer
                      format: int32
                A3853:
                  type: object
                  properties:
                    city:
                      type: string
                    loc:
                      $ref: "#/components/schemas/L3853"
                U3853_heab3f:
                  type: object
                  properties:
                    id:
                      type: string
                      description: Retrieves the unique identifier for the user.
                    name:
                      type: string
                      description: Retrieves the name of the user.
                  description: "Represents a user entity identified by an ID and name, with associated\\
                    \\ address details, roles, and metadata. This class is immutable, ensuring\\
                    \\ the integrity of its fields."
                U3853_2l7:
                  type: object
                  properties:
                    id:
                      type: string
                      description: Retrieves the unique identifier for the user.
                  description: "Represents a user entity identified by an ID and name, with associated\\
                    \\ address details, roles, and metadata. This class is immutable, ensuring\\
                    \\ the integrity of its fields."
                U3853_ff52rt:
                  type: object
                  properties:
                    id:
                      type: string
                      description: Retrieves the unique identifier for the user.
                    address:
                      $ref: "#/components/schemas/A3853"
                  description: "Represents a user entity identified by an ID and name, with associated\\
                    \\ address details, roles, and metadata. This class is immutable, ensuring\\
                    \\ the integrity of its fields."
                A3853_1tgff:
                  type: object
                  properties:
                    city:
                      type: string
                U3853_xsv0o:
                  type: object
                  properties:
                    id:
                      type: string
                      description: Retrieves the unique identifier for the user.
                    address:
                      $ref: "#/components/schemas/A3853_1tgff"
                  description: "Represents a user entity identified by an ID and name, with associated\\
                    \\ address details, roles, and metadata. This class is immutable, ensuring\\
                    \\ the integrity of its fields."
            """);
  }

  @OpenAPITest(value = App3853.class, version = SpecVersion.V31)
  public void shouldDetectProjectAnnotationV31(OpenAPIResult result) {
    assertThat(result.toYaml())
        .isEqualToIgnoringNewLines(
            """
            openapi: 3.1.0
            info:
              title: 3853 API
              description: 3853 API description
              version: "1.0"
            paths:
              /3853/{id}:
                get:
                  operationId: findUser
                  parameters:
                  - name: id
                    in: path
                    required: true
                    schema:
                      type: string
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/U3853_heab3f"
              /3853:
                get:
                  operationId: findUsers
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            type: array
                            items:
                              $ref: "#/components/schemas/U3853_heab3f"
              /3853/optional:
                get:
                  operationId: findUserIdOnly
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/U3853_2l7"
              /3853/full-address/{id}:
                get:
                  operationId: userIdWithFullAddress
                  parameters:
                  - name: id
                    in: path
                    required: true
                    schema:
                      type: string
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/U3853_ff52rt"
              /3853/partial-address/{id}:
                get:
                  operationId: userIdWithAddressCity
                  parameters:
                  - name: id
                    in: path
                    required: true
                    schema:
                      type: string
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/U3853_xsv0o"
            components:
              schemas:
                U3853:
                  type: object
                  description: "Represents a user entity identified by an ID and name, with associated\\
                    \\ address details, roles, and metadata. This class is immutable, ensuring\\
                    \\ the integrity of its fields."
                  properties:
                    id:
                      type: string
                      description: Retrieves the unique identifier for the user.
                    name:
                      type: string
                      description: Retrieves the name of the user.
                    address:
                      $ref: "#/components/schemas/A3853"
                      description: Retrieves the address associated with the user.
                    roles:
                      type: array
                      description: Retrieves the list of roles associated with the user.
                      items:
                        $ref: "#/components/schemas/R3853"
                    meta:
                      type: object
                      additionalProperties:
                        type: string
                      description: Retrieves the metadata associated with the user.
                L3853:
                  type: object
                  properties:
                    lat:
                      type: number
                      format: double
                    lon:
                      type: number
                      format: double
                R3853:
                  type: object
                  properties:
                    name:
                      type: string
                    level:
                      type: integer
                      format: int32
                A3853:
                  type: object
                  properties:
                    city:
                      type: string
                    loc:
                      $ref: "#/components/schemas/L3853"
                U3853_heab3f:
                  type: object
                  description: "Represents a user entity identified by an ID and name, with associated\\
                    \\ address details, roles, and metadata. This class is immutable, ensuring\\
                    \\ the integrity of its fields."
                  properties:
                    id:
                      type: string
                      description: Retrieves the unique identifier for the user.
                    name:
                      type: string
                      description: Retrieves the name of the user.
                U3853_2l7:
                  type: object
                  description: "Represents a user entity identified by an ID and name, with associated\\
                    \\ address details, roles, and metadata. This class is immutable, ensuring\\
                    \\ the integrity of its fields."
                  properties:
                    id:
                      type: string
                      description: Retrieves the unique identifier for the user.
                U3853_ff52rt:
                  type: object
                  description: "Represents a user entity identified by an ID and name, with associated\\
                    \\ address details, roles, and metadata. This class is immutable, ensuring\\
                    \\ the integrity of its fields."
                  properties:
                    id:
                      type: string
                      description: Retrieves the unique identifier for the user.
                    address:
                      $ref: "#/components/schemas/A3853"
                      description: Retrieves the address associated with the user.
                A3853_1tgff:
                  type: object
                  properties:
                    city:
                      type: string
                U3853_xsv0o:
                  type: object
                  description: "Represents a user entity identified by an ID and name, with associated\\
                    \\ address details, roles, and metadata. This class is immutable, ensuring\\
                    \\ the integrity of its fields."
                  properties:
                    id:
                      type: string
                      description: Retrieves the unique identifier for the user.
                    address:
                      $ref: "#/components/schemas/A3853_1tgff"
            """);
  }
}
