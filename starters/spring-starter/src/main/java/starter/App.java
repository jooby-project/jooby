package starter;

import io.jooby.Jooby;
import io.jooby.di.SpringModule;
import io.jooby.json.JacksonModule;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App extends Jooby {

  {
    install(new JacksonModule());

    install(new SpringModule());

    /** Mvc routes are discovered by Spring. */
  }

  public static void main(String[] args) {
    runApp(args, App::new);
  }
}
