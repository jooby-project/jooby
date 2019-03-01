package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;

public class MvcApp extends Jooby {

  {
    use(new PlainText());
  }

  public static void main(String[] args) {
    run(MvcApp::new, ExecutionMode.EVENT_LOOP, args);
  }
}
