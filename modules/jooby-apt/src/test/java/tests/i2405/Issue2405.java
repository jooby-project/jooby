package tests.i2405;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.MockContext;
import io.jooby.MockRouter;
import io.jooby.apt.MvcModuleCompilerRunner;

public class Issue2405 {
  @Test
  public void shouldGenerateUniqueNames() throws Exception {
    new MvcModuleCompilerRunner(new C2405())
        .module(app -> {
          app.converter(new Converter2405());
          MockRouter router = new MockRouter(app);
          assertEquals("foo", router.get("/2405/blah", new MockContext()
              .setQueryString("?blah=foo")).value().toString());

          assertEquals("bar", router.get("/2405/blah2", new MockContext()
              .setQueryString("?blah=bar")).value().toString());
        });
  }

}
