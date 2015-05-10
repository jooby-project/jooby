package org.jooby;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import org.jooby.mvc.Body;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class BuiltinBodyParserFeature extends ServerFeature {

  public enum VOWELS {
    A,
    B;
  }

  @Path("/r")
  public static class Resource {

    @POST
    @Path("/optional/int")
    public Object intBody(final @Body Optional<Integer> p) {
      return p.toString();
    }

    @POST
    @Path("/locale")
    public Object localeBody(final @Body Locale l) {
      return l;
    }

    @POST
    @Path("/enum")
    public Object enumBody(final @Body VOWELS v) {
      return v;
    }

    @POST
    @Path("/date")
    public Object dateBody(final @Body Date d) {
      return new SimpleDateFormat("dd-MM-yy").format(d);
    }

    @POST
    @Path("/ldate")
    public Object dateBody(final @Body LocalDate d) {
      return d.format(DateTimeFormatter.ofPattern("dd-MM-yy"));
    }

  }

  {

    use(Resource.class);

    post("/optional/int", req -> {
      return req.body().toOptional(int.class);
    });

    post("/locale", req -> {
      return req.body().to(Locale.class);
    });

    post("/enum", req -> {
      return req.body().to(VOWELS.class);
    });

    post("/date", req -> {
      return new SimpleDateFormat("dd-MM-yy").format(req.body().to(Date.class));
    });

    post("/ldate", req -> {
      return req.body().to(LocalDate.class).format(DateTimeFormatter.ofPattern("dd-MM-yy"));
    });
  }

  @Test
  public void optionalBody() throws Exception {
    request()
        .post("/optional/int")
        .body("7", "text/palin")
        .expect("Optional[7]");

    request()
        .post("/optional/int")
        .expect("Optional.empty");

    request()
        .post("/r/optional/int")
        .body("7", "text/palin")
        .expect("Optional[7]");

    request()
        .post("/r/optional/int")
        .expect("Optional.empty");
  }

  @Test
  public void localeBody() throws Exception {
    request()
        .post("/locale")
        .body("es_AR", "text/palin")
        .expect("es_AR");

    request()
        .post("/r/locale")
        .body("es_AR", "text/palin")
        .expect("es_AR");

  }

  @Test
  public void enumBody() throws Exception {
    request()
        .post("/enum")
        .body("A", "text/palin")
        .expect("A");

    request()
        .post("/r/enum")
        .body("A", "text/palin")
        .expect("A");

  }

  @Test
  public void dateBody() throws Exception {
    request()
        .post("/date")
        .body("10-05-15", "text/palin")
        .expect("10-05-15");

    request()
        .post("/r/date")
        .body("10-05-15", "text/palin")
        .expect("10-05-15");
  }

  @Test
  public void localDateBody() throws Exception {
    request()
        .post("/ldate")
        .body("10-05-15", "text/palin")
        .expect("10-05-15");

    request()
        .post("/r/ldate")
        .body("10-05-15", "text/palin")
        .expect("10-05-15");
  }

}
