package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class WebJarFeature extends ServerFeature {

  {
    assets("/webjars/**", "/META-INF/resources/webjars/{0}");
    assets("/css/**", "/assets/{0}");
  }

  @Test
  public void jquery() throws Exception {
    request()
        .get("/webjars/jquery/2.2.4/jquery.js")
        .expect(200)
        .header("Content-Type", "application/javascript;charset=UTF-8")
        .header("Content-Length", 257551);

    request()
        .get("/webjars/jquery/2.2.4/jquery.min.js")
        .expect(200)
        .header("Content-Type", "application/javascript;charset=UTF-8")
        .header("Content-Length", 85613);

    request()
        .get("/webjars/jquery/2.2.4/jquery.min.map")
        .expect(200)
        .header("Content-Type", "text/plain;charset=UTF-8")
        .header("Content-Length", 129572);
  }

  @Test
  public void jqueryui() throws Exception {
    request()
        .get("/css/jquery-ui.css")
        .expect(200)
        .header("Content-Type", "text/css;charset=UTF-8")
        .header("Content-Length", 25272);
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
