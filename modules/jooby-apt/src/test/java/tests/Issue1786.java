package tests;

import io.jooby.MockContext;
import io.jooby.MockRouter;
import io.jooby.StatusCode;
import io.jooby.apt.MvcModuleCompilerRunner;
import io.jooby.exception.MissingValueException;
import org.junit.jupiter.api.Test;
import source.Controller1786;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Issue1786 {

  @Test
  public void shouldThrowMissingValueExceptionIfRequiredStringParamNotSpecified() throws Exception {
    new MvcModuleCompilerRunner(new Controller1786())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          assertThrows(MissingValueException.class, () -> router.get("/required-string-param"));
        });
  }

  @Test
  public void shouldReturnValueIfRequiredStringParamSpecified() throws Exception {
    new MvcModuleCompilerRunner(new Controller1786())
        .module(app -> {
          final String expectedValue = "non-null string";

          MockContext ctx = new MockContext();
          ctx.setQueryString("value=" + expectedValue);

          MockRouter router = new MockRouter(app);
          router.get("/required-string-param", ctx, rsp -> {
            assertEquals(StatusCode.OK, rsp.getStatusCode());
            assertEquals(expectedValue, rsp.value());
          });
        });
  }
}
