package org.jooby.integration;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.Env;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class ConfigPropertiesFeature extends ServerFeature {

  public static class ValueOf {

    private String value;

    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof ValueOf) {
        return value.equals(((ValueOf) obj).value);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public String toString() {
      return value.toString();
    }

    public static ValueOf valueOf(final String value) {
      ValueOf v = new ValueOf();
      v.value = value;
      return v;
    }

  }

  public enum Letter {
    A,
    B;
  }

  @Path("/r")
  public static class Resource {

    private Charset charset;
    private Integer intprop;
    private String stringprop;
    private Env mode;
    private List<String> list;
    private Letter letter;
    private UUID uuid;
    private ValueOf valueOf;

    @Inject
    public Resource(final Charset charset, final Env mode,
        @Named("intprop") final int intprop,
        @Named("stringprop") final String stringprop,
        @Named("list") final List<String> list,
        @Named("letter") final Letter letter,
        @Named("uuid") final UUID uuid,
        @Named("valueOf") final ValueOf valueOf) {
      this.charset = charset;
      this.mode = mode;
      this.intprop = intprop;
      this.stringprop = stringprop;
      this.list = list;
      this.letter = letter;
      this.uuid = uuid;
      this.valueOf = valueOf;
    }

    @GET
    @Path("/properties")
    public Object properties() {
      return charset + " " + intprop + " " + stringprop + " " + uuid + " " + valueOf;
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
    use(ConfigFactory.parseResources("test.conf"));
    use(Resource.class);
  }

  @Test
  public void properties() throws Exception {
    request()
        .get("/r/properties")
        .expect("UTF-8 14 The man who sold the world a8843f4a-2c71-42ef-82aa-83fa8246c0d4 valueOf");
  }

  @Test
  public void mode() throws Exception {
    request()
        .get("/r/mode")
        .expect("dev");
  }

  @Test
  public void list() throws Exception {
    request()
        .get("/r/list")
        .expect("[a, b, c]");
  }

  @Test
  public void letter() throws Exception {
    request()
        .get("/r/letter")
        .expect("A");
  }

}
