package org.jooby.integration;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class DateTimeFormatterFeature extends ServerFeature {

  public static class Resource {

    private DateTimeFormatter formatter;

    private String dateFormat;

    private ZoneId zoneId;

    @Inject
    public Resource(final DateTimeFormatter formatter, final ZoneId zoneId,
        @Named("application.dateFormat") final String dateFormat) {
      assertEquals(ZoneId.of("GMT"), zoneId);
      assertEquals(zoneId, formatter.getZone());
      this.formatter = requireNonNull(formatter, "def formatter is required.");
      this.zoneId = zoneId;
      this.dateFormat = requireNonNull(dateFormat, "The dateFormat is required.");
    }

    @GET
    @Path("/")
    public String formatter(final long time, final org.jooby.Request req) {
      Date date = new Date(time);
      SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, req.locale());
      sdf.setTimeZone(TimeZone.getTimeZone(zoneId));
      String sdate = sdf.format(date);
      String newsdate = formatter.format(formatter.parse(sdate));
      return sdate + "|" + newsdate;
    }
  }

  {
    use(ConfigFactory
        .empty()
        .withValue("application.lang", ConfigValueFactory.fromAnyRef("en_US"))
        .withValue("application.dateFormat", ConfigValueFactory.fromAnyRef("MM/dd/yy H:mm"))
        .withValue("application.tz", ConfigValueFactory.fromAnyRef("GMT")));
    use(Resource.class);
  }

  @Test
  public void dateFormat() throws Exception {
    long time = 1412824189989l;
    request()
      .get("/?time=" + time)
      .expect(200)
      .expect("10/09/14 3:09|10/09/14 3:09");
  }

}
