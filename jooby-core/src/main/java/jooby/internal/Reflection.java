package jooby.internal;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

import com.google.common.collect.Lists;

public class Reflection {

  public static List<Method> methods(final Class<?> clazz) {
    return members(clazz, (owner) -> Lists.newArrayList(owner.getDeclaredMethods()));
  }

  private static <M extends Member> List<M> members(final Class<?> clazz,
      final Function<Class<?>, List<M>> supplier) {
    List<M> members = supplier.apply(clazz);
    if (clazz.getSuperclass() != null) {
      members.addAll(members(clazz.getSuperclass(), supplier));
    } else if (clazz.isInterface()) {
      for (Class<?> superIfc : clazz.getInterfaces()) {
        members.addAll(members(superIfc, supplier));
      }
    }
    return members;
  }

}
