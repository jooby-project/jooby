package scanner.sub;

import org.jooby.Env;
import org.jooby.Jooby;

import com.google.inject.Binder;
import com.typesafe.config.Config;

public class Submod implements Jooby.Module {

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
  }
}
