package org.jooby;

import javax.inject.Singleton;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartManagedObjectFeature extends ServerFeature {

  /** The logging system. */
  private static final Logger log = LoggerFactory.getLogger(StartManagedObjectFeature.class);

  @Singleton
  public static class SingletonObject implements Managed {

    static int started;

    @Override
    public void start() throws Exception {
      log.info("starting: {}", this);
      started += 1;
    }

    @Override
    public void stop() throws Exception {
    }

  }

  {

    managed(new SingletonObject());

    get("/singleton", req -> {
      return SingletonObject.started;
    });
  }

  static int c = 1;

  @Test
  public void startSingleton() throws Exception {
    request()
        .get("/singleton")
        .expect("" + c);
    c += 1;
  }

}
