package tests.i1807;

import io.jooby.annotations.FormParam;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

import edu.umd.cs.findbugs.annotations.NonNull;

public class C1807 {
  @Path("/test/{word}")
  @POST
  public Word1807 hello(@FormParam @NonNull Word1807 data) {
    return data;
  }
}
