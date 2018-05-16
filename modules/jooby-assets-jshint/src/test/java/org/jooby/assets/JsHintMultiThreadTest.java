package org.jooby.assets;

import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JsHintMultiThreadTest {

  private static V8EngineFactory engineFactory = new V8EngineFactory();

  @AfterClass
  public static void release() {
    engineFactory.release();
  }

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Test
  public void uglify() throws Exception {
    long s = System.currentTimeMillis();
    char start = 'a';
    char stop = 'z';
    int size = stop - start;
    ExecutorService executor = Executors.newFixedThreadPool(size + 1);
    CountDownLatch latch = new CountDownLatch(size);
    for (char i = 0; i <= size; i++) {
      executor.execute(run(latch, "var " + (char) (i + start) + "=1;"));
    }
    latch.await();
    long e = System.currentTimeMillis();
    log.info(" {} took {}ms each", size, ((e - s) / size));
  }

  private Runnable run(final CountDownLatch latch, final String statement) {
    return () -> {
      try {
        assertEquals(statement, new Jshint()
            .set(engineFactory)
            .process("/x.js", statement, ConfigFactory.empty()));
      } catch (Exception ex) {
        ex.printStackTrace();
        throw new IllegalStateException(ex);
      } finally {
        latch.countDown();
      }
    };
  }

}
