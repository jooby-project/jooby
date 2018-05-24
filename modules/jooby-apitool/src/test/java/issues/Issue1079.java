package issues;

import kt.App1079;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteMethodAssert;
import org.jooby.apitool.RouteParameter;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Issue1079 {

  @Test
  public void shouldDetectKotlinNativeType() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1079());

    new RouteMethodAssert(routes)
        .next(r -> {
          r.returnType(String.class);
          r.method("GET");
          r.pattern("/");
          r.description(null);
          r.summary(null);
          r.returns(null);
          r.param(p -> {
            p.type("java.util.Optional<java.lang.Integer>");
            p.name("p1");
            p.kind(RouteParameter.Kind.QUERY);
          }).param(p -> {
            p.type("java.util.Optional<java.lang.Integer>");
            p.name("p2");
            p.kind(RouteParameter.Kind.QUERY);
          }).param(p -> {
            p.type("int");
            p.name("p3");
            p.kind(RouteParameter.Kind.QUERY);
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
