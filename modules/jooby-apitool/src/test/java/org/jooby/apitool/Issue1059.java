package org.jooby.apitool;

import apps.App1059;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Issue1059 {

  @Test
  public void topLevelDescriptionShouldWorkWhenAnnotatedContructorIsPresent() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new App1059()))
        .next(r -> {
          r.returnType(String.class);
          r.pattern("/test");
          r.method("GET");
          r.description("Say hi.");
          r.summary("Top level comment.");
        })
        .next(r -> {
          r.returnType(String.class);
          r.pattern("/test/x");
          r.method("GET");
          r.description("Say X.");
          r.summary("Top level comment.");
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
