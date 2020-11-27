package kt.i2121

import io.jooby.openapi.OpenAPIResult
import io.jooby.openapi.OpenAPITest
import org.junit.jupiter.api.Assertions

class Issue2121 {

  @OpenAPITest(value = App2121::class)
  fun shouldParseInstallApp(result: OpenAPIResult) {
    Assertions.assertEquals("""openapi: 3.0.1
info:
  title: 2121 API
  description: 2121 API description
  version: "1.0"
paths:
  /:
    get:
      summary: Values for single ID
      description: Delivers full data for an ID for a given year
      operationId: listDataForID
      parameters:
      - name: year
        in: query
        description: The year where the data will be retrieved
        required: true
        schema:
          type: integer
          format: int32
        example: "2018"
      - name: id
        in: query
        description: An ID value which belongs to a dataset
        required: true
        schema:
          type: string
        example: XD12345
      responses:
        "200":
          description: Success
          content:
            application/json:
              schema:
                type: object
""", result.toYaml())
  }
}
