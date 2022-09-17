package starter.mod;

import io.jooby.Jooby;

public class App extends Jooby {
  {
      get("/", ctx -> {
    	  return "hello";
      });
      mvc(ModController.class, () -> new ModController());
  }

  public static void main(String[] args) {
    runApp(args, App::new);
  }
}
