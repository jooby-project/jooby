package tests;

import io.jooby.Context;
import io.jooby.MockRouter;
import io.jooby.apt.MvcModuleCompilerRunner;
import source.Controller1552;
import source.Controller1552Empty;
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

          assertEquals("/inherited/postOnly", router.post("/inherited/postOnly").value());

          assertEquals("/inherited/pathAttributeWork", router.get("/inherited/pathAttributeWork").value());
          assertEquals("/inherited/path", router.get("/inherited/path").value());
          assertEquals("/inherited/value", router.get("/inherited/value").value());

          assertEquals("/inherited/path1", router.get("/inherited/path1").value());
          assertEquals("/inherited/path2", router.get("/inherited/path2").value());

          assertEquals("/inherited/childOnly", router.get("/inherited/childOnly").value());
          assertEquals("/inherited/childOnly", router.post("/inherited/childOnly").value());

          assertEquals("/inherited/path1", router.post("/inherited/path1").value());
          assertEquals("/inherited/path2", router.post("/inherited/path2").value());

        });
  }

  @Test
  public void inherited_empty() throws Exception {
    new MvcModuleCompilerRunner(new Controller1552Empty())
        .module(app -> {
          MockRouter router = new MockRouter(app);

          assertEquals("/inherit_empty", router.get("/inherit_empty").value());
          assertEquals(Arrays.asList("/inherit_empty/subpath"), router.get("/inherit_empty/subpath").value());
          assertTrue(router.get("/inherit_empty/object").value() instanceof Context);

          assertTrue(router.post("/inherit_empty/post").value() instanceof JavaBeanParam);

          assertEquals("/inherit_empty/postOnly", router.post("/inherit_empty/postOnly").value());

          assertEquals("/inherit_empty/pathAttributeWork", router.get("/inherit_empty/pathAttributeWork").value());
          assertEquals("/inherit_empty/path", router.get("/inherit_empty/path").value());
          assertEquals("/inherit_empty/value", router.get("/inherit_empty/value").value());

          assertEquals("/inherit_empty/path1", router.get("/inherit_empty/path1").value());
          assertEquals("/inherit_empty/path2", router.get("/inherit_empty/path2").value());

          assertEquals("/inherit_empty/path1", router.post("/inherit_empty/path1").value());
          assertEquals("/inherit_empty/path2", router.post("/inherit_empty/path2").value());

        });
  }
}
