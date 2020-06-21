package tests.i1806;

import io.jooby.MockRouter;
import io.jooby.apt.MvcModuleCompilerRunner;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1806 {

  @Test
  public void shouldNotGetListWithNullValue() throws Exception {
    new MvcModuleCompilerRunner(new C1806())
        .debugModule(app -> {
          MockRouter router = new MockRouter(app);
          assertEquals(Collections.emptyList(), router.get("/1806/c").value());
        });
  }

}
