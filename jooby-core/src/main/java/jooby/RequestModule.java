package jooby;

import java.util.Set;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

public interface RequestModule extends Module {

  Key<Set<RequestModule>> KEY = Key.get(new TypeLiteral<Set<RequestModule>>() {});

  @Override
  void configure(Binder binder);

}
