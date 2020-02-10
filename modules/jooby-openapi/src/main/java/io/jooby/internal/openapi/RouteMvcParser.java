package io.jooby.internal.openapi;

import io.jooby.Context;
import io.jooby.Router;
import io.jooby.Session;
import io.jooby.annotations.ContextParam;
import io.jooby.annotations.CookieParam;
import io.jooby.annotations.FormParam;
import io.jooby.annotations.GET;
import io.jooby.annotations.HeaderParam;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;
import io.jooby.annotations.QueryParam;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.jooby.internal.openapi.TypeFactory.KT_FUN_0;
import static io.jooby.internal.openapi.TypeFactory.KT_KLASS;

public class RouteMvcParser {
  static final String PACKAGE = GET.class.getPackage().getName();

  static final Set<String> IGNORED_PARAM_TYPE = Arrays.asList(
      Context.class.getName(),
      Session.class.getName(),
      "java.util.Optional<" + Session.class.getName() + ">",
      "kotlin.coroutines.Continuation"
  ).stream().collect(Collectors.toSet());

  public static List<RouteDescriptor> parse(ExecutionContext ctx, String prefix,
      Signature signature, MethodInsnNode node) {
    if (signature.matches(Class.class) ||
        signature.matches(Class.class, Provider.class)
        || signature.matches(KT_KLASS)
        || signature.matches(KT_KLASS, KT_FUN_0)) {
      Type type = InsnSupport.prev(node)
          .filter(e -> e instanceof LdcInsnNode && ((LdcInsnNode) e).cst instanceof Type)
          .findFirst()
          .map(e -> (Type) ((LdcInsnNode) e).cst)
          .orElseThrow(() -> new IllegalStateException(
              "Mvc class not found: " + InsnSupport.toString(node)));
      return parse(ctx, prefix, type);
    }
    return Collections.emptyList();
  }

  private static List<RouteDescriptor> parse(ExecutionContext ctx, String prefix, Type type) {
    List<RouteDescriptor> result = new ArrayList<>();
    ClassNode classNode = ctx.classNode(type);
    for (MethodNode method : classNode.methods) {
      if (isRouter(method)) {
        ctx.debugHandler(method);
        result.addAll(routerMethod(ctx, prefix, classNode, method));
      }
    }
    return result;
  }

  private static List<RouteDescriptor> routerMethod(ExecutionContext ctx, String prefix,
      ClassNode classNode, MethodNode method) {
    List<RouteDescriptor> result = new ArrayList<>();

    List<RouteArgument> arguments = routerArguments(method);
    List<RouteReturnType> returnTypes = returnTypes(method);

    for (String httpMethod : httpMethod(method.visibleAnnotations)) {
      for (String pattern : httpPattern(classNode, method, httpMethod)) {
        result.add(new RouteDescriptor(httpMethod, RoutePath.path(prefix, pattern), arguments,
            returnTypes));
      }
    }

    return result;
  }

  private static List<RouteReturnType> returnTypes(MethodNode method) {
    List<RouteReturnType> result = new ArrayList<>();
    Signature signature = Signature.create(method);
    String desc = Optional.ofNullable(method.signature).orElse(method.desc);
    String continuationType = "Lkotlin/coroutines/Continuation;";
    if (signature.matches(Type.getType(continuationType)) && method.signature != null) {
      desc = method.signature.substring(1, method.signature.indexOf(')'));
      // Lkotlin/coroutines/Continuation<-Ljava/lang/String;>;
      desc = desc.substring(continuationType.length() + 1, desc.length() - 2);
    }
    int i = desc.indexOf(')');
    if (i > 0) {
      desc = desc.substring(i + 1);
    }
    RouteReturnType rrt = new RouteReturnType(ASMType.parse(desc));
    result.add(rrt);
    return result;
  }

