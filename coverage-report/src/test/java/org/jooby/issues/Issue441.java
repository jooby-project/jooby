package org.jooby.issues;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;

public class Issue441 extends ServerFeature {

  {

    port(9000);

    charset(StandardCharsets.US_ASCII);
    dateFormat("YYYY-MM-dd");
    numberFormat("000000.000");
    lang("es");
    timezone(ZoneId.of("Europe/Paris"));

    get("/441", req -> {
      Config conf = req.require(Config.class);
      return req.charset() + ";" + req.locale() + ";" + conf.getString("application.dateFormat")
          + ";" + conf.getString("application.numberFormat") + ";"
          + conf.getString("application.tz") + ":" + req.port();
    });
  }

  @Test
  public void harcodeCodeOptions() throws URISyntaxException, Exception {
    request()
        .get("/441")
        .expect("US-ASCII;es;YYYY-MM-dd;000000.000;Europe/Paris:9000");
  }
}
