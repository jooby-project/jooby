package examples;

import io.jooby.FileUpload;
import io.jooby.annotations.FormParam;
import io.jooby.annotations.POST;

public class FormController {

  @POST("/single")
  public String postSingle(@FormParam String name) {
    return "...";
  }

  @POST("/multiple")
  public String postMultiple(@FormParam String firstname, @FormParam String lastname, @FormParam
      FileUpload picture) {
    return "...";
  }

  @POST("/bean")
  public String postBean(@FormParam AForm form) {
    return "...";
  }
}
