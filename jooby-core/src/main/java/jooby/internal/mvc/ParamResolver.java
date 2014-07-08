package jooby.internal.mvc;

import java.lang.reflect.Method;
import java.util.List;

interface ParamResolver {

  List<ParamValue> resolve(Method method) throws Exception;

}
