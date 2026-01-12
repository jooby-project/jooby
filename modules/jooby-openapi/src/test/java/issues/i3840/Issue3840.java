/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3840;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue3840 {
  @OpenAPITest(value = App3840.class)
  public void shouldNotThrowNPE(OpenAPIResult result) {
    assertThat(result).isNotNull();
  }
}
