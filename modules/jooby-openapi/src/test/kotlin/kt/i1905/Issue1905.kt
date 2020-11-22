package kt.i1905

import io.jooby.openapi.OpenAPIResult
import io.jooby.openapi.OpenAPITest
import org.junit.jupiter.api.Assertions

class Issue1905 {

  @OpenAPITest(value = App1905::class)
  fun shouldParseInstallApp(result: OpenAPIResult) {
    Assertions.assertEquals("""openapi: 3.0.1
info:
  title: 1905 API
  description: 1905 API description
  version: "1.0"
paths:
  /sub:
    get:
      operationId: getSub
      responses:
        "200":
          description: Success
          content:
            application/json:
              schema:
                type: string
  /static/ref/sub:
    get:
      operationId: getStaticRefSub
      responses:
        "200":
          description: Success
          content:
            application/json:
              schema:
                type: string
  /instance/ref/sub:
    get:
      operationId: getInstanceRefSub
      responses:
        "200":
          description: Success
          content:
            application/json:
              schema:
                type: string
  /supplier/sub:
    get:
      operationId: getSupplierSub
      responses:
        "200":
          description: Success
          content:
            application/json:
              schema:
                type: string
""", result.toYaml())
  }
}
