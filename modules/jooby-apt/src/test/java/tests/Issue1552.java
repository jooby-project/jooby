package tests;

import io.jooby.Context;
import io.jooby.MockRouter;
import io.jooby.apt.MvcModuleCompilerRunner;
import source.Controller1552;
import source.JavaBeanParam;

import org.junit.jupiter.api.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

public class Issue1552 {
  @Test
  public void inherited() throws Exception {
    new MvcModuleCompilerRunner(new Controller1552())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          assertEquals("/inherited", router.get("/inherited").value());
          assertEquals(Arrays.asList("/inherited/subpath"), router.get("/inherited/subpath").value());
          assertTrue(router.get("/inherited/object").value() instanceof Context);
          assertTrue(router.post("/inherited/post").value() instanceof JavaBeanParam);

          assertEquals("/inherited/pathAttributeWork", router.get("/inherited/pathAttributeWork").value());
          assertEquals("/inherited/path", router.get("/inherited/path").value());
          assertEquals("/inherited/value", router.get("/inherited/value").value());

          assertEquals("/inherited/path1", router.get("/inherited/path1").value());
          assertEquals("/inherited/path2", router.get("/inherited/path2").value());

          assertEquals("/inherited/path1", router.post("/inherited/path1").value());
          assertEquals("/inherited/path2", router.post("/inherited/path2").value());
        });
  }
}
