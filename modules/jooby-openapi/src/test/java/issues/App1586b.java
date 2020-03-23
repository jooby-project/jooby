package issues;

import examples.EmptySubClassController;
import io.jooby.Jooby;

public class App1586b extends Jooby {
  {
    mvc(new EmptySubClassController());
  }
}
