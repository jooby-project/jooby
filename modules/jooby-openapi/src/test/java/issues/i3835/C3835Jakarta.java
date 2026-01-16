/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3835;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.List;
import java.util.Map;

@Path("/3835")
public class C3835Jakarta {

  /**
   * Search/scan index.
   *
   * @param q Search string. Defaults to <code>*</code>
   * @return Search result.
   */
  @GET
  public Map<String, Object> search(
      @QueryParam("q") @DefaultValue("*") String q,
      @QueryParam("pageSize") @DefaultValue("20") int pageSize,
      @QueryParam("page") @DefaultValue("1") int page,
      @QueryParam("options") @DefaultValue("--a") List<String> options) {
    return null;
  }
}
