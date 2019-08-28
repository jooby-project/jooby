package output;

import io.jooby.Jooby;

public class MvcDispatch implements Runnable {
  private Jooby application;

  public MvcDispatch(Jooby application) {
    this.application = application;
  }

  @Override public void run() {
    application.get("/", ctx -> "xx");
  }
}
