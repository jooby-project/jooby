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

@Path("/p")
public class Provisioning {

  @GET("/noarg")
  public String noarg() {
    return "noarg";
  }

  @GET("/context")
  public String context(Context ctx) {
    assertTrue(ctx instanceof Context);
    return "ctx";
  }

  @GET("/contextFirst")
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

  @GET("/sessionOrNull")
  public String sessionOrNull(Optional<Session> session) {
    return "session:" + session.isPresent();
  }

  @GET("/pathParam")
  public String pathParam(@PathParam String p1) {
    return p1;
  }

  @GET("/bytePathParam")
  public String bytePathParam(@PathParam byte p1) {
    return Integer.toString(p1);
  }

  @GET("/intPathParam")
  public String intPathParam(@PathParam int p1) {
    return Integer.toString(p1);
  }

  @GET("/longPathParam")
  public String longPathParam(@PathParam long p1) {
    return Long.toString(p1);
  }

  @GET("/floatPathParam")
  public String floatPathParam(@PathParam float p1) {
    return Float.toString(p1);
  }

  @GET("/doublePathParam")
  public String doublePathParam(@PathParam double p1) {
    return Double.toString(p1);
  }

  @GET("/booleanPathParam")
  public String booleanPathParam(@PathParam boolean p1) {
    return Boolean.toString(p1);
  }

  @GET("/optionalStringPathParam")
  public String optionalStringPathParam(@PathParam Optional<String> p1) {
    return p1.toString();
  }

  @GET("/optionalIntPathParam")
  public String optionalIntPathParam(@PathParam Optional<Integer> p1) {
    return p1.toString();
  }

  @GET("/javaBeanPathParam")
  public String javaBeanPathParam(@PathParam JavaBeanParam param) {
    return param.toString();
  }

  @GET("/listStringPathParam")
  public String listStringPathParam(@PathParam List<String> values) {
    return values.toString();
  }

  @GET("/listDoublePathParam")
  public String listDoublePathParam(@PathParam List<Double> values) {
    return values.toString();
  }

  @GET("/listBeanPathParam")
  public String listBeanPathParam(@PathParam List<JavaBeanParam> bean) {
    return bean.toString();
  }

  @GET("/setStringPathParam")
  public String setStringPathParam(@PathParam Set<String> values) {
    return values.toString();
  }

  @GET("/setDoublePathParam")
  public String setDoublePathParam(@PathParam Set<Double> values) {
    return values.toString();
  }

  @GET("/setBeanPathParam")
  public String setBeanPathParam(@PathParam Set<JavaBeanParam> bean) {
    return bean.toString();
  }

  @GET("/enumParam")
  public String enumParam(@PathParam EnumParam letter) {
    return letter.name();
  }

  @GET("/optionalEnumParam")
  public String optionalEnumParam(@PathParam Optional<EnumParam> letter) {
    return letter.toString();
  }

  @GET("/listEnumParam")
  public String listEnumParam(@PathParam List<EnumParam> letter) {
    return letter.toString();
  }

  @GET("/primitiveWrapper")
  public String primitiveWrapper(@PathParam Integer value) {
    return String.valueOf(value);
  }

  @GET("/queryParam")
  public String queryParam(@QueryParam String q) {
    return q;
  }

  @GET("/cookieParam")
  public String cookieParam(@CookieParam String c) {
    return c;
  }

  @GET("/headerParam")
  public String headerParam(@HeaderParam Instant instant) {
    return String.valueOf(instant.toEpochMilli());
  }

  @GET("/flashParam")
  public String flashParam(@FlashParam String message) {
    return message;
  }

  @GET("/formParam")
  public String formParam(@FormParam String name) {
    return name;
  }

  @GET("/parameters")
  public String parameters(@PathParam String path, Context ctx, @QueryParam int offset, @QueryParam JavaBeanParam javaBean) {
    return path + ctx + offset + javaBean;
  }

  @GET("/fileParam")
  public String fileParam(FileUpload file) {
    return file.toString();
  }

  @GET("/fileParams")
  public String fileParams(List<FileUpload> file) {
    return file.toString();
  }

  @GET("/uuidParam")
  public String uuidParam(@QueryParam UUID value) {
    return value.toString();
  }

  @GET("/bigDecimalParam")
  public String bigDecimalParam(@QueryParam BigDecimal value) {
    return value.toString();
  }

  @GET("/bigIntegerParam")
  public String bigIntegerParam(@QueryParam BigInteger value) {
    return value.toString();
  }

  @GET("/charsetParam")
  public String charsetParam(@QueryParam Charset value) {
    return value.toString();
  }

  @GET("/pathFormParam")
  public String pathFormParam(@FormParam java.nio.file.Path file) {
    return file.toString();
  }

  @GET("/returnByte")
  public byte returnByte() {
    return 8;
  }

  @GET("/returnShort")
  public short returnShort() {
    return 8;
  }

  @GET("/returnInteger")
  public int returnInteger() {
    return 7;
  }

  @GET("/returnLong")
  public long returnLong() {
    return 9;
  }

  @GET("/returnFloat")
  public float returnFloat() {
    return 7.9f;
  }

  @GET("/returnChar")
  public char returnChar() {
    return 'c';
  }

  @GET("/returnDouble")
  public double returnDouble() {
    return 8.9;
  }

  @GET("/returnStatusCode")
  public StatusCode returnStatusCode() {
    return StatusCode.NO_CONTENT;
  }

  @GET("/statusCode")
  public StatusCode statusCode(@QueryParam StatusCode statusCode, @QueryParam String q) {
    return statusCode;
  }

  @GET("/noContent")
  public void noContent() {
  }

  @GET("/sideEffect")
  public void sideEffect(Context ctx) {
    ctx.send(StatusCode.CREATED);
  }

  @POST
  @Path("/bodyStringParam")
  public String bodyStringParam(String body) {
    return body;
  }

  @POST
  @Path("/bodyBytesParam")
  public String bodyBytesParam(byte[] body) {
    return new String(body, StandardCharsets.UTF_8);
  }


  @POST
  @Path("/bodyInputStreamParam")
  public String bodyInputStreamParam(InputStream body) {
    assertTrue(body instanceof InputStream);
    return body.toString();
  }

  @POST
  @Path("/bodyChannelParam")
  public String bodyChannelParam(ReadableByteChannel body) {
    assertTrue(body instanceof ReadableByteChannel);
    return body.toString();
  }

  @POST
  @Path("/bodyBeanParam")
  public String bodyBeanParam(JavaBeanParam body) {
    return body.toString();
  }

  @POST
  @Path("/bodyIntParam")
  public int bodyIntParam(int body) {
    return body;
  }

  @POST
  @Path("/bodyOptionalIntParam")
  public Optional<Integer> bodyOptionalIntParam(Optional<Integer> body) {
    return body;
  }

  @POST
  @Path("/bodyMapParam")
  public Map<String, Object> bodyMapParam(Map<String, Object> json) {
    return json;
  }

  @POST
  @Path("/bodyCustomGenericParam")
  public CustomGenericType<String> bodyCustomGenericParam(CustomGenericType<String> body) {
    return body;
  }
}
