/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3864;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3868 {
  @Test
  public void shouldGenerateJsonRpcService() throws Exception {
    new ProcessorRunner(new C3864())
        .withSourceCode(
            source -> {
              assertThat(source)
                  .contains(
                      "public class C3864Rpc_ implements io.jooby.jsonrpc.JsonRpcService,"
                          + " io.jooby.Extension {")
                  .contains("public java.util.List<String> getMethods() {")
                  .contains(
                      "public Object execute(io.jooby.Context ctx, io.jooby.jsonrpc.JsonRpcRequest"
                          + " req) throws Exception {");
            });
  }
}
