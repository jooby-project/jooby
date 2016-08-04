package utow;

import org.jooby.Jooby;

public class UtowHttp2 extends Jooby {

  {

    http2();
    securePort(8443);

    get("/http2", () -> "OK");

  }

  public static void main(final String[] args) throws Throwable {
    run(UtowHttp2::new, args);
  }
}
