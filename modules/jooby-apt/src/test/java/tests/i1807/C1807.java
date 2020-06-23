package tests.i1807;

import io.jooby.annotations.FormParam;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

import javax.annotation.Nonnull;

public class C1807 {
  @Path("/test/{word}")
  @POST
  public Word1807 hello(@FormParam @Nonnull Word1807 data) {
    return data;
  }
}
