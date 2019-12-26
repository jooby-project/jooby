package output;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.Route;
import io.jooby.internal.mvc.CoroutineLauncher;

import javax.annotation.Nonnull;
import javax.inject.Provider;

public class MvcExtension implements Extension {
  private Provider<MyController> provider;

  public MvcExtension(Provider<MyController> c) {
    this.provider = c;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    application.get("/mypath", new CoroutineLauncher(ctx -> {
      MyController myController = provider.get();
      return myController.controllerMethod();
    })).setReturnType(String.class)
        .attribute("RoleAnnotation", "User");
  }
}
