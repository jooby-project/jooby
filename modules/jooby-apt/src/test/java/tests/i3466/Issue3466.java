/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3466;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3466 {

  @Test
  public void shouldGetNameFromParamLookup() throws Exception {
    new ProcessorRunner(new C3466())
        .withRouter(
            (app, source) -> {
              assertTrue(
                  source
                      .toString()
                      .contains(
                          "ctx.lookup(\"client_id\", io.jooby.ParamSource.QUERY,"
                              + " io.jooby.ParamSource.FORM)"));
              assertFalse(source.toString().contains("ignoredBodyName"));
            });
  }
}
