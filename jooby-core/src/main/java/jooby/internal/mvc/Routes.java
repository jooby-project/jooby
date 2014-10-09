package jooby.internal.mvc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jooby.MediaType;
import jooby.Mode;
import jooby.Route;
import jooby.Route.Definition;
import jooby.mvc.Consumes;
import jooby.mvc.DELETE;
import jooby.mvc.GET;
import jooby.mvc.HEAD;
import jooby.mvc.OPTIONS;
import jooby.mvc.POST;
import jooby.mvc.PUT;
import jooby.mvc.Path;
import jooby.mvc.Produces;
import jooby.mvc.TRACE;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class Routes {

  private static final String[] NO_ARG = new String[0];

  private static final List<MediaType> ALL = ImmutableList.of(MediaType.all);

  @SuppressWarnings("unchecked")
  private static final Set<Class<? extends Annotation>> VERBS = ImmutableSet.of(GET.class,
      POST.class, PUT.class, DELETE.class, HEAD.class, OPTIONS.class, TRACE.class);

  @SuppressWarnings({"unchecked", "rawtypes" })
  public static List<Route.Definition> routes(final Mode mode, final Class<?> routeClass) {
    Map<String, String[]> params = new HashMap<>();
    Map<String, Integer> lines = new HashMap<>();

    collectParameterNamesAndLineNumbers(routeClass, params, lines);

    ParamProvider provider =
        new ParamProviderImpl(
            new ChainParamNameProvider(
                ParamNameProvider.NAMED,
                ParamNameProvider.JAVA_8,
                new ASMParamNameProvider(params)
            ));

    String rootPath = path(routeClass);

    Map<Method, List<Class<?>>> methods = new HashMap<>();
    for (Method method : routeClass.getDeclaredMethods()) {
      List<Class<?>> annotations = new ArrayList<>();
      for (Class annotationType : VERBS) {
        Annotation annotation = method.getAnnotation(annotationType);
        if (annotation != null) {
          annotations.add(annotationType);
        }
      }
      if (annotations.size() > 0) {
        if (!Modifier.isPublic(method.getModifiers())) {
          throw new IllegalArgumentException("Not a public method: " + method);
        }
        methods.put(method, annotations);
      }
    }

    List<Definition> definitions = new ArrayList<>();

    methods
        .keySet()
        .stream()
        .sorted((m1, m2) -> {
          String k1 = key(m1);
          String k2 = key(m2);
          int l1 = lines.getOrDefault(k1, 0);
          int l2 = lines.getOrDefault(k2, 0);
          return l1 - l2;
        })
        .forEach(
            method -> {
              List<Class<?>> verbs = methods.get(method);
              String path = rootPath + "/" + path(method);
              String name = routeClass.getSimpleName() + "." + method.getName();
              for (Class<?> verb : verbs) {
                Definition definition = new Route.Definition(
                    verb.getSimpleName(), path, new MvcRoute(method, provider))
                    .produces(produces(method))
                    .consumes(consumes(method))
                    .name(name);

                definitions.add(definition);
              }
            });

    return definitions;
  }

  private static String key(final Method method) {
    return method.getName() + Type.getMethodDescriptor(method);
  }

  private static void collectParameterNamesAndLineNumbers(final Class<?> routeClass,
      final Map<String, String[]> params, final Map<String, Integer> lines) {
    try {
      new ClassReader(routeClass.getName()).accept(visitor(routeClass, params, lines), 0);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to read class: " + routeClass.getName(), ex);
    }
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

  private static String path(final AnnotatedElement owner) {
    Path annotation = owner.getAnnotation(Path.class);
    if (annotation == null) {
      return "";
    }
    return annotation.value();
  }

  private static ClassVisitor visitor(final Class<?> clazz, final Map<String, String[]> params,
      final Map<String, Integer> lines) {
    return new ClassVisitor(Opcodes.ASM5) {

      @Override
      public MethodVisitor visitMethod(final int access, final String name,
          final String desc, final String signature, final String[] exceptions) {
        boolean isPublic = ((access & Opcodes.ACC_PUBLIC) > 0) ? true : false;
        String key = name + desc;
        if (!isPublic) {
          // ignore
          return null;
        }
        Type[] args = Type.getArgumentTypes(desc);
        String[] names = args.length == 0 ? NO_ARG : new String[args.length];
        params.put(key, names);

        int minIdx = ((access & Opcodes.ACC_STATIC) > 0) ? 0 : 1;
        int maxIdx = Arrays.stream(args).mapToInt(Type::getSize).sum();

        return new MethodVisitor(Opcodes.ASM5) {

          private int i = 0;

          private boolean skipLocalTable = false;

          @Override
          public void visitParameter(final String name, final int access) {
            skipLocalTable = true;
            // save current parameter
            names[i] = name;
            // move to next
            i += 1;
          }

          @Override
          public void visitLineNumber(final int line, final Label start) {
            // save line number
            lines.putIfAbsent(key, line);
          }

          @Override
          public void visitLocalVariable(final String name, final String desc,
              final String signature,
              final Label start, final Label end, final int index) {
            if (!skipLocalTable) {
              if (index >= minIdx && index <= maxIdx) {
                // save current parameter
                names[i] = name;
                // move to next
                i += 1;
              }
            }
          }

        };
      }

    };
  }
}
