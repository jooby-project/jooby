package issues;

import examples.OverrideMethodSubClassController;
import io.jooby.Jooby;

public class App1586c extends Jooby {
  {
    mvc(new OverrideMethodSubClassController());
  }
}
