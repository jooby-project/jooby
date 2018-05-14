package org.jooby.apitool;

import apps.App540;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Issue540 {

  @Test
  public void shouldSkipSpecialParameters() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new App540()))
        .next(r -> {
          r.returnType(Cat.class);
          r.pattern("/api/cat/{name}");
          r.description("Another description\n Another line");
          r.summary(null);
          r.param(p -> {
            p.name("name")
                .type(String.class)
                .kind(RouteParameter.Kind.PATH);
          });
        }).done();
  }

  private Path dir() {
    Path userdir = Paths.get(System.getProperty("user.dir"));
    if (!userdir.toString().endsWith("jooby-apitool")) {
      userdir = userdir.resolve("modules").resolve("jooby-apitool");
    }
    return userdir;
  }
}
