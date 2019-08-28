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
    application.dispatch(new MvcDispatch(application));
  }
}
