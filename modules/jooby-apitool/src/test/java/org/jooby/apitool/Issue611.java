package org.jooby.apitool;

import apps.App611;
import apps.User611;
import com.google.inject.util.Types;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class Issue611 {

  @Test
  public void shouldGetBodyAndQueryParameters() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir())
        .modify(r -> r.name().orElse("").endsWith("findUsersByEmail"), route -> {
          route.response().type(Types.listOf(User611.class));
          route.param("emails", p -> {
            p.type(Types.listOf(String.class));
          });
        })
        .parseFully(new App611()))
        .next(m -> {
          m.returnType(Types.listOf(User611.class));
          m.param(param -> {
            param
                .name("userId")
                .type(Long.class)
                .value(null)
                .kind(RouteParameter.Kind.QUERY)
                .description("User ID.");
          }).param(param -> {
            param
                .name("context")
                .type(Types.newParameterizedType(Optional.class, String.class))
                .value(null)
                .kind(RouteParameter.Kind.QUERY)
                .description("Context value.");
          }).param(param -> {
            param
                .name("emails")
                .type(Types.listOf(String.class))
                .value(null)
                .kind(RouteParameter.Kind.BODY)
                .description("List of emails.");
          });
        })
        .done();
  }

  private Path dir() {
    Path userdir = Paths.get(System.getProperty("user.dir"));
    if (!userdir.toString().endsWith("jooby-apitool")) {
      userdir = userdir.resolve("modules").resolve("jooby-apitool");
    }
    return userdir;
  }
}
