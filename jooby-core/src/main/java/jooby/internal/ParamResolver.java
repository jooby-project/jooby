package jooby.internal;

import java.lang.reflect.Method;
import java.util.List;

interface ParamResolver {

  List<ParameterDefinition> resolve(Method method) throws Exception;

}
