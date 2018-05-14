package org.jooby.apitool;

import apps.App944;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Issue944 {

  @Test
  public void shouldSkipEmptyResultType() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new App944()))
        .next(r -> {
          r.returnType(void.class);
          r.pattern("/");
          r.description(null);
          r.summary(null);
          r.status(204, "No Content");
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
