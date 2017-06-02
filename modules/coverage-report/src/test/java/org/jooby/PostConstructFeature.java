package org.jooby;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostConstructFeature extends ServerFeature {

  /** The logging system. */
  private static final Logger log = LoggerFactory.getLogger(PostConstructFeature.class);

  @Singleton
  public static class SingletonObject {
    static int started;

    @PostConstruct
    public void start() throws Exception {
      log.info("starting: {}", this);
      started += 1;
    }

  }

  {

    lifeCycle(SingletonObject.class);

    get("/singleton", req -> {
      return SingletonObject.started;
    });
  }

  static int value = 1;

  @Test
  public void shouldCallPostCConstructMethod() throws Exception {
    request()
        .get("/singleton")
        .expect(value + "");
    value += 1;
  }

}
