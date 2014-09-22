package jooby.internal.mvc;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jooby.MediaType;
import jooby.Mode;
import jooby.RouteDefinition;
import jooby.Router;
import jooby.internal.Reflection;
import jooby.internal.RouteDefinitionImpl;
import jooby.mvc.Consumes;
import jooby.mvc.DELETE;
import jooby.mvc.GET;
import jooby.mvc.POST;
import jooby.mvc.PUT;
import jooby.mvc.Path;
import jooby.mvc.Produces;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class Routes {

  private static final List<MediaType> ALL = ImmutableList.of(MediaType.all);

  private static final Set<Class<? extends Annotation>> VERBS = ImmutableSet.of(GET.class,
      POST.class, PUT.class, DELETE.class);

  @SuppressWarnings({"unchecked", "rawtypes" })
  public static List<RouteDefinition> route(final Mode mode, final Class<?> routeClass) {
    ParamProvider base = new ParamProviderImpl(ParamNameProvider.HEAD);
    if (!mode.name().equals("dev")) {
      base = new CachedParamProvider(base);
    }
    ParamProvider provider = base;


    String topLevelPath = path(routeClass);
    String rootPath = "/".equals(topLevelPath) ? "" : topLevelPath;

    return Reflection
        .methods(routeClass)
        .stream()
        .filter(
            m -> {
              List<Annotation> annotations = new ArrayList<>();
              for (Class annotationType : VERBS) {
                Annotation annotation = m.getAnnotation(annotationType);
                if (annotation != null) {
                  annotations.add(annotation);
                }
              }
              if (annotations.size() == 0) {
                return false;
              }
              if (annotations.size() > 1) {
                throw new IllegalStateException("A resource method: " + m
                    + " should have only one HTTP verb. Found: "
                    + annotations);
              }
              return true;
            })
        .map(
            m -> {
              String verb = verb(m);
              String path = rootPath + path(m);
              checkArgument(path.length() > 0, "Missing path for: %s.%s",
                  routeClass.getSimpleName(),
                  m.getName());
              Router resource = new MvcRoute(m, provider);
              return new RouteDefinitionImpl(verb, path, resource)
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
            .orElse(ALL));
  }

  private static List<MediaType> consumes(final Method method) {
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
            .orElse(ALL));
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
    Path annotation = owner.getAnnotation(Path.class);
    if (annotation == null) {
      return "";
    }
    String path = annotation.value();
    return normalize(path);
  }

  public static String normalize(final String candidate) {
    String path = candidate.trim();
    if ("/".equals(path) || path.length() == 0) {
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
