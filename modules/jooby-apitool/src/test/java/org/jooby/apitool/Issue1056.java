package org.jooby.apitool;

import apps.App1056;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.jooby.Upload;


public class Issue1056 {

  @Test
  public void shouldWorkWithBodilessAndMultiformPosts() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new App1056()))
        .next(r -> {
          r.returnType(void.class);
          r.pattern("/api/inner/cat/{name}/pat");
          r.method("POST");
          r.param(p -> {
            p.name("name")
                .type(String.class)
                .kind(RouteParameter.Kind.PATH);
          });
        })
        .next(r -> {
          r.returnType(void.class);
          r.pattern("/api/inner/cat/{name}/feed");
          r.method("POST");
          r.param(p -> {
            p.name("name")
                .type(String.class)
                .kind(RouteParameter.Kind.PATH);
          }).param(p -> {
            p.name("food")
                .type(Upload.class)
                .kind(RouteParameter.Kind.FILE);
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
