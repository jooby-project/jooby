package org.jooby.issues;

import static org.junit.Assert.assertNotNull;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.jooby.BodyFormatter;
import org.jooby.MediaType;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.typesafe.config.Config;

public class Issue26 extends ServerFeature {

  private static final CountDownLatch latch = new CountDownLatch(1);

  {
    use(new BodyFormatter() {

      @Override
      public List<MediaType> types() {
        return ImmutableList.of(MediaType.html);
      }

      @Override
      public void format(final Object body, final BodyFormatter.Context writer) throws Exception {
        Config config = (Config) writer.locals().get("config");
        assertNotNull(config);
        writer.text(out -> CharStreams.copy(new StringReader(body.toString()), out));
        latch.countDown();
      }

      @Override
      public boolean canFormat(final Class<?> type) {
        return true;
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
