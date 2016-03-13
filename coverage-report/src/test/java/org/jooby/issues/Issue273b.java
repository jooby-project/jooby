package org.jooby.issues;

import java.util.Locale;
import java.util.Optional;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue273b extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.lang",
            ConfigValueFactory.fromAnyRef("fr-CA,fr-FR,en,en-CA,en-GB,en-US,de")));

    get("/locale/filter", req -> req.locales(Locale::filter));

    get("/locale/lookup", req -> Optional.ofNullable(req.locale(Locale::lookup))
            .map(Locale::toString).orElse(""));
  }

  @Test
  public void filter() throws Exception {
    request().get("/locale/filter")
        .expect("[]");
    request().get("/locale/lookup")
        .expect("");

    request().get("/locale/filter")
        .header("Accept-Language", "fr")
        .expect("[fr_CA, fr_FR, fr_FX]");
    request().get("/locale/lookup")
        .header("Accept-Language", "fr")
        .expect("");

    request().get("/locale/filter")
        .header("Accept-Language", "en")
        .expect("[en, en_CA, en_GB, en_US]");
    request().get("/locale/lookup")
        .header("Accept-Language", "en")
        .expect("en");

    request().get("/locale/filter")
        .header("Accept-Language", "de")
        .expect("[de]");
    request().get("/locale/lookup")
        .header("Accept-Language", "de")
        .expect("de");

    request().get("/locale/filter")
        .header("Accept-Language", "es")
        .expect("[]");
    request().get("/locale/lookup")
        .header("Accept-Language", "es")
        .expect("");

    request().get("/locale/filter")
        .header("Accept-Language", "fr-*")
        .expect("[fr_CA, fr_FR, fr_FX]");
    request().get("/locale/lookup")
        .header("Accept-Language", "fr-*")
        .expect("fr_CA");

    request().get("/locale/filter")
        .header("Accept-Language", "*-CA")
        .expect("[fr_CA, en_CA]");
    request().get("/locale/lookup")
        .header("Accept-Language", "*-CA")
        .expect("fr_CA");

    request().get("/locale/filter")
        .header("Accept-Language", "*-DE")
        .expect("[]");
    request().get("/locale/lookup")
        .header("Accept-Language", "*-DE")
        .expect("en");

    request().get("/locale/filter")
        .header("Accept-Language", "*-IT")
        .expect("[]");
    request().get("/locale/lookup")
        .header("Accept-Language", "*-IT")
        .expect("en");
  }

}
