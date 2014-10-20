package org.jooby.internal.mvc;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedParamProvider implements ParamProvider {

  private Map<Executable, List<Param>> cache = new ConcurrentHashMap<>();

  private ParamProvider provider;

  public CachedParamProvider(final ParamProvider provider) {
    this.provider = provider;
  }

  @Override
  public List<Param> parameters(final Executable exec) {
    Parameter[] parameters = exec.getParameters();
    if (parameters.length == 0) {
      return Collections.emptyList();
    }
    List<Param> params = cache.get(exec);
    if (params == null) {
      cache.put(exec, provider.parameters(exec));
    }
    return null;
  }

}
