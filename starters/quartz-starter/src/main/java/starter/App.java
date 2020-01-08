package starter;

import io.jooby.Jooby;
import io.jooby.flyway.FlywayModule;
import io.jooby.hikari.HikariModule;
import io.jooby.json.JacksonModule;
import io.jooby.quartz.QuartzApp;
import io.jooby.quartz.QuartzModule;
import org.quartz.Scheduler;

public class App extends Jooby {
  {
    /** Uncomment Hikari to use JDBC Job Store: */
     // install(new HikariModule());

    /** Uncomment Flyway to use JDBC Job Store: */
     // install(new FlywayModule());

    install(new JacksonModule());

    install(new QuartzModule(SampleJob.class, BeanJob.class));

    use("/quartz", new QuartzApp());
  }

  public static void main(String[] args) {
    runApp(args, App::new);
  }
}
