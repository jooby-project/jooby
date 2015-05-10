package org.jooby.internal.reqparam;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jooby.Mutant;
import org.jooby.Parser;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.reflect.ParameterNameProvider;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Primitives;
import com.google.common.reflect.Reflection;
import com.google.inject.TypeLiteral;

public class BeanParser implements Parser {

  @Override
  public Object parse(final TypeLiteral<?> type, final Context ctx) throws Exception {
    Class<?> beanType = type.getRawType();
    if (Primitives.isWrapperType(Primitives.wrap(beanType))
        || CharSequence.class.isAssignableFrom(beanType)) {
      return ctx.next();
    }
    return ctx.params(map -> {
      final Object bean;
      if (beanType.isInterface()) {
        bean = newBeanInterface(ctx.require(Request.class), beanType);
      } else {
        bean = newBean(ctx.require(Request.class), ctx.require(Response.class), map, beanType);
      }

      return bean == null ? ctx.next() : bean;
    });
  }

  private static Object newBean(final Request req, final Response rsp,
      final Map<String, Mutant> params, final Class<?> beanType)
      throws Exception {
    ParameterNameProvider classInfo = req.require(ParameterNameProvider.class);
    Constructor<?>[] constructors = beanType.getDeclaredConstructors();
    if (constructors.length > 1) {
      return null;
    }
    final Object bean;
    Constructor<?> constructor = constructors[0];
    RequestParamProvider provider =
        new RequestParamProviderImpl(new RequestParamNameProvider(classInfo));
    List<RequestParam> parameters = provider.parameters(constructor);
    Object[] args = new Object[parameters.size()];
    for (int i = 0; i < args.length; i++) {
      args[i] = parameters.get(i).value(req, rsp);
    }
    // inject args
    bean = constructor.newInstance(args);

    // inject fields
    for (Entry<String, Mutant> param : params.entrySet()) {
      String name = param.getKey();
      try {
        Field field = beanType.getDeclaredField(name);
        int mods = field.getModifiers();
        if (!Modifier.isFinal(mods) && !Modifier.isStatic(mods)) {
          // get
          RequestParam fparam = new RequestParam(field);
          @SuppressWarnings("unchecked")
          Object value = req.param(fparam.name).to(fparam.type);

          // set
          field.setAccessible(true);
          field.set(bean, value);
        }
      } catch (NoSuchFieldException ex) {
        LoggerFactory.getLogger(Request.class).debug("No matching field: {}", name);
      }
    }
    return bean;
  }

  private static Object newBeanInterface(final Request req, final Class<?> beanType) {

    return Reflection.newProxy(beanType, (proxy, method, args) -> {
      StringBuilder name = new StringBuilder(method.getName()
          .replace("get", "")
          .replace("is", "")
          );
      name.setCharAt(0, Character.toLowerCase(name.charAt(0)));
      return req.param(name.toString()).to(TypeLiteral.get(method.getGenericReturnType()));
    });
  }

}
