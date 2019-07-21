package source;

import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.FlashMap;
import io.jooby.Formdata;
import io.jooby.Multipart;
import io.jooby.QueryString;
import io.jooby.Session;
import io.jooby.StatusCode;
import io.jooby.annotations.CookieParam;
import io.jooby.annotations.FlashParam;
import io.jooby.annotations.FormParam;
import io.jooby.annotations.GET;
import io.jooby.annotations.HeaderParam;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;
import io.jooby.annotations.QueryParam;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

  @GET("/formdata")
  public String formdata(Formdata value) {
    assertTrue(value instanceof Formdata);
    return "formdata";
  }

  @GET("/multipart")
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

  @GET
  public String primitiveWrapper(@PathParam Integer value) {
    return String.valueOf(value);
  }

  @GET
  public String queryParam(@QueryParam String q) {
    return q;
  }

  @GET
  public String cookieParam(@CookieParam String c) {
    return c;
  }

  @GET
  public String headerParam(@HeaderParam Instant instant) {
    return String.valueOf(instant.toEpochMilli());
  }

  @GET
  public String flashParam(@FlashParam String message) {
    return message;
  }

  @GET
  public String formParam(@FormParam String name) {
    return name;
  }

  @GET
  public String parameters(@PathParam String path, Context ctx, @QueryParam int offset, @QueryParam JavaBeanParam javaBean) {
    return path + ctx + offset + javaBean;
  }

  @GET
  public String fileParam(FileUpload file) {
    return file.toString();
  }

  @GET
  public String fileParams(List<FileUpload> file) {
    return file.toString();
  }

  @GET
  public String uuidParam(@QueryParam UUID value) {
    return value.toString();
  }

  @GET
  public String bigDecimalParam(@QueryParam BigDecimal value) {
    return value.toString();
  }

  @GET
  public String bigIntegerParam(@QueryParam BigInteger value) {
    return value.toString();
  }

  @GET
  public String charsetParam(@QueryParam Charset value) {
    return value.toString();
  }

  @GET
  public String pathParam(@QueryParam Charset value) {
    return value.toString();
  }

  @GET
  public String pathParam(@FormParam java.nio.file.Path file) {
    return file.toString();
  }

  @GET
  public byte returnByte() {
    return 8;
  }

  @GET
  public short returnShort() {
    return 8;
  }

  @GET
  public int returnInteger() {
    return 7;
  }

  @GET
  public long returnLong() {
    return 9;
  }

  @GET
  public float returnFloat() {
    return 7.9f;
  }

  @GET
  public char returnChar() {
    return 'c';
  }

  @GET
  public double returnDouble() {
    return 8.9;
  }

  @GET
  public StatusCode returnStatusCode() {
    return StatusCode.NO_CONTENT;
  }

  @GET
  public StatusCode statusCode(@QueryParam StatusCode statusCode, @QueryParam String q) {
    return statusCode;
  }

  @GET
  public void noContent() {
  }

  @GET
  public void sideEffect(Context ctx) {
    ctx.send(StatusCode.CREATED);
  }

  @POST
  @Path("/body/str")
  public String bodyStringParam(String body) {
    return body;
  }

  @POST
  @Path("/body/bytes")
  public String bodyBytesParam(byte[] body) {
    return new String(body, StandardCharsets.UTF_8);
  }


  @POST
  @Path("/body/stream")
  public String bodyInputStreamParam(InputStream body) {
    assertTrue(body instanceof InputStream);
    return body.toString();
  }

  @POST
  @Path("/body/channel")
  public String bodyChannelParam(ReadableByteChannel body) {
    assertTrue(body instanceof ReadableByteChannel);
    return body.toString();
  }

  @POST
  @Path("/body/bean")
  public String bodyBeanParam(JavaBeanParam body) {
    return body.toString();
  }
}
