package org.jooby.apitool;

import apps.App537;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Issue537 {

  @Test
  public void shouldWorkWithDefaultConstructor() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new App537()))
        .next(r -> {
          r.returnType(Cat.class);
          r.pattern("/api/cat/{name}");
          r.description(null);
          r.summary("Produces Cat object\n Next line");
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
