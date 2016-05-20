package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue374 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.lang", ConfigValueFactory.fromAnyRef("en-us, es-ar")));

    get("/374", req -> req.locale().toString().toLowerCase());
  }

  @Test
  public void shouldRestoreIllFormed() throws Exception {
    request()
        .get("/374")
        .header("Accept-Language", "es-ar;")
        .expect("es_ar");
  }

  @Test
  public void dontFailOnBadAcceptLanguage() throws Exception {

    request()
        .get("/374")
        .header("Accept-Language", "xx^x")
        .expect("en_us");
  }

}
