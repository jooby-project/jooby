package org.jooby.integration;

import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.jooby.mvc.GET;
import org.jooby.mvc.Header;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class HeaderParamFeature extends ServerFeature {

  public enum VOWELS {
    A,
    B;
  }

  public static class Resource {

    @GET
    @Path("/boolean")
    public Object booleanHeader(@Header final boolean h) {
      return h;
    }

    @GET
    @Path("/booleanList")
    public Object booleanHeader(@Header final List<Boolean> h) {
      return h.toString();
    }

    @GET
    @Path("/booleanOptional")
    public Object booleanHeader(@Header final Optional<Boolean> h) {
      return h.toString();
    }

    @GET
    @Path("/modifiedSince")
    public Object modifiedSince(@Named("If-Modified-Since") @Header final Optional<Long> h) {
      return h.orElse(-1l);
    }

    @GET
    @Path("/int")
    public Object intHeader(@Header final int h) {
      return h;
    }

    @GET
    @Path("/enum")
    public Object enumHeader(@Header final VOWELS h) {
      return h;
    }

  }

  {
    use(Resource.class);
  }

  @Test
  public void booleanHeader() throws Exception {
    request()
        .get("/boolean")
        .header("h", true)
        .expect("true");
  }

  @Test
  public void booleanListHeader() throws Exception {
    request()
        .get("/booleanList")
        .header("h", true)
        .header("h", false)
        .expect("[true, false]");
  }

  @Test
  public void booleanOptionalHeader() throws Exception {
    request()
        .get("/booleanOptional")
        .expect("Optional.empty");

    request()
        .get("/booleanOptional")
        .header("h", true)
        .expect("Optional[true]");
  }

  @Test
  public void modifiedSince() throws Exception {
    request()
        .get("/modifiedSince")
        .expect("-1");

    request()
        .get("/modifiedSince")
        .header("If-Modified-Since", "Mon, 14 Jul 2014 21:35:09 GMT")
        .expect("1405373709000");
  }

  @Test
  public void missingHeader() throws Exception {
    request()
        .get("/int")
        .expect(400);
  }

  @Test
  public void intHeader() throws Exception {
    request()
        .get("/int")
        .header("h", "302")
        .expect("302");
  }

  @Test
  public void enumHeader() throws Exception {
    request()
        .get("/enum")
        .header("h", "A")
        .expect("A");
  }

}
