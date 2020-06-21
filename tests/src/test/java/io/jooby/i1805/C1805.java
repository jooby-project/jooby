package io.jooby.i1805;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;
import io.jooby.annotations.QueryParam;

import java.net.URI;
import java.net.URL;

@Path("/1805")
public class C1805 {
  @GET("/uri")
  public URI uri(@QueryParam URI param) {
    return param;
  }

  @GET("/url")
  public URL url(@QueryParam URL param) {
    return param;
  }
}
