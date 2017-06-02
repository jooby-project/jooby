package org.jooby.quartz;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.quartz.DisallowConcurrentExecution;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class QuartzFeature extends ServerFeature {

  private static volatile CountDownLatch latch = new CountDownLatch(1);

  @DisallowConcurrentExecution
  public static class QJob {

    @Scheduled("3s;delay=0")
    public void run() {
      latch.countDown();
    }
  }

  {
    use(ConfigFactory.empty()
        .withValue("org.quartz.scheduler.instanceName",
            ConfigValueFactory.fromAnyRef(UUID.randomUUID().toString())));
    use(new Quartz(QJob.class));

    get("/boost", () -> "done");
  }

  @Test
  public void runJob() throws Exception {
    latch = new CountDownLatch(1);
    request()
        .get("/boost")
        .expect("done");
    latch.await();
  }
}
