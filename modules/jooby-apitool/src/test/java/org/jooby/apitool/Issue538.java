package org.jooby.apitool;

import apps.App538;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Issue538 {

  @Test
  public void shouldWorkWithInnerClasses() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new App538()))
        .next(r -> {
          r.returnType(Cat.class);
          r.pattern("/api/inner/cat/{name}");
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
