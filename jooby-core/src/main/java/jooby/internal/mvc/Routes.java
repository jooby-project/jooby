package jooby.internal.mvc;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jooby.MediaType;
import jooby.Mode;
import jooby.Reflection;
import jooby.Route;
import jooby.RouteDefinition;
import jooby.mvc.Consumes;
import jooby.mvc.DELETE;
import jooby.mvc.GET;
import jooby.mvc.POST;
import jooby.mvc.PUT;
import jooby.mvc.Path;
import jooby.mvc.Produces;
import net.sf.cglib.reflect.FastClass;

import com.google.common.collect.ImmutableSet;

public class Routes {

  private static final Set<Class<? extends Annotation>> VERBS = ImmutableSet.of(GET.class,
      POST.class, PUT.class, DELETE.class);

  public static List<RouteDefinition> route(final Mode mode, final Class<?> routeClass) {
    boolean reloadParams = mode.name().equals("dev");
    ParamResolver paramResolver = new DefaultParamResolver(reloadParams);
    FastClass fastRoute = FastClass.create(routeClass);
    String rootPath = path(routeClass);
    return Reflection
        .methods(routeClass)
        .stream()
        .filter(m -> {
          Set<Annotation> annotations = Reflection.Annotations.anyOf(m, VERBS);
          if (annotations.size() == 0) {
            return false;
          }
          if (annotations.size() > 1) {
            // TODO: error
            return false;
          }
          Class<?> returnType = m.getReturnType();
          if (returnType == void.class) {
            // TODO: error
            return false;
          }
          return true;
        })
        .map(
            m -> {
              String verb = verb(m);
              String path = rootPath + path(m);
              checkArgument(path.length() > 0, "Missing path for: %s.%s", routeClass.getSimpleName(),
                  m.getName());
              Route resource = new MvcRoute(fastRoute.getMethod(m), paramResolver);
              return new RouteDefinition(verb, path, resource)
                  .produces(produces(m))
                  .consumes(consumes(m));
            })
        .collect(Collectors.toList());
  }

  private static List<MediaType> produces(final Method method) {
    Function<AnnotatedElement, Optional<List<MediaType>>> fn = (element) -> {
      Produces produces = element.getAnnotation(Produces.class);
      if (produces != null) {
        return Optional.of(MediaType.valueOf(produces.value()));
      }
      return Optional.empty();
    };

    // method level
    return fn.apply(method)
        // class level
        .orElseGet(() -> fn.apply(method.getDeclaringClass())
            // none
            .orElse(MediaType.ALL));
  }

  private static  List<MediaType> consumes(final Method method) {
    Function<AnnotatedElement, Optional<List<MediaType>>> fn = (element) -> {
      Consumes consumes = element.getAnnotation(Consumes.class);
      if (consumes != null) {
        return Optional.of(MediaType.valueOf(consumes.value()));
      }
      return Optional.empty();
    };

    // method level
    return fn.apply(method)
        // class level
        .orElseGet(() -> fn.apply(method.getDeclaringClass())
            // none
            .orElse(MediaType.ALL));
  }

  @SuppressWarnings("unchecked")
  private static String verb(final Method method) {
    for (Class<?> annotation : VERBS) {
      if (method.isAnnotationPresent((Class<? extends Annotation>) annotation)) {
        return annotation.getSimpleName();
      }
    }
    throw new IllegalStateException("Couldn't find a HTTP annotation");
  }

  private static String path(final AnnotatedElement owner) {
    Path path = owner.getAnnotation(Path.class);
    return normalize(path != null ? path.value() : "/");
  }

  public static String normalize(final String path) {
    if ("/".equals(path) || path == null || path.length() == 0) {
      return "/";
    }
    StringBuilder normalized = new StringBuilder();
    if (!path.startsWith("/")) {
      normalized.append("/");
    }
    if (path.endsWith("/")) {
      normalized.append(path.substring(0, path.length() - 1));
    } else {
      normalized.append(path);
    }
    return normalized.toString();
  }

}