  private static List<RouteArgument> routerArguments(MethodNode method) {
    List<RouteArgument> result = new ArrayList<>();
    if (method.parameters != null) {
      for (int i = 0; i < method.parameters.size(); i++) {
        ParameterNode parameter = method.parameters.get(i);

        List<String> parameterNames;
        if (parameter.name.equals("continuation") && i == method.parameters.size() - 1) {
          parameterNames = Arrays.asList(parameter.name, "$" + parameter.name);
        } else {
          parameterNames = Collections.singletonList(parameter.name);
        }
        /** Java Type: */
        LocalVariableNode variable = method.localVariables.stream()
            .filter(var -> parameterNames.contains(var.name))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Parameter type not found on method: " + method.name + ", parameter: "
                    + parameter.name));
        String javaType = variable.signature == null
            ? ASMType.parse(variable.desc)
            : ASMType.parse(variable.signature);

        if (IGNORED_PARAM_TYPE.contains(javaType)) {
          continue;
        }

        /** HTTP Type: */
        HttpType httpType = HttpType.BODY;
        if (method.visibleParameterAnnotations != null
            && i < method.visibleParameterAnnotations.length) {
          List<AnnotationNode> annotations = method.visibleParameterAnnotations[i];
          httpType = findAnnotationByType(annotations, httpType()).stream().findFirst()
              .map(RouteMvcParser::paramAnnotationToHttpType)
              .orElse(null);
        }

        /** Required: */
        boolean required = isPrimitive(javaType)
            ? true
            : !isNullable(method, i);//!javaType.startsWith("java.util.Optional");

        RouteArgument argument = new RouteArgument();
        argument.setName(parameter.name);
        argument.setJavaType(javaType);
        argument.setHttpType(httpType);
        argument.setRequired(required);

        result.add(argument);
      }
    }
    return result;
  }

  private static boolean isNullable(MethodNode method, int paramIndex) {
    if (paramIndex < method.invisibleAnnotableParameterCount) {
      List<AnnotationNode> annotations = method.invisibleParameterAnnotations[paramIndex];
      return annotations.stream()
          .anyMatch(a -> a.desc.equals("Lorg/jetbrains/annotations/Nullable;"));
    }
    return true;
  }

  private static boolean isPrimitive(String javaType) {
    switch (javaType) {
      case "boolean":
      case "byte":
      case "char":
      case "short":
      case "int":
      case "float":
      case "double":
      case "long":
        return true;
    }
    return false;
  }

  private static HttpType paramAnnotationToHttpType(AnnotationNode node) {
    String classname = Type.getType(node.desc).getClassName();
    if (classname.equals(QueryParam.class.getName())
        || classname.equals(javax.ws.rs.QueryParam.class.getName())) {
      return HttpType.QUERY;
    }
    if (classname.equals(FormParam.class.getName())
        || classname.equals(javax.ws.rs.FormParam.class.getName())) {
      return HttpType.FORM;
    }
    if (classname.equals(HeaderParam.class.getName())
        || classname.equals(javax.ws.rs.HeaderParam.class.getName())) {
      return HttpType.HEADER;
    }
    if (classname.equals(PathParam.class.getName())
        || classname.equals(javax.ws.rs.PathParam.class.getName())) {
      return HttpType.PATH;
    }
    if (classname.equals(CookieParam.class.getName())
        || classname.equals(javax.ws.rs.CookieParam.class.getName())) {
      return HttpType.COOKIE;
    }
    if (classname.equals(ContextParam.class.getName())) {
      return HttpType.CONTEXT;
    }
    return HttpType.BODY;
  }

  private static List<String> httpPattern(ClassNode classNode, MethodNode method,
      String httpMethod) {
    List<String> patterns = new ArrayList<>();

    List<String> rootPattern = httpPattern(httpMethod, null, classNode.visibleAnnotations);
    if (rootPattern.size() == 0) {
      rootPattern = Arrays.asList("/");
    }

    for (String prefix : rootPattern) {
      patterns.addAll(httpPattern(httpMethod, prefix, method.visibleAnnotations));
    }

    if (patterns.size() == 0) {
      patterns.add("/");
    }

    return patterns;
  }

  private static List<String> httpPattern(String httpMethod, String prefix,
      List<AnnotationNode> annotations) {
    List<String> patterns = new ArrayList<>();
    if (annotations != null) {

      List<Map<String, Object>> values = findAnnotationByType(annotations,
          Arrays.asList(PACKAGE + "." + httpMethod)).stream()
          .flatMap(annotation -> Stream.of(annotation.values)
              .filter(Objects::nonNull)
              .map(RouteMvcParser::arrayToMap)
          )
          .collect(Collectors.toList());

      if (values.isEmpty()) {
        values = findAnnotationByType(annotations, Arrays.asList(Path.class.getName())).stream()
            .flatMap(annotation -> Stream.of(annotation.values)
                .filter(Objects::nonNull)
                .map(RouteMvcParser::arrayToMap)
            )
            .collect(Collectors.toList());

        if (values.isEmpty()) {
          values = findAnnotationByType(annotations,
              Arrays.asList(javax.ws.rs.Path.class.getName())).stream()
              .flatMap(annotation -> Stream.of(annotation.values)
                  .filter(Objects::nonNull)
                  .map(RouteMvcParser::arrayToMap)
              )
              .collect(Collectors.toList());
        }
      }

      for (Map<String, Object> map : values) {
        Object value = map.getOrDefault("value", Collections.emptyList());
        if (!(value instanceof Collection)) {
          value = Arrays.asList(value);
        }
        ((List) value)
            .forEach(v -> patterns.add(RoutePath.path(prefix, v.toString())));
      }
    }
    if (prefix != null && patterns.isEmpty()) {
      patterns.add(RoutePath.path(prefix, null));
    }
    return patterns;
  }

  private static Map<String, Object> arrayToMap(Object o) {
    Map<String, Object> map = new LinkedHashMap<>();
    List values = (List) o;
    for (int i = 0; i < values.size(); i += 2) {
      String k = (String) values.get(i);
      Object v = values.get(i + 1);
      map.put(k, v);
    }
    return map;
  }

  private static List<AnnotationNode> findAnnotationByType(List<AnnotationNode> source,
      List<String> types) {
    return source.stream()
        .filter(n -> types.stream().anyMatch(t -> t.equals(Type.getType(n.desc).getClassName())))
        .collect(Collectors.toList());
  }

  private static List<String> httpMethod(List<AnnotationNode> annotations) {
    List<String> methods = findAnnotationByType(annotations, httpMethods()).stream()
        .map(n -> Type.getType(n.desc).getClassName())
        .map(name -> {
          String[] names = name.split("\\.");
          return names[names.length - 1];
        })
        .distinct()
        .collect(Collectors.toList());
    if (methods.size() == 1 && methods.contains("Path")) {
      return Arrays.asList(Router.GET);
    }
    methods.remove("Path");
    return methods;
  }

  private static boolean isRouter(MethodNode node) {
    if (node.visibleAnnotations != null) {
      // Jooby annotations
      List<String> annotationTypes = httpMethods().stream()
          .map(classname -> Type.getObjectType(classname.replace(".", "/")).getDescriptor())
          .collect(Collectors.toList());

      return node.visibleAnnotations.stream()
          .anyMatch(a -> annotationTypes.contains(a.desc));
    }
    return false;
  }

  private static List<String> httpMethods() {
    List<String> annotationTypes = httpMethod(GET.class.getPackage().getName(), Path.class);

    // JAXRS annotations
    annotationTypes
        .addAll(httpMethod(javax.ws.rs.GET.class.getPackage().getName(), javax.ws.rs.Path.class));
    return annotationTypes;
  }

  private static List<String> httpType() {
    return Stream.of(
        QueryParam.class, FormParam.class, PathParam.class, CookieParam.class, HeaderParam.class,
        // jaxrs
        javax.ws.rs.QueryParam.class, javax.ws.rs.FormParam.class, javax.ws.rs.PathParam.class,
        javax.ws.rs.CookieParam.class, javax.ws.rs.HeaderParam.class
    )
        .map(t -> t.getName())
        .collect(Collectors.toList());
  }

  private static List<String> httpMethod(String pkg, Class pathType) {
    List<String> annotationTypes = Router.METHODS.stream()
        .map(m -> pkg + "." + m)
        .collect(Collectors.toList());
    if (pathType != null) {
      annotationTypes.add(Type.getType(pathType).getClassName());
    }
    return annotationTypes;
  }

}
