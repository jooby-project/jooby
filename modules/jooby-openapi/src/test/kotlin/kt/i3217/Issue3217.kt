/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.i3217

import io.jooby.openapi.OpenAPIResult
import io.jooby.openapi.OpenAPITest
import io.jooby.openapi.RouteIterator
import org.junit.jupiter.api.Assertions.assertEquals

class Issue3217 {

  @OpenAPITest(value = App3217::class)
  fun shouldFindBeanTypeOnKtCollections(iterator: RouteIterator) {
    iterator
      .next { route ->
        assertEquals("java.util.List<kt.i3217.SomeBean3217>", route.defaultResponse.javaType)
      }
      .next { route ->
        assertEquals("java.util.List<kt.i3217.SomeBean3217>", route.defaultResponse.javaType)
      }
      .verify()
  }

  @OpenAPITest(value = App3217::class)
  fun shouldFindBeanTypeOnKtCollectionsAndRenderGoodYAML(result: OpenAPIResult) {
    assertEquals(
      "openapi: 3.0.1\n" +
        "info:\n" +
        "  title: 3217 API\n" +
        "  description: 3217 API description\n" +
        "  version: \"1.0\"\n" +
        "paths:\n" +
        "  /api-script/beans:\n" +
        "    get:\n" +
        "      operationId: getApiScriptBeans\n" +
        "      responses:\n" +
        "        \"200\":\n" +
        "          description: Success\n" +
        "          content:\n" +
        "            application/json:\n" +
        "              schema:\n" +
        "                type: array\n" +
        "                items:\n" +
        "                  \$ref: \"#/components/schemas/SomeBean3217\"\n" +
        "  /api-mvc/beans:\n" +
        "    get:\n" +
        "      operationId: getBeans\n" +
        "      responses:\n" +
        "        \"200\":\n" +
        "          description: Success\n" +
        "          content:\n" +
        "            application/json:\n" +
        "              schema:\n" +
        "                type: array\n" +
        "                items:\n" +
        "                  \$ref: \"#/components/schemas/SomeBean3217\"\n" +
        "components:\n" +
        "  schemas:\n" +
        "    SomeBean3217:\n" +
        "      type: object\n" +
        "      properties:\n" +
        "        name:\n" +
        "          type: string\n",
      result.toYaml(),
    )
  }
}
