package issues;

import kt.App1235;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.ApiToolFeature;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteMethodAssert;
import org.junit.Test;

import java.util.List;

public class Issue1235 extends ApiToolFeature {

  @Test
  public void shouldSupportExplicitRequestHandler() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1235());
    new RouteMethodAssert(routes)
        .next(r -> {
          r.returnType("com.typesafe.config.Config");
          r.method("GET");
          r.pattern("/qwe");
          r.description(null);
          r.summary(null);
          r.returns(null);
        })
        .next(r -> {
          r.returnType("com.typesafe.config.Config");
          r.method("GET");
          r.pattern("/asd");
          r.description(null);
          r.summary(null);
          r.returns(null);
        })
        .next(r -> {
          r.returnType("com.typesafe.config.Config");
          r.method("GET");
          r.pattern("/zxc");
          r.description(null);
          r.summary(null);
          r.returns(null);
        })
        .next(r -> {
          r.returnType("com.typesafe.config.Config");
          r.method("GET");
          r.pattern("/rty");
          r.description(null);
          r.summary(null);
          r.returns(null);
        })
        .done();
  }
}
