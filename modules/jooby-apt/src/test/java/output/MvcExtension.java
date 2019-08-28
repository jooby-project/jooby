package output;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.Route;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import java.util.Arrays;

public class MvcExtension implements Extension {
  public MvcExtension(Provider c) {

  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    Route route = application.get("/", ctx -> "..");
    route.attribute("str", "string");
    route.attribute("annotation", Arrays.asList(1, 40, 1204, 33400));
  }
}
