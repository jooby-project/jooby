package org.jooby.jdbi;

import java.util.concurrent.CountDownLatch;

import org.jooby.jdbc.Jdbc;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class JdbiConfigureCallbackFeature extends ServerFeature {

  {

    CountDownLatch latch = new CountDownLatch(1);
    use(ConfigFactory.empty().withValue("db", ConfigValueFactory.fromAnyRef("mem")));

    use(new Jdbc());
    use(new Jdbi().doWith((dbi, config) -> {
      latch.countDown();
    }));

    get("/jdbi-handle", req -> {
      latch.await();
      return latch.getCount();
    });
  }

  @Test
  public void doWithCallback() throws Exception {
    request()
        .get("/jdbi-handle")
        .expect("0");
  }
}
