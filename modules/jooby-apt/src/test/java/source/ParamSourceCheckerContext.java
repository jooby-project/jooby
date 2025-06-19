/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import java.util.function.Consumer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.ParamSource;
import io.jooby.test.MockContext;
import io.jooby.value.Value;

public class ParamSourceCheckerContext extends MockContext {

  private final Consumer<ParamSource[]> onLookup;

  public ParamSourceCheckerContext(Consumer<ParamSource[]> onLookup) {
    this.onLookup = onLookup;
  }

  @Override
  public Value lookup(@NonNull String name, ParamSource... sources) {
    onLookup.accept(sources);
    return super.lookup(name, sources);
  }
}
