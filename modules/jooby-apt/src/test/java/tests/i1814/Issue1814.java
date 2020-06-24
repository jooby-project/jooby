package tests.i1814;

import io.jooby.MockContext;
import io.jooby.MockRouter;
import io.jooby.apt.MvcModuleCompilerRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1814 {

  @Test
  public void shouldIgnoreWildcardResponseType() throws Exception {
    new MvcModuleCompilerRunner(new C1814())
        .example(Expected1814.class)
        .module(app -> {
          MockRouter router = new MockRouter(app);
          MockContext ctx = new MockContext();
          ctx.setQueryString("?type=foo");
          assertEquals("[foo]", router.get("/1814", ctx).value().toString());
        });
  }

}
