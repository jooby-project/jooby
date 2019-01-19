package issues;

import kt.App1261;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.ApiToolFeature;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteMethodAssert;
import org.junit.Test;

import java.util.List;

public class Issue1261 extends ApiToolFeature {

  @Test
  public void shouldContainsSwaggerResponseDescription() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1261());
    new RouteMethodAssert(routes)
        .next(r -> {
          r.returnType("kt.ResultData");
          r.method("GET");
          r.pattern("/");
          r.description(null);
          r.summary(null);
          r.returns(null);
        })
        .done();
  }
}
