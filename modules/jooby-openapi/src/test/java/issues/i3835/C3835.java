/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3835;

import java.util.List;
import java.util.Map;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;

@Path("/3835")
public class C3835 {

  /**
   * Search/scan index.
   *
   * @param q Search string. Defaults to <code>*</code>
   * @return Search result.
   */
  @GET("/")
  public Map<String, Object> search(
      @QueryParam("*") String q,
      @QueryParam("20") int pageSize,
      @QueryParam("1") int page,
      @QueryParam("--a") List<String> options) {
    return null;
  }
}
