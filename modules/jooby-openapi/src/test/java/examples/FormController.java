package examples;

import io.jooby.annotations.FormParam;
import io.jooby.annotations.POST;

public class FormController {

  @POST("/single")
  public String postSingle(@FormParam String name) {
    return "...";
  }

  @POST("/multiple")
  public String postMultiple(@FormParam String firstname, @FormParam String lastname) {
    return "...";
  }

  @POST("/bean")
  public String postBean(@FormParam ABean form) {
    return "...";
  }
}
