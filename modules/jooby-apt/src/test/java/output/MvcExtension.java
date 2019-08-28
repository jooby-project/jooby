package output;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.Route;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MvcExtension implements Extension {
  public MvcExtension(Provider c) {

  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    Route route = application.get("/", ctx -> "..");
    route.attribute("str", "string");
    route.attribute("annotation", newMap());
  }

  private static Map<String, Object> newMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("LinkAnnotation", "v");
    return map;
  }
}
