package source;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.annotations.GET;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RouteWithMimeTypes {

  @GET(value = "/consumes", consumes = "application/json")
  public String doIt(Context ctx) {
    assertEquals(Arrays.asList(MediaType.json), ctx.getRoute().getConsumes());
    return ctx.pathString();
  }

  @GET(value = "/consumes2", consumes = {"application/json", "application/xml"})
  public String consumes2(Context ctx) {
    assertEquals(Arrays.asList(MediaType.json, MediaType.xml), ctx.getRoute().getConsumes());
    return ctx.pathString();
  }

  @GET(value = "/produces", produces = "text/html")
  public String produces(Context ctx) {
    assertEquals(Arrays.asList(MediaType.html), ctx.getRoute().getProduces());
    return ctx.pathString();
  }

  @GET(value = "/consumes/produces", consumes = "text/html", produces = "text/html")
  public String consumesProduces(Context ctx) {
    assertEquals(Arrays.asList(MediaType.html), ctx.getRoute().getConsumes());
    assertEquals(Arrays.asList(MediaType.html), ctx.getRoute().getProduces());
    return ctx.pathString();
  }
}
