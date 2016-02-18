package org.jooby.issues;

import java.util.Locale;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue273 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.lang", ConfigValueFactory.fromAnyRef("en-us")));

    get("/273", req -> {
      Locale selected = req.locale();
      Locale noMatch = req.locale(Locale.forLanguageTag("de-at"));
      Locale match = req.locale(Locale.forLanguageTag("fr"));
      return selected + ";" + noMatch + ";" + match;
    });
  }

  @Test
  public void shouldHanleComplexLocaleExpressions() throws Exception {
    request().get("/273")
        .header("Accept-Language", "de-DE,de;q=0.8,fr-CA;q=0.7,fr;q=0.5,en-CA;q=0.3,en;q=0.2")
        .expect("de_DE;en_US;fr");
  }

}
