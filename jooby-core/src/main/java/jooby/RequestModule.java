package jooby;

import com.google.inject.Binder;
import com.google.inject.Module;

public interface RequestModule extends Module {

  @Override
  void configure(Binder binder);

}
