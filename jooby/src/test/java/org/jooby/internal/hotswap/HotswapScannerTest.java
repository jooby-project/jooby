package org.jooby.internal.hotswap;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.google.common.io.Files;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HotswapScannerTest {

  @Test
  public void newfile() throws IOException, InterruptedException {
    String file = UUID.randomUUID().toString() + ".uuid";

    Config config = ConfigFactory.empty().withValue("application.builddir",
        ConfigValueFactory.fromAnyRef("target"));

    CountDownLatch latch = new CountDownLatch(1);
    HotswapScanner scanner = new HotswapScanner(resource -> {
      assertEquals(file, resource);
      latch.countDown();
    }, config);

    scanner.start();

    // new file
    Files.touch(new File("target", file));

    latch.await();

    scanner.stop();
  }

  @Test
  public void newdir() throws IOException, InterruptedException {
    String file = UUID.randomUUID().toString();

    Config config = ConfigFactory.empty().withValue("application.builddir",
        ConfigValueFactory.fromAnyRef("target"));

    CountDownLatch latch = new CountDownLatch(1);
    HotswapScanner scanner = new HotswapScanner(resource -> {
      assertEquals(file + File.separator + file + ".uuid", resource);
      latch.countDown();
    }, config);

    scanner.start();

    // new file
    new File("target", file).mkdir();
    Thread.sleep(5000L);
    Files.touch(new File(new File("target", file), file + ".uuid"));
    latch.await();

    scanner.stop();
  }
}
