package source;

import io.jooby.annotations.GET;

public class RouteAttributes {
  @SomeAnnotation(annotation = @LinkAnnotation("IGNORED"), value = "string", d = 99, f = 8, i = 5, l = 200, type = Integer.class, bool = true, values = {
      "a", "b"}, c = 'X', s = Short.MIN_VALUE)
  @RoleAnnotation("User")
  @GET
  public String attributes() {
    return "...";
  }
}
