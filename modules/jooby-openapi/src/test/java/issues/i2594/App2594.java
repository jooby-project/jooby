package issues.i2594;

import io.jooby.Jooby;
import io.jooby.OpenAPIModule;

public class App2594 extends Jooby {
  {

    install(new OpenAPIModule());

    mvc(HealthController2594.class);

    mount("/api/v1", new ControllersAppV12594());
    mount("/api/v2", new ControllersAppV22594());
  }
}
