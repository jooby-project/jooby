package org.jooby;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import java.text.DecimalFormat;

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

public class NumberFormatterFeature extends ServerFeature {

  public static class Resource {

    private DecimalFormat formatter;

    private String dateFormat;

    @Inject
    public Resource(final DecimalFormat formatter,
        @Named("application.numberFormat") final String numberFormat) {
      this.formatter = requireNonNull(formatter, "def formatter is required.");
      this.dateFormat = requireNonNull(numberFormat, "The dateFormat is required.");
    }

    @GET
    @Path("/")
    public String formatter() {
      return dateFormat + "|" + formatter.toPattern();
    }
  }

  {
    use(ConfigFactory.empty().withValue("application.lang",
        ConfigValueFactory.fromAnyRef("en_US")));

    use(Resource.class);
  }

  @Test
  public void numberFormat() throws Exception {
    assertEquals("#,##0.###|#,##0.###",
        execute(GET(uri("/")), (response) -> {
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
