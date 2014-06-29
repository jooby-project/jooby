package jooby.internal;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jooby.Consumes;
import jooby.DELETE;
import jooby.GET;
import jooby.JoobyModule;
import jooby.MediaType;
import jooby.Mode;
import jooby.POST;
import jooby.PUT;
import jooby.Path;
import jooby.Produces;
import jooby.Reflection;
import jooby.Route;
import jooby.RouteDefinition;
import net.sf.cglib.reflect.FastClass;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

public class Routes implements JoobyModule {

  private Set<Class<?>> routes;

  public Routes(final Set<Class<?>> routes) {
    this.routes = requireNonNull(routes, "The routes are required.");
  }

  @Override
  public void configure(final Mode mode, final Config config, final Binder binder)
      throws Exception {
    Boolean reloadParams = mode.on("dev", true)
        .get(false);
    ParamResolver paramResolver = new DefaultParamResolver(reloadParams);

    Set<RouteDefinition> routeDefinitions = new LinkedHashSet<>();
    for (Class<?> route : routes) {
      FastClass fastRoute = FastClass.create(route);
      String rootPath = path(route);
      routeDefinitions.addAll(Reflection
          .methods(route)
          .stream()
          .filter(m -> {
            Set<Annotation> annotations = Reflection.Annotations.anyOf(m, GET.class, POST.class,
                PUT.class, DELETE.class);
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
                String httpMethod = httpMethod(m);
                String path = rootPath + path(m);
                checkArgument(path.length() > 0, "Missing path for: %s.%s", route.getSimpleName(),
                    m.getName());
                Route resource = new MvcRoute(fastRoute.getMethod(m), paramResolver);
                return new RouteDefinition(httpMethod, path, resource)
                    .produces(produces(m))
                    .consumes(consumes(m));
              })
          .collect(Collectors.toSet()));
    }
    Multibinder<RouteDefinition> bindings = Multibinder.newSetBinder(binder, RouteDefinition.class);
    routeDefinitions.forEach(route -> bindings.addBinding().toInstance(route));
  }

  private List<MediaType> produces(final Method method) {
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

  private List<MediaType> consumes(final Method method) {
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
  private static String httpMethod(final Method method) {
    Class<?>[] annotations = {GET.class };
    for (Class<?> annotation : annotations) {
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
