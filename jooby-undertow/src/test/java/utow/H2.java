package utow;

import org.jooby.Jooby;

public class H2 extends Jooby {

  {

    http2();
    securePort(8443);

    assets("/assets/**");
    assets("/", "index.html");

  }

  public static void main(final String[] args) throws Throwable {
    run(H2::new, args);
  }
}
