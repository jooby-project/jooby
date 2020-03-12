package tests;

import io.jooby.MockRouter;
import io.jooby.StatusCode;
import io.jooby.apt.MvcModuleCompilerRunner;
import org.junit.jupiter.api.Test;
import source.Controller1545;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1545 {
  @Test
  public void shouldSetNoContentCodeForVoidRoute() throws Exception {
    new MvcModuleCompilerRunner(new Controller1545())
        .module(app -> {
          MockRouter router = new MockRouter(app);

          router.delete("/1545", rsp -> {
            assertEquals(StatusCode.NO_CONTENT, rsp.getStatusCode());
          });

          router.delete("/1545/success", rsp -> {
            assertEquals(StatusCode.OK, rsp.getStatusCode());
          });

          router.post("/1545", rsp -> {
            assertEquals(StatusCode.CREATED, rsp.getStatusCode());
          });

          router.post("/1545/novoid", rsp -> {
            assertEquals(StatusCode.CREATED, rsp.getStatusCode());
            assertEquals("OK", rsp.value());
          });
        })
    ;
  }
}
