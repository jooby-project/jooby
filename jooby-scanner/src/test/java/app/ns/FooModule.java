package app.ns;

import org.jooby.Env;
import org.jooby.Jooby.Module;

import com.google.inject.Binder;
import com.typesafe.config.Config;

public class FooModule implements Module {

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    binder.bind(FooModule.class);
  }

}
