package org.jooby.integration;

import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;

import javax.inject.Named;

import org.jooby.Cookie;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestParamFeature extends ServerFeature {

  public enum VOWELS {
    A,
    B;
  }

  public static class Resource {

    @GET
    @Path("/boolean")
    public Object booleanParam(final boolean p1, final Boolean p2) {
      return p1 + "/" + p2;
    }

    @GET
    @Path("/booleanList")
    public Object booleanParam(final List<Boolean> p) {
      return p.toString();
    }

    @GET
    @Path("/booleanOptional")
    public Object booleanParam(final Optional<Boolean> p) {
      return p.toString();
    }

    @GET
    @Path("/byte")
    public Object byteParam(final byte p1, final Byte p2) {
      return p1 + p2;
    }

    @GET
    @Path("/short")
    public Object shortParam(final short p1, final Short p2) {
      return p1 + p2;
    }

    @GET
    @Path("/int")
    public Object intParam(final int p1, final Integer p2) {
      return p1 + p2;
    }

    @GET
    @Path("/optionalInt")
    public Object intParam(final Optional<Integer> p) {
      return p.toString();
    }

    @GET
    @Path("/long")
    public Object longParam(final long p1, final long p2) {
      return p1 + p2;
    }

    @GET
    @Path("/float")
    public Object floatParam(final float p1, final float p2) {
      return p1 + p2;
    }

    @GET
    @Path("/double")
    public Object doubleParam(final double p1, final double p2) {
      return p1 + p2;
    }

    @GET
    @Path("/enum")
    public Object enumParam(final VOWELS p1, final VOWELS p2) {
      return p1.name() + ", " + p2.name();
    }

    @GET
    @Path("/sortedEnum")
    public Object sortedEnumParam(final SortedSet<VOWELS> p) {
      return p.toString();
    }

    @GET
    @Path("/optionalEnum")
    public Object optionalEnumParam(final Optional<VOWELS> p) {
      return p.toString();
    }

    @GET
    @Path("/uuid")
    public Object uuid(final UUID p) {
      return p.toString();
    }

    @GET
    @Path("/decimal")
    public Object bigDecimal(final BigDecimal p) {
      return p.toString();
    }

    @GET
    @Path("/named-param")
    public Object namedParam(@Named("not-java-name") final String p) {
      return p.toString();
    }

    @GET
    @Path("/cookie")
    public Object cookieParam(final Cookie galleta) {
      return galleta.toString();
    }

    @GET
    @Path("/ocookie")
    public Object ocookie(final Optional<Cookie> galleta) {
      return galleta.toString();
    }

    @GET
    @Path("/optionalCookie")
    public Object cookieParam(final Optional<Cookie> galleta) {
      return galleta.toString();
    }

    @GET
    @Path("/req")
    public Object reqparam(final org.jooby.Request req) {
      assertNotNull(req);
      return req.path();
    }

    @GET
    @Path("/rsp")
    public Object rspparam(final org.jooby.Response rsp) {
      assertNotNull(rsp);
      return "/rsp";
    }

  }

  {

    use(Resource.class);
  }

  @Test
  public void reqParam() throws Exception {
    request()
        .get("/req")
        .expect("/req");
  }

  @Test
  public void rspParam() throws Exception {
    request()
        .get("/rsp")
        .expect("/rsp");
  }

  @Test
  public void booleanParam() throws Exception {
    request()
        .get("/boolean?p1=true&p2=false")
        .expect("true/false");

  }

  @Test
  public void namedParam() throws Exception {
    request()
        .get("/named-param?not-java-name=awesome")
        .expect("awesome");
  }

  @Test
  public void missingParam() throws Exception {
    request()
        .get("/boolean")
        .expect(400);
  }

  @Test
  public void optionalBooleanParam() throws Exception {
    request()
        .get("/booleanOptional")
        .expect("Optional.empty");

    request()
        .get("/booleanOptional?p=true")
        .expect("Optional[true]");

  }

  @Test
  public void booleanListParam() throws Exception {
    request()
        .get("/booleanList?p=true&p=false")
        .expect("[true, false]");
  }

  @Test
  public void byteParam() throws Exception {
    request()
        .get("/byte?p1=23&p2=4")
        .expect("27");
  }

  @Test
  public void shortParam() throws Exception {
    request()
        .get("/short?p1=1&p2=2")
        .expect("3");
  }

  @Test
  public void intParam() throws Exception {
    request()
        .get("/int?p1=40&p2=303")
        .expect("343");
  }

  @Test
  public void optionalIntParam() throws Exception {
    request()
        .get("/optionalInt")
        .expect("Optional.empty");

    request()
        .get("/optionalInt?p=40")
        .expect("Optional[40]");
  }

  @Test
  public void longParam() throws Exception {
    request()
        .get("/long?p1=40&p2=303")
        .expect("343");
  }

  @Test
  public void floatParam() throws Exception {
    request()
        .get("/float?p1=40&p2=303.5")
        .expect("343.5");
  }

  @Test
  public void doubleParam() throws Exception {
    request()
        .get("/double?p1=40&p2=303.5")
        .expect("343.5");
  }

  @Test
  public void enumParam() throws Exception {
    request()
        .get("/enum?p1=A&p2=B")
        .expect("A, B");
  }

  @Test
  public void enumSortedParam() throws Exception {
    request()
        .get("/sortedEnum?p=B&p=A")
        .expect("[A, B]");
  }

  @Test
  public void optionalEnumParam() throws Exception {
    request()
        .get("/optionalEnum")
        .expect("Optional.empty");

    request()
        .get("/optionalEnum?p=B")
        .expect("Optional[B]");
  }

  @Test
  public void uuidParam() throws Exception {
    String uuid = UUID.randomUUID().toString();
    request()
        .get("/uuid?p=" + uuid)
        .expect(uuid);
  }

  @Test
  public void decimalParam() throws Exception {
    BigDecimal decimal = new BigDecimal(Math.PI + "");
    request()
        .get("/decimal?p=" + decimal.toString())
        .expect(decimal.toString());
  }

  @Test
  public void cookieParam() throws Exception {
    request()
        .get("/cookie")
        .header("Cookie", "galleta=galleta")
        .expect(
            "{name=galleta, value=Optional[galleta], domain=Optional.empty, path=/, maxAge=-1, secure=false}");

    request()
        .get("/ocookie")
        .header("Cookie", "galleta=galleta")
        .expect(
            "Optional[{name=galleta, value=Optional[galleta], domain=Optional.empty, path=/, maxAge=-1, secure=false}]");

  }

  @Test
  public void optionalCookie() throws Exception {
    request()
        .get("/optionalCookie")
        .expect("Optional.empty");
  }

}
