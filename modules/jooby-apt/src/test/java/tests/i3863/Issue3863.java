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
        .withTrpcCode(
            source -> {
              assertThat(source)
                  .contains("app.get(path + \"/users.getUserById\", this::trpcGetUserById);")
                  .contains("app.post(path + \"/users.createUser\", this::trpcCreateUser);");
            });
  }

  @Test
  public void mixedAnnotation() throws Exception {
    new ProcessorRunner(new MixedTrpcAnnotation())
        .withTrpcCode(
            source -> {
              assertThat(source)
                  // tRPC
                  .contains("app.get(path + \"/users.getUserById\", this::trpcGetUserById);")
                  .contains("app.post(path + \"/users.createUser\", this::trpcCreateUser);");
            })
        .withSourceCode(
            source -> {
              assertThat(source)
                  // REST
                  .contains("app.get(\"/api/users/{id}\", this::getUserById);")
                  .contains("app.post(\"/api/users\", this::createUser);");
            });
  }

  @Test
  public void mixedMutation() throws Exception {
    new ProcessorRunner(new MixedMutation())
        .withTrpcCode(
            source -> {
              assertThat(source)
                  // tRPC
                  .contains("app.post(path + \"/users.createUser\", this::trpcCreateUser);")
                  .contains("app.post(path + \"/users.updateUser\", this::trpcUpdateUser);")
                  .contains("app.post(path + \"/users.patchUser\", this::trpcPatchUser);")
                  .contains("app.post(path + \"/users.deleteUser\", this::trpcDeleteUser);");
            })
        .withSourceCode(
            source -> {
              assertThat(source)
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
        .withTrpcCode(
            source -> {
              assertThat(source)
                  // tRPC
                  .contains("app.get(path + \"/users.ping\", this::trpcPing);")
                  .contains("app.get(path + \"/users.ping.since\", this::trpcPingInteger);");
            });
  }
}
