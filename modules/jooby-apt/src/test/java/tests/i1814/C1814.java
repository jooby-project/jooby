package tests.i1814;

import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class C1814 {
  @GET("/1814")
  public List<? extends U1814> getUsers(@QueryParam @Nonnull String type, Route route) {
    assertEquals(Reified.list(U1814.class).getType(), route.getReturnType());
    return Collections.singletonList(new U1814(type));
  }
}
