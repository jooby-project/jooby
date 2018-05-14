package org.jooby.issues;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue877 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.lang",
          ConfigValueFactory.fromAnyRef("en, de")));

    get("/877", req -> req.locale().toLanguageTag());
  }

  @Test
  public void lookupCommonLanguageInsteadOfDefault() throws Exception {
    request().get("/877")
        .header("Accept-Language", "de-DE")
        .expect("de");
  }

  @Test
  public void fallbackToDefaultInCaseNothingMatches() throws Exception {
    request().get("/877")
        .header("Accept-Language", "fr-CA")
        .expect("en");
  }
}
