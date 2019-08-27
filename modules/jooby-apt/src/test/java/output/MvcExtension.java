package output;

import io.jooby.Extension;
import io.jooby.Jooby;

import javax.annotation.Nonnull;
import javax.inject.Provider;

public class MvcExtension implements Extension {
  public MvcExtension(Provider c) {

  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    application.get("/", ctx -> "..")
        .setReturnType(int.class);
  }
}
