package org.jooby.apitool;

import org.jooby.Jooby;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class KtResourceTest {

  @Test
  public void shouldWorkWithKotlinResource() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(kt("kt.ResourceApp")))
        .next(r -> {
          r.returnType("java.util.List<kt.KR1>");
          r.pattern("/kr");
          r.description("List KR.");
          r.summary("KR API.");
          r.param(p -> {
            p.name("name")
                .type(String.class)
                .description("KR name.")
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
