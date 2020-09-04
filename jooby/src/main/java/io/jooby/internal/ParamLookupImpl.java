/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.ParamLookup;
import io.jooby.ParamSource;
import io.jooby.Value;

import java.util.ArrayList;
import java.util.List;

public class ParamLookupImpl implements ParamLookup.Stage {

  private final Context context;
  private final List<ParamSource> chain = new ArrayList<>();

  public ParamLookupImpl(Context context) {
    this.context = context;
  }

  @Override
  public Stage in(ParamSource source) {
    chain.add(source);
    return this;
  }

  @Override
  public Value get(String name) {
    return context.lookup(name, chain.toArray(new ParamSource[0]));
  }
}
