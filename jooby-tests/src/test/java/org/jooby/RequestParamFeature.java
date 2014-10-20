package org.jooby;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;

import javax.inject.Named;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.jooby.Response;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
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
  }

  {

    use(Resource.class);
  }

  @Test
  public void booleanParam() throws Exception {
    assertEquals("true/false",
        GET(uri("boolean").addParameter("p1", "true").addParameter("p2", "false")));
  }

  @Test
  public void namedParam() throws Exception {
    assertEquals("awesome",
        GET(uri("named-param").addParameter("not-java-name", "awesome")));
  }

  @Test
  public void missingParam() throws Exception {
    assertStatus(Response.Status.BAD_REQUEST, () -> GET(uri("boolean")));
  }

  @Test
  public void optionalBooleanParam() throws Exception {
    assertEquals("Optional.empty", GET(uri("booleanOptional")));

    assertEquals("Optional[true]", GET(uri("booleanOptional").addParameter("p", "true")));
  }

  @Test
  public void booleanListParam() throws Exception {
    assertEquals("[true, false]",
        GET(uri("booleanList").addParameter("p", "true").addParameter("p", "false")));
  }

  @Test
  public void byteParam() throws Exception {
    assertEquals("27", GET(uri("byte").addParameter("p1", "23").addParameter("p2", "4")));
  }

  @Test
  public void shortParam() throws Exception {
    assertEquals("3", GET(uri("short").addParameter("p1", "1").addParameter("p2", "2")));
  }

  @Test
  public void intParam() throws Exception {
    assertEquals("343", GET(uri("int").addParameter("p1", "40").addParameter("p2", "303")));
  }

  @Test
  public void optionalIntParam() throws Exception {
    assertEquals("Optional.empty", GET(uri("optionalInt")));
    assertEquals("Optional[40]", GET(uri("optionalInt").addParameter("p", "40")));
  }

  @Test
  public void longParam() throws Exception {
    assertEquals("343", GET(uri("long").addParameter("p1", "40").addParameter("p2", "303")));
  }

  @Test
  public void floatParam() throws Exception {
    assertEquals("343.5",
        GET(uri("float").addParameter("p1", "40").addParameter("p2", "303.5")));
  }

  @Test
  public void doubleParam() throws Exception {
    assertEquals("343.5",
        GET(uri("double").addParameter("p1", "40").addParameter("p2", "303.5")));
  }

  @Test
  public void enumParam() throws Exception {
    assertEquals("A, B",
        GET(uri("enum").addParameter("p1", "A").addParameter("p2", "B")));
  }

  @Test
  public void enumSortedParam() throws Exception {
    assertEquals("[A, B]",
        GET(uri("sortedEnum").addParameter("p", "B").addParameter("p", "A")));
  }

  @Test
  public void optionalEnumParam() throws Exception {
    assertEquals("Optional.empty", GET(uri("optionalEnum")));
    assertEquals("Optional[B]", GET(uri("optionalEnum").addParameter("p", "B")));
  }

  @Test
  public void uuidParam() throws Exception {
    String uuid = UUID.randomUUID().toString();
    assertEquals(uuid, GET(uri("uuid").addParameter("p", uuid)));
  }

  @Test
  public void decimalParam() throws Exception {
    BigDecimal decimal = new BigDecimal(Math.PI + "");
    assertEquals(decimal.toString(), GET(uri("decimal").addParameter("p", decimal.toString())));
  }

  private static String GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build()).execute().returnContent().asString();
  }

}
