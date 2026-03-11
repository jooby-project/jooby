/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3863;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3863 {
  @Test
  public void shouldGenerateTrpcService() throws Exception {
    new ProcessorRunner(new C3863())
        .withSourceCode(
            source -> {
              assertThat(source)
                  .contains("app.get(\"/trpc/users.ping\", this::trpcPingInteger);")
                  .contains("app.get(\"/trpc/users.ping\", this::trpcPing);")
                  .contains(
                      "public io.jooby.trpc.TrpcResponse<java.util.List<tests.i3863.U3863>>"
                          + " trpcMultipleSimpleArgs(io.jooby.Context ctx) throws Exception {");
            });
  }
}
