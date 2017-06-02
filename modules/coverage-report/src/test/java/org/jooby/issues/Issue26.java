package org.jooby.issues;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;

import org.jooby.MediaType;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;

public class Issue26 extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  {
    renderer((object, ctx) -> {
      if (ctx.accepts(MediaType.html)) {
        Config config = (Config) ctx.locals().get("config");
        assertNotNull(config);
        ctx.send(object.toString());
        latch.countDown();
      }
    });

    get("*", (req, rsp) -> req.set("config", req.require(Config.class)));

    get("/", req -> "OK");
  }

  @Test
  public void shouldHaveAccessToLocalVarFromBodyWriteContext() throws Exception {
    request()
        .get("/")
        .expect(200)
        .expect("OK");

    latch.await();
  }

}
