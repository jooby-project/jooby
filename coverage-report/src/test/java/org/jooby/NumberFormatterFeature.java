package org.jooby;

import static java.util.Objects.requireNonNull;

import java.text.DecimalFormat;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
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
    request()
        .get("/")
        .expect("#,##0.###|#,##0.###")
        .expect(200);
  }

}
