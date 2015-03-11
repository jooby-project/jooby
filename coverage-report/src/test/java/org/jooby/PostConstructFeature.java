package org.jooby;

import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostConstructFeature extends ServerFeature {

  /** The logging system. */
  private static final Logger log = LoggerFactory.getLogger(PostConstructFeature.class);

  private static CountDownLatch counter;

  public static class ManagedObject {

    @PostConstruct
    public void start() throws Exception {
      log.info("starting: {}", getClass().getName());
      counter.countDown();
    }

  }

  @Singleton
  public static class SingletonObject {

    @PostConstruct
    public void start() throws Exception {
      log.info("starting: {}", this);
      counter.countDown();
    }

  }

  public static class ManagedExObject {

    @PostConstruct
    public void start() throws Exception {
      throw new Exception("intentional err");
    }

  }

  {

    get("/proto", req -> {
      int n = req.param("n").intValue();
      for (int i = 0; i < n; i++) {
        req.require(ManagedObject.class);
      }
      return "OK";
    });

    get("/singleton", req -> {
      int n = req.param("n").intValue();
      for (int i = 0; i < n; i++) {
        req.require(SingletonObject.class);
      }
      return "OK";
    });

    get("/protoex", req ->
        req.require(ManagedExObject.class));
  }

  @Test
  public void startOneProto() throws Exception {
    counter = new CountDownLatch(1);
    request()
        .get("/proto?n=" + counter.getCount())
        .expect("OK");
    counter.await();
  }

  @Test
  public void startNSingleton() throws Exception {
    counter = new CountDownLatch(1);
    request()
        .get("/singleton?n=7")
        .expect("OK");
    counter.await();
  }

  @Test
  public void startTwoProto() throws Exception {
    counter = new CountDownLatch(2);
    request()
        .get("/proto?n=" + counter.getCount())
        .expect("OK");
    counter.await();
  }

  @Test
  public void startNProto() throws Exception {
    counter = new CountDownLatch(5);
    request()
        .get("/proto?n=" + counter.getCount())
        .expect("OK");
    counter.await();
  }

  @Test
  public void startWithCheckedException() throws Exception {
    request()
        .get("/protoex")
        .expect(500);
  }
}
