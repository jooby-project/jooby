/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3820;

import static org.assertj.core.api.Assertions.assertThat;

import io.jooby.openapi.CurrentDir;
import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue3820 {
  @OpenAPITest(value = App3820a.class)
  public void shouldGenerateRequestBodySchema(OpenAPIResult result) {
    assertThat(result.toAsciiDoc(CurrentDir.testClass(getClass(), "schema.adoc")))
        .isEqualToIgnoringNewLines(
            """
            [source, json]
            ----
            {
              "isbn" : "string",
              "title" : "string",
              "publicationDate" : "date",
              "text" : "string",
              "type" : "string",
              "publisher" : {
                "id" : "int64",
                "name" : "string"
              },
              "authors" : [ {
                "ssn" : "string",
                "name" : "string",
                "address" : {
                  "street" : "string",
                  "city" : "string",
                  "zip" : "string"
                }
              } ]
            }
            ----\
            """);
  }
}
