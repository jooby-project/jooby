package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class LocaleFeature extends ServerFeature {

  public static class Resource {

    @Inject
    public Resource(@Named("application.lang") final Locale namedLocale, final Locale locale) {
      assertEquals(locale, namedLocale);
    }

    @GET
    @Path("/")
    public String locale(final org.jooby.Request req) {
      return req.locale().toString();
    }
  }

  {
    use(ConfigFactory.empty().withValue("application.lang", ConfigValueFactory.fromAnyRef("es_ar")));
    use(Resource.class);
  }

  @Test
  public void locale() throws Exception {
    request()
        .get("/")
        .expect(200)
        .expect("es_AR");
  }

}
