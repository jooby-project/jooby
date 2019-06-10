package examples;

import io.jooby.Context;
import io.jooby.FlashMap;
import io.jooby.Formdata;
import io.jooby.Multipart;
import io.jooby.QueryString;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Path("/args")
public class Provisioning {

  @GET
  @Path("/ctx")
  public String getIt(Context ctx) {
    return ctx.pathString();
  }

  @GET
  @Path("/flash")
  public String flash(@FlashParam String success, FlashMap flash) {
    return flash + success;
  }

  @GET
  @Path("/sendStatusCode")
  public Context sendStatusCode(Context ctx) {
    return ctx.send(StatusCode.CREATED);
  }

  @GET
  @Path("/file/{filename}")
  public String file(@PathParam String filename) {
    return filename;
  }

  @GET
  @Path("/int/{id}")
  public int intId(@PathParam int id) {
    return id;
  }

  @GET
  @Path("/long/{value}")
  public long longparam(@PathParam long value) {
    return value;
  }

  @GET
  @Path("/float/{value}")
  public float floatparam(@PathParam float value) {
    return value;
  }

  @GET
  @Path("/double/{value}")
  public double doublevalue(@PathParam double value) {
    return value;
  }

  @GET
  @Path("/bool/{value}")
  public boolean boolvalue(@PathParam boolean value) {
    return value;
  }

  @GET
  @Path("/str/{value}")
  public String str(@PathParam String value) {
    return value;
  }

  @GET
  @Path("/list/{value}")
  public List<String> list(@PathParam List<String> value) {
    return value;
  }

  @GET
  @Path("/custom/{value}")
  public BigDecimal customvalue(@PathParam BigDecimal value) {
    return value;
  }

  @GET
  @Path("/{s}/{i}/{j}/{f}/{d}/{b}")
  public String mix(@PathParam String s, @PathParam Integer i, @PathParam double d, Context ctx,
      @PathParam long j, @PathParam Float f, @PathParam boolean b) {
    return ctx.pathString();
  }

  @GET
  @Path("/search")
  public String search(@QueryParam String q) {
    return q;
  }

  @GET
  @Path("/search-opt")
  public Optional<String> searchopt(@QueryParam Optional<String> q) {
    return q;
  }

  @GET
  @Path("/header")
  public String header(@HeaderParam String foo) {
    return foo;
  }

  @POST
  @Path("/form")
  public String form(@FormParam String foo) {
    return foo;
  }

  @GET
  @Path("/querystring")
  public QueryString queryString(QueryString queryString) {
    return queryString;
  }

  @POST
  @Path("/formdata")
  public Formdata formdata(Formdata data) {
    return data;
  }

  @POST
  @Path("/multipart")
  public Multipart multipart(Multipart data) {
    return data;
  }

  @GET
  @Path("/cookie")
  public String cookie(@CookieParam String foo) {
    return foo;
  }
}
