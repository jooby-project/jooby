package apps;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;

@Path("/api/1182")
public class Controller1182 {

  public static class Query {

    @ApiModelProperty(hidden = true)
    public String internal;

    @ApiModelProperty("Search query")
    public String q;

    @ApiModelProperty(hidden = true)
    public String getInternalFoo() {
      return q + "foo";
    }
  }

  @GET
  @ApiOperation(value = "Api param works")
  public String list(@ApiParam(hidden = true) String internal, @ApiParam(value = "bar name is ignored", name = "bar") String foo, Query q) {
    return foo;
  }

}
