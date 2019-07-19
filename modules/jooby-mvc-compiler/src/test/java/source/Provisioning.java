package source;

import io.jooby.Context;
import io.jooby.FlashMap;
import io.jooby.Formdata;
import io.jooby.Multipart;
import io.jooby.QueryString;
import io.jooby.Session;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Path("/controller")
public class Provisioning {

  @GET
  public String noarg() {
    return "noarg";
  }

  @GET("/ctx")
  public String context(Context ctx) {
    assertTrue(ctx instanceof Context);
    return "ctx";
  }

  @GET("/ctxfirst")
  public String contextFirst(Context ctx, QueryString queryString) {
    assertTrue(ctx instanceof Context);
    assertTrue(queryString instanceof QueryString);
    return "ctxfirst";
  }

  @GET("/queryString")
  public String queryString(QueryString queryString) {
    assertTrue(queryString instanceof QueryString);
    return "queryString";
  }

  @GET("/queryStringOptional")
  public String queryStringOptional(Optional<QueryString> queryString) {
    queryString.ifPresent(it -> assertTrue(it instanceof QueryString));
    return "queryStringOptional:" + queryString.isPresent();
  }

  @POST("/formdata")
  public String formdata(Formdata value) {
    assertTrue(value instanceof Formdata);
    return "formdata";
  }

  @POST("/multipart")
  public String multipart(Multipart value) {
    assertTrue(value instanceof Multipart);
    return "multipart";
  }

  @GET("/flashMap")
  public String flashMap(FlashMap value) {
    assertTrue(value instanceof FlashMap);
    return "flashMap";
  }

  @GET("/session")
  public String session(Session value) {
    assertTrue(value instanceof Session);
    return "session";
  }

  @GET
  public String sessionOrNull(Optional<Session> session) {
    return "session:" + session.isPresent();
  }

  @GET
  public String pathParam(@PathParam String p1) {
    return p1;
  }

  @GET
  public String bytePathParam(@PathParam byte p1) {
    return Integer.toString(p1);
  }

  @GET
  public String intPathParam(@PathParam int p1) {
    return Integer.toString(p1);
  }

  @GET
  public String longPathParam(@PathParam long p1) {
    return Long.toString(p1);
  }

  @GET
  public String floatPathParam(@PathParam float p1) {
    return Float.toString(p1);
  }

  @GET
  public String doublePathParam(@PathParam double p1) {
    return Double.toString(p1);
  }

  @GET
  public String booleanPathParam(@PathParam boolean p1) {
    return Boolean.toString(p1);
  }

  @GET
  public String optionalStringPathParam(@PathParam Optional<String> p1) {
    return p1.toString();
  }

  @GET
  public String optionalIntPathParam(@PathParam Optional<Integer> p1) {
    return p1.toString();
  }

  @GET
  public String javaBeanPathParam(@PathParam JavaBeanParam param) {
    return param.toString();
  }

  @GET
  public String listStringPathParam(@PathParam List<String> values) {
    return values.toString();
  }

  @GET
  public String listDoublePathParam(@PathParam List<Double> values) {
    return values.toString();
  }

  @GET
  public String listBeanPathParam(@PathParam List<JavaBeanParam> bean) {
    return bean.toString();
  }

  @GET
  public String setStringPathParam(@PathParam Set<String> values) {
    return values.toString();
  }

  @GET
  public String setDoublePathParam(@PathParam Set<Double> values) {
    return values.toString();
  }

  @GET
  public String setBeanPathParam(@PathParam Set<JavaBeanParam> bean) {
    return bean.toString();
  }

  @GET
  public String enumParam(@PathParam EnumParam letter) {
    return letter.name();
  }

  @GET
  public String optionalEnumParam(@PathParam Optional<EnumParam> letter) {
    return letter.toString();
  }

  @GET
  public String listEnumParam(@PathParam List<EnumParam> letter) {
    return letter.toString();
  }
}
