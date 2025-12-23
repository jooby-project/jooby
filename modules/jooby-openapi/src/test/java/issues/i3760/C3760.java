/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3760;

import java.util.Map;

import io.jooby.annotation.GET;
import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Path("/3760")
public class C3760 {

  /**
   * Search/scan index.
   *
   * @param q Search string. Defaults to <code>*</code>
   * @return Search result.
   */
  @GET("/")
  public Map<String, Object> search(
      @QueryParam("*") String q,
      @Min(10) @Max(50) @QueryParam("20") int pageSize,
      @Min(1) @QueryParam("1") int page) {
    return null;
  }

  @GET("/query")
  public Q3760 query(@QueryParam Q3760 queryString) {
    return null;
  }

  @POST
  public UP3760 save(UP3760 body) {
    return null;
  }
}
