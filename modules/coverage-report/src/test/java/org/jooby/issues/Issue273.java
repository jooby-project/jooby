package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue273 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.lang",
          ConfigValueFactory.fromAnyRef("fr-CA,fr-FR,en,en-CA,en-GB,en-US,de")));

    get("/273", req -> req.locale().toLanguageTag());
  }

  @Test
  public void noNegotiation() throws Exception {
    request().get("/273")
        .expect("fr-CA");
  }

  @Test
  public void exactMatch() throws Exception {
    request().get("/273")
        .header("Accept-Language", "de")
        .expect("de");

    request().get("/273")
        .header("Accept-Language", "en-GB")
        .expect("en-GB");
  }

  @Test
  public void inexactMatch() throws Exception {
    request().get("/273")
        .header("Accept-Language", "fr")
        .expect("fr-CA");
  }

  @Test
  public void noMatch() throws Exception {
    request().get("/273")
        .header("Accept-Language", "es")
        .expect("fr-CA");

    request().get("/273")
        .header("Accept-Language", "en-AU")
        .expect("en");
  }

  @Test
  public void shouldHanleComplexLocaleExpressions() throws Exception {
    request().get("/273")
        .header("Accept-Language", "de-DE,de;q=0.8,fr-CA;q=0.7,fr;q=0.5,en-CA;q=0.3,en;q=0.2")
        .expect("de");
  }

}
