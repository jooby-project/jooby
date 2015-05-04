package org.jooby.internal.reqparam;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map.Entry;

import org.jooby.Mutant;
import org.jooby.ParamConverter;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.reflect.ParameterNameProvider;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.Reflection;
import com.google.inject.TypeLiteral;

public class BeanParamConverter implements ParamConverter {

  @Override
  public Object convert(final TypeLiteral<?> type, final Object[] values, final Context ctx)
      throws Exception {
    if (values.length == 0) {
      Class<?> beanType = type.getRawType();
      final Object bean;
      if (beanType.isInterface()) {
        bean = newBeanInterface(ctx, beanType);
      } else {
        bean = newBean(ctx, beanType);
      }

      return bean == null ? ctx.convert(type, values) : bean;
    }
    return ctx.convert(type, values);
  }

  private static Object newBean(final Context ctx, final Class<?> beanType) throws Exception {
    Request req = ctx.require(Request.class);
    Response rsp = ctx.require(Response.class);
    ParameterNameProvider classInfo = ctx.require(ParameterNameProvider.class);
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
    for (Entry<String, Mutant> param : req.params().entrySet()) {
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

  private static Object newBeanInterface(final Context ctx, final Class<?> beanType) {
    Request req = ctx.require(Request.class);

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
