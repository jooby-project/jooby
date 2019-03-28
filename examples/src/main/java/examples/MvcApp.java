package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;

public class MvcApp extends Jooby {

  {
    mvc(new PlainText());
  }

  public static void main(String[] args) {
    runApp(ExecutionMode.EVENT_LOOP, args, MvcApp::new);
  }
}
