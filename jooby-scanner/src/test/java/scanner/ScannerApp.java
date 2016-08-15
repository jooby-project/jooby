package scanner;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import org.jooby.Jooby;
import org.jooby.json.Jackson;
import org.jooby.scanner.Scanner;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;

public class ScannerApp extends Jooby {

  {
    use(new Jackson());

    use(new Scanner()
        .scan(Service.class)
        .scan(IBar.class)
        .scan(Named.class)
        .scan(AbsFoo.class)
        );

    get("/guava", req -> {
      ServiceManager sm = req.require(ServiceManager.class);
      return sm.servicesByState();
    });

    get("/bar", req -> {
      Bar sm = req.require(Bar.class);
      return sm.bar();
    });

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    get("/stop", () -> {
      executor.schedule(() -> {
        this.stop();
        System.exit(0);
      }, 500, TimeUnit.MILLISECONDS);
      return "Stopping";
    });
  }

  public static void main(final String[] args) throws Throwable {
    run(ScannerApp::new, args);
  }
}
