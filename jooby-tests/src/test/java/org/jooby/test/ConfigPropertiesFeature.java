package org.jooby.test;

import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.client.fluent.Request;
import org.jooby.Mode;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.junit.Test;

public class ConfigPropertiesFeature extends ServerFeature {

  public enum Letter {
    A,
    B;
  }

  @Path("/r")
  public static class Resource {

    private Charset charset;
    private Integer intprop;
    private String stringprop;
    private Mode mode;
    private List<String> list;
    private Letter letter;
    private UUID uuid;

    @Inject
    public Resource(final Charset charset, final Mode mode,
        @Named("intprop") final int intprop,
        @Named("stringprop") final String stringprop,
        @Named("list") final List<String> list,
        @Named("letter") final Letter letter,
        @Named("uuid") final UUID uuid) {
      this.charset = charset;
      this.mode = mode;
      this.intprop = intprop;
      this.stringprop = stringprop;
      this.list = list;
      this.letter = letter;
      this.uuid = uuid;
    }

    @GET
    @Path("/properties")
    public Object properties() {
      return charset + " " + intprop + " " + stringprop + " " + uuid;
    }

    @GET
    @Path("/mode")
    public Object mode() {
      return mode;
    }

    @GET
    @Path("/list")
    public Object list() {
      return list;
    }

    @GET
    @Path("/letter")
    public Object letter() {
      return letter;
    }
  }

  {
    use(Resource.class);
  }

  @Test
  public void properties() throws Exception {
    assertEquals("UTF-8 14 The man who sold the world a8843f4a-2c71-42ef-82aa-83fa8246c0d4", Request.Get(uri("r", "properties").build())
        .execute().returnContent()
        .asString());
  }

  @Test
  public void mode() throws Exception {
    assertEquals("dev", Request.Get(uri("r", "mode").build()).execute().returnContent()
        .asString());
  }

  @Test
  public void list() throws Exception {
    assertEquals("[a, b, c]", Request.Get(uri("r", "list").build()).execute().returnContent()
        .asString());
  }

  @Test
  public void letter() throws Exception {
    assertEquals("A", Request.Get(uri("r", "letter").build()).execute().returnContent()
        .asString());
  }

}
