package org.jooby.apitool;

import apps.Tag;
import apps.VarApp;
import org.junit.Test;
import parser.CommonPathApp;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CommonPathTest {

  @Test
  public void shouldProcessPathOperator() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parse(CommonPathApp.class.getName()))
        .next(r -> {
          r.returnType(String.class);
          r.pattern("/app/foo/1");
          r.description(null);
          r.summary(null);
        })
        .next(r -> {
          r.returnType(String.class);
          r.pattern("/app/bar/1");
          r.description(null);
          r.summary(null);
        })
        .next(r -> {
          r.returnType(String.class);
          r.pattern("/api/pets");
          r.description("List pets.");
          r.summary("API Pets:");
        })
        .next(r -> {
          r.returnType(String.class);
          r.pattern("/api/pets/{id}");
          r.description("Find pet.");
          r.summary("API Pets:");
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
