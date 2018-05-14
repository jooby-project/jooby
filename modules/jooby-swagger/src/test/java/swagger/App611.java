package swagger;

import org.jooby.Jooby;
import org.jooby.swagger.SwaggerUI;

public class App611 extends Jooby {

  {
    use(Controller611.class);

    new SwaggerUI()
        .filter(r -> true)
        .install(this);
  }

  public static void main(final String[] args) {
    run(App611::new, args);
  }
}
