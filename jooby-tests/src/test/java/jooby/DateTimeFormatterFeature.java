package jooby;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import jooby.FilterFeature.HttpResponseValidator;
import jooby.mvc.GET;
import jooby.mvc.Path;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class DateTimeFormatterFeature extends ServerFeature {

  public static class Resource {

    private DateTimeFormatter formatter;

    private String dateFormat;

    @Inject
    public Resource(final DateTimeFormatter formatter,
        @Named("application.dateFormat") final String dateFormat) {
      this.formatter = requireNonNull(formatter, "def formatter is required.");
      this.dateFormat = requireNonNull(dateFormat, "The dateFormat is required.");
    }

    @GET
    @Path("/")
    public String formatter(final long time, final jooby.Request req) {
      Date date = new Date(time);
      String sdate = new SimpleDateFormat(dateFormat, req.locale()).format(date);
      String newsdate = formatter.format(formatter.parse(sdate));
      return sdate + "|" + newsdate;
    }
  }

  {
    use(ConfigFactory.empty().withValue("application.lang",
        ConfigValueFactory.fromAnyRef("en_US")));
    use(Resource.class);
  }

  @Test
  public void dateFormat() throws Exception {
    long time = 1412824189989l;
    assertEquals("10/9/14 12:09 AM|10/9/14 12:09 AM",
        execute(GET(uri("/?time=" + time)), (response) -> {
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
