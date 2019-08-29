package output;

import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.Route;
import io.jooby.internal.mvc.CoroutineLauncher;

import javax.annotation.Nonnull;
import javax.inject.Provider;

public class MvcExtension implements Extension {
  private Provider provider;

  public MvcExtension(Provider c) {
    this.provider = c;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    Route route = application.get("/", new CoroutineLauncher(new MyControllerHandler(provider)));
    route.setReturnType(Context.class);
  }
}
