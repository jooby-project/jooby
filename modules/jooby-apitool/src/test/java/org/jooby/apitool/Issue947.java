package org.jooby.apitool;

import apps.App947;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Issue947 {

  @Test
  public void shouldProcessMvcWithPath() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new App947()))
        .next(r -> {
          r.returnType(String.class);
          r.pattern("/path/mvc");
          r.description("MVC doIt.");
          r.summary("MVC API.");
          r.param(p -> {
            p.name("q");
            p.type(String.class);
            p.description("Query string. Like: <code>q=foo</code>");
          });
        }).done();
  }

  @Test
  public void shouldProcessMvcWithPathFromKotlin() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parse("kt.Kt947"))
        .next(r -> {
          r.returnType(String.class);
          r.pattern("/kpath/mvc");
          r.description("MVC doIt.");
          r.summary("MVC API.");
          r.param(p -> {
            p.name("q");
            p.type(String.class);
            p.description("Query string. Like: <code>q=foo</code>");
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
