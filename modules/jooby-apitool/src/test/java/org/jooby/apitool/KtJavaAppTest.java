package org.jooby.apitool;

import org.jooby.Jooby;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class KtJavaAppTest {

  @Test
  public void shouldWorkWithJavaAPI() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parse(kt("kt.JavaApp")))
        .next(r -> {
          r.returnType(String.class);
          r.pattern("/java");
          r.description("Java API.");
          r.summary(null);
          r.param(p -> {
            p.name("p1")
                .type(String.class)
                .description(null)
                .kind(RouteParameter.Kind.QUERY);
          });
        }).done();
  }

  private Jooby kt(final String classname) throws Exception {
    return (Jooby) getClass().getClassLoader().loadClass(classname).newInstance();
  }

  private Path dir() {
    Path userdir = Paths.get(System.getProperty("user.dir"));
    if (!userdir.toString().endsWith("jooby-apitool")) {
      userdir = userdir.resolve("modules").resolve("jooby-apitool");
    }
    return userdir;
  }
}
