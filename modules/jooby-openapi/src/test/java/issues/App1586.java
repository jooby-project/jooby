package issues;

import examples.SubController;
import io.jooby.Jooby;

public class App1586 extends Jooby {
  {
    mvc(new SubController());
  }
}
