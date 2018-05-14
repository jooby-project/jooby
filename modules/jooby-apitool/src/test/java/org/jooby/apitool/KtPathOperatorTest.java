package org.jooby.apitool;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class KtPathOperatorTest {

  @Test
  public void shouldWorkWithPathOperator() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parse("kt.PathOperatorApp"))
        .next(r -> {
          r.returnType("java.util.List<parser.Foo>");
          r.pattern("/api/path");
          r.description("List all.");
          r.summary("Summary API.");
        })
        .next(r -> {
          r.returnType(parser.Foo.class);
          r.pattern("/api/path/{id}");
          r.description("List one.");
          r.summary("Summary API.");
          r.param(p -> {
            p.name("id")
                .type(int.class)
                .description("ID.")
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
