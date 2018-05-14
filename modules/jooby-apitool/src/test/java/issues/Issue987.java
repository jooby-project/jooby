package issues;

import org.jooby.apitool.ApiParser;
import org.jooby.apitool.RouteMethodAssert;
import org.jooby.apitool.RouteParameter;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Issue987 {

  @Test
  public void shouldNotLostBodyParameterType() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new App987()))
        .next(r -> {
          r.returnType(String.class);
          r.pattern("/api/login");
          r.description("Authenticates a user, generating an Authorization token.");
          r.summary(null);
          r.param(p -> {
            p.name("body")
                .type(LoginRequest.class)
                .kind(RouteParameter.Kind.BODY);
          });
        })
        .next(r -> {
          r.returnType(String.class);
          r.pattern("/use/config");
          r.description(null);
          r.summary(null);
          r.param(p -> {
            p.name("body")
                .type(LoginRequest.class)
                .kind(RouteParameter.Kind.BODY);
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
