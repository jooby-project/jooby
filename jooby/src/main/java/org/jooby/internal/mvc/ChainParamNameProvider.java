package org.jooby.internal.mvc;

import java.lang.reflect.Parameter;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class ChainParamNameProvider implements ParamNameProvider {

  private final List<ParamNameProvider> providers;

  public ChainParamNameProvider(final ParamNameProvider... providers) {
    this.providers = ImmutableList.copyOf(providers);
  }

  @Override
  public String name(final int index, final Parameter parameter) {
    for (ParamNameProvider provider : providers) {
      String name = provider.name(index, parameter);
      if (name != null) {
        return name;
      }
    }
    return null;
  }

}
