package org.jooby.apitool;

import apps.App946;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Issue946 {

  @Test
  public void shouldProcessNestedPath() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new App946()))
        .next(r -> {
          r.returnType(int.class);
          r.pattern("/some/path/{id}");
          r.description("GET.");
          r.summary("Top.");
          r.param(p -> {
            p.name("id");
            p.type(int.class);
            p.description("Param ID.");
          });
        })
        .next(r -> {
          r.returnType(int.class);
          r.pattern("/some/path/{id}/foo");
          r.description("GET foo.");
          r.summary("Top.");
          r.param(p -> {
            p.name("id");
            p.type(int.class);
            p.description("Param ID.");
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
