package source;

import io.jooby.annotations.GET;
import io.jooby.annotations.Path;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Path("/hello")
@TopAnnotation(TopEnum.FOO)
public class Controller1527 {
  public enum Role {USER, ADMIN}

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME) @interface RequireRole {
    Role value();
  }

  @GET @RequireRole(Role.ADMIN)
  public String login() {
    return "";
  }

  @GET @TopAnnotation({TopEnum.BAR, TopEnum.FOO})
  public String topannotation() {
    return "";
  }

  @GET
  @StringArrayAnnotation({"a", "b", "c"})
  public String classannotation() {
    return "";
  }
}
