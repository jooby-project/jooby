/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3059;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue3059 {

  @OpenAPITest(value = App3059A.class)
  public void itFindsDirectlyRegisteredMvcRoutes(OpenAPIResult result) {
    System.out.println(result.toYaml());
    assertEquals(
        result.toYaml(),
        """
      openapi: 3.0.1
      info:
        title: 3059A API
        description: 3059A API description
        version: "1.0"
      paths:
        /mvc/a:
          get:
            operationId: pathA
            responses:
              "200":
                description: Success
                content:
                  application/json:
                    schema:
                      type: string
        /mvc/b:
          post:
            operationId: pathB
            responses:
              "200":
                description: Success
      """);
  }

  @OpenAPITest(value = App3059B.class)
  public void ItDoesNotPullInOtherDirectlyRegisteredMvcRoutes(OpenAPIResult result) {
    assertEquals(
        result.toYaml(),
        """
      openapi: 3.0.1
      info:
        title: 3059B API
        description: 3059B API description
        version: "1.0"
      paths:
        /mvc/a:
          get:
            operationId: pathA
            responses:
              "200":
                description: Success
                content:
                  application/json:
                    schema:
                      type: string
        /mvc/c:
          put:
            operationId: pathC
            responses:
              "200":
                description: Success
      """);
  }
}
