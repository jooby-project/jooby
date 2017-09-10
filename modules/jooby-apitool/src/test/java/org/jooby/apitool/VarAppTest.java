package org.jooby.apitool;

import apps.Tag;
import apps.VarApp;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class VarAppTest {

  @Test
  public void shouldWorkWithVar() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parse(VarApp.class.getName()))
        .next(r -> {
          r.returnType(Tag.class);
          r.pattern("/tag/{id}");
          r.description("Tag doc.");
          r.summary(null);
          r.param(p -> {
            p.name("id")
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
