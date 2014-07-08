package jooby;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class Reflection {

  public static class Annotations {
    public static List<Annotation> anyOf(final AnnotatedElement owner,
        final Class<? extends Annotation> first,
        final Class<? extends Annotation> second) {
      return anyOf(owner, ImmutableSet.of(first, second));
    }

    public static List<Annotation> anyOf(final AnnotatedElement owner,
        final Class<? extends Annotation> first,
        final Class<? extends Annotation> second,
        final Class<? extends Annotation> third) {
      return anyOf(owner, ImmutableSet.of(first, second, third));
    }

    public static List<Annotation> anyOf(final AnnotatedElement owner,
        final Class<? extends Annotation> first,
        final Class<? extends Annotation> second,
        final Class<? extends Annotation> third,
        final Class<? extends Annotation> four) {
      return anyOf(owner, ImmutableSet.of(first, second, third, four));
    }

    public static List<Annotation> anyOf(final AnnotatedElement owner,
        final Class<? extends Annotation>[] annotations) {
      return anyOf(owner, ImmutableSet.copyOf(annotations));
    }

    public static List<Annotation> anyOf(final AnnotatedElement owner,
        final Set<Class<? extends Annotation>> annotations) {
      return annotations.stream()
          .filter(owner::isAnnotationPresent)
          .map(type -> owner.getAnnotation(type))
          .collect(Collectors.toList());
    }
  }

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
