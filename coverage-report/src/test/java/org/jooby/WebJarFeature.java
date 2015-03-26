package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class WebJarFeature extends ServerFeature {

  {
    assets("/webjars/**", "/META-INF/resources/webjars/{0}");

  }

  @Test
  public void jquery() throws Exception {
    request()
        .get("/webjars/jquery/2.1.3/jquery.js")
        .expect(200)
        .header("Content-Type", "application/javascript;charset=UTF-8")
        .header("Content-Length", 247387);

    request()
        .get("/webjars/jquery/2.1.3/jquery.min.js")
        .expect(200)
        .header("Content-Type", "application/javascript;charset=UTF-8")
        .header("Content-Length", 84355);

    request()
        .get("/webjars/jquery/2.1.3/jquery.min.map")
        .expect(200)
        .header("Content-Type", "text/plain;charset=UTF-8")
        .header("Content-Length", 127542);
  }

  @Test
  public void bootstrap() throws Exception {
    request()
        .get("/webjars/bootstrap/3.3.4/js/bootstrap.js")
        .expect(200)
        .header("Content-Type", "application/javascript;charset=UTF-8")
        .header("Content-Length", 67546);

    request()
        .get("/webjars/bootstrap/3.3.4/css/bootstrap.css")
        .expect(200)
        .header("Content-Type", "text/css;charset=UTF-8")
        .header("Content-Length", 141622);
  }
}
