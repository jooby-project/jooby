/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3863;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3863 {
  @Test
  public void trpcOnTopLevelDoesNothingByItSelf() throws Exception {
    new ProcessorRunner(new TopLevelTrpcDoesNothingByItSelf()).withSourceCode(Assert::assertNull);
  }

  @Test
  public void trpcQuery() throws Exception {
    new ProcessorRunner(new SpecificTrpcAnnotation())
        .withSourceCode(
            source -> {
              assertThat(source)
                  .contains("app.get(\"/trpc/users.getUserById\", this::trpcGetUserById);")
                  .contains("app.post(\"/trpc/users.createUser\", this::trpcCreateUser);");
            });
  }

  @Test
  public void mixedAnnotation() throws Exception {
    new ProcessorRunner(new MixedTrpcAnnotation())
        .withSourceCode(
            source -> {
              assertThat(source)
                  // tRPC
                  .contains("app.get(\"/trpc/users.getUserById\", this::trpcGetUserById);")
                  .contains("app.post(\"/trpc/users.createUser\", this::trpcCreateUser);")
                  // REST
                  .contains("app.get(\"/api/users/{id}\", this::getUserById);")
                  .contains("app.post(\"/api/users\", this::createUser);");
            });
  }

  @Test
  public void mixedMutation() throws Exception {
    new ProcessorRunner(new MixedMutation())
        .withSourceCode(
            source -> {
              assertThat(source)
                  // tRPC
                  .contains("app.post(\"/trpc/users.createUser\", this::trpcCreateUser);")
                  .contains("app.post(\"/trpc/users.updateUser\", this::trpcUpdateUser);")
                  .contains("app.post(\"/trpc/users.patchUser\", this::trpcPatchUser);")
                  .contains("app.post(\"/trpc/users.deleteUser\", this::trpcDeleteUser);")
                  // REST
                  .contains("app.post(\"/\", this::createUser);")
                  .contains("app.put(\"/\", this::updateUser);")
                  .contains("app.patch(\"/\", this::patchUser);")
                  .contains("app.delete(\"/\", this::deleteUser);");
            });
  }

  @Test
  public void overloadTrpc() throws Exception {
    new ProcessorRunner(new OverloadTrpc())
        .withSourceCode(
            source -> {
              assertThat(source)
                  // tRPC
                  .contains("app.get(\"/trpc/users.ping\", this::trpcPing);")
                  .contains("app.get(\"/trpc/users.ping\", this::trpcPingInteger);");
            });
  }
}
