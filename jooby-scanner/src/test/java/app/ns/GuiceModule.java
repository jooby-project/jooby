package app.ns;

import com.google.inject.Binder;
import com.google.inject.Module;

public class GuiceModule implements Module {

  @Override
  public void configure(final Binder binder) {
    binder.bind(GuiceModule.class);
  }

}
