package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.FilterFeature.HttpResponseValidator;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
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
    assertEquals("es_AR", execute(GET(uri("/")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));

    assertEquals("en_us", execute(GET(uri("/")).addHeader("Accept-Language", "en_us"), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static Object execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }
}
