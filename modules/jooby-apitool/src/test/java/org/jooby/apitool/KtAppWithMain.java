package org.jooby.apitool;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class KtAppWithMain {

  @Test
  public void shouldWorkUsingMainKtClass() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parse("kt.KtAppWithMainKt"))
        .next(r -> {
          r.returnType("java.util.List<parser.Foo>");
          r.pattern("/");
          r.description(null);
          r.summary(null);
        }).done();
  }

  @Test
  public void shouldWorkUsingMainKtClassJoobyRun() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parse("kt.KtAppWithMainJoobyRunKt"))
        .next(r -> {
          r.returnType("java.util.List<parser.Foo>");
          r.pattern("/");
          r.description(null);
          r.summary(null);
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
