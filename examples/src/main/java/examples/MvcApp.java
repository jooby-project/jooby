package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;

public class MvcApp extends Jooby {

  {
    mvc(new PlainText());
  }

  public static void main(String[] args) {
    runApp(args, ExecutionMode.EVENT_LOOP, MvcApp::new);
  }
}
