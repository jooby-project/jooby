package jooby.internal.mvc;

import java.lang.reflect.Method;
import java.util.List;

import jooby.internal.ParameterDefinition;

interface ParamResolver {

  List<ParameterDefinition> resolve(Method method) throws Exception;

}
