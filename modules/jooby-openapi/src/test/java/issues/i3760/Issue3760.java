/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3760;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;
import io.swagger.v3.oas.models.SpecVersion;

public class Issue3760 {

  @OpenAPITest(value = App3760.class)
  public void shouldParseJakartaConstraints(OpenAPIResult result) {
    assertThat(result.toYaml())
        .isEqualToIgnoringNewLines(
            """
            openapi: 3.0.1
            info:
              title: 3760 API
              description: 3760 API description
              version: "1.0"
            paths:
              /3760:
                get:
                  summary: Search/scan index.
                  operationId: search
                  parameters:
                  - name: q
                    in: query
                    description: Search string. Defaults to *
                    schema:
                      type: string
                      default: '*'
                  - name: pageSize
                    in: query
                    schema:
                      maximum: 50
                      minimum: 10
                      type: integer
                      format: int32
                      default: 20
                  - name: page
                    in: query
                    schema:
                      minimum: 1
                      type: integer
                      format: int32
                      default: 1
                  responses:
                    "200":
                      description: Search result.
                      content:
                        application/json:
                          schema:
                            type: object
                            additionalProperties:
                              type: object
                post:
                  operationId: save
                  requestBody:
                    content:
                      application/json:
                        schema:
                          $ref: "#/components/schemas/UP3760"
                    required: true
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/UP3760"
              /3760/query:
                get:
                  operationId: query
                  parameters:
                  - name: text
                    in: query
                    schema:
                      maximum: 1000
                      minimum: 10
                      type: string
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/Q3760"
            components:
              schemas:
                Q3760:
                  type: object
                  properties:
                    text:
                      maximum: 1000
                      minimum: 10
                      type: string
                UP3760:
                  type: object
                  properties:
                    id:
                      type: integer
                      format: int64
                      nullable: false
                    resetToken:
                      type: string
                      nullable: true
                    username:
                      minLength: 1
                      pattern: .*\\S.*
                      type: string
                    bio:
                      minLength: 1
                      type: string
                    contactEmail:
                      type: string
                      format: email
                    serialNumber:
                      pattern: "^[A-Z0-9]+$"
                      type: string
                    profileComment:
                      maximum: 200
                      minimum: 10
                      type: string
                    age:
                      maximum: 120
                      minimum: 18
                      type: integer
                      format: int32
                    price:
                      maximum: 999.99
                      minimum: 0.01
                      type: number
                    taxRate:
                      multipleOf: 0.01
                      maximum: 99999.99
                      type: number
                    loyaltyPoints:
                      minimum: 1
                      type: integer
                      format: int32
                    balanceAdjustment:
                      maximum: 0
                      type: integer
                      format: int32
                    dateOfBirth:
                      type: string
                      format: date
                    registeredAt:
                      type: string
                      format: date-time
                    subscriptionExpiry:
                      type: string
                      format: date
                    nextBillingDate:
                      type: string
                      format: date-time
                    termsAccepted:
                      type: boolean
                    isInternalAccount:
                      type: boolean
                    roles:
                      maximum: 5
                      minimum: 1
                      minLength: 1
                      type: array
                      items:
                        type: string\
            """);
  }

  @OpenAPITest(value = App3760.class, version = SpecVersion.V31)
  public void shouldParseJakartaConstraintsV31(OpenAPIResult result) {
    assertThat(result.toYaml())
        .isEqualToIgnoringNewLines(
            """
            openapi: 3.1.0
            info:
              title: 3760 API
              description: 3760 API description
              version: "1.0"
            paths:
              /3760:
                get:
                  summary: Search/scan index.
                  operationId: search
                  parameters:
                  - name: q
                    in: query
                    description: Search string. Defaults to *
                    schema:
                      type: string
                      default: '*'
                  - name: pageSize
                    in: query
                    schema:
                      type: integer
                      format: int32
                      default: 20
                      maximum: 50
                      minimum: 10
                  - name: page
                    in: query
                    schema:
                      type: integer
                      format: int32
                      default: 1
                      minimum: 1
                  responses:
                    "200":
                      description: Search result.
                      content:
                        application/json:
                          schema:
                            type: object
                            additionalProperties:
                              type: object
                post:
                  operationId: save
                  requestBody:
                    content:
                      application/json:
                        schema:
                          $ref: "#/components/schemas/UP3760"
                    required: true
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/UP3760"
              /3760/query:
                get:
                  operationId: query
                  parameters:
                  - name: text
                    in: query
                    schema:
                      type: string
                      maximum: 1000
                      minimum: 10
                  responses:
                    "200":
                      description: Success
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/Q3760"
            components:
              schemas:
                Q3760:
                  type: object
                  properties:
                    text:
                      type: string
                      maximum: 1000
                      minimum: 10
                UP3760:
                  type: object
                  properties:
                    id:
                      type: integer
                      format: int64
                    resetToken:
                      type:
                      - string
                      - "null"
                    username:
                      type: string
                      minLength: 1
                      pattern: .*\\S.*
                    bio:
                      type: string
                      minLength: 1
                    contactEmail:
                      type: string
                      format: email
                    serialNumber:
                      type: string
                      pattern: "^[A-Z0-9]+$"
                    profileComment:
                      type: string
                      maximum: 200
                      minimum: 10
                    age:
                      type: integer
                      format: int32
                      maximum: 120
                      minimum: 18
                    price:
                      type: number
                      maximum: 999.99
                      minimum: 0.01
                    taxRate:
                      type: number
                      maximum: 99999.99
                      multipleOf: 0.01
                    loyaltyPoints:
                      type: integer
                      format: int32
                      minimum: 1
                    balanceAdjustment:
                      type: integer
                      format: int32
                      maximum: 0
                    dateOfBirth:
                      type: string
                      format: date
                    registeredAt:
                      type: string
                      format: date-time
                    subscriptionExpiry:
                      type: string
                      format: date
                    nextBillingDate:
                      type: string
                      format: date-time
                    termsAccepted:
                      type: boolean
                    isInternalAccount:
                      type: boolean
                    roles:
                      type: array
                      items:
                        type: string
                      maximum: 5
                      minLength: 1
                      minimum: 1\
            """);
  }
}
