package io.jooby.internal.openapi;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.Router;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.jooby.internal.openapi.RoutePath.path;

public class OperationParser {

  public List<Operation> parse(ExecutionContext ctx) {
    List<Operation> result = parse(ctx, null, ctx.classNode(ctx.getRouter()));

    // swagger/openapi:
    for (Operation operation : result) {
      OpenApiParser.parse(ctx, operation.getNode(), operation);
    }

    // Initialize schema types
    for (Operation operation : result) {
      List<io.swagger.v3.oas.models.parameters.Parameter> parameters = operation.getParameters();
      for (io.swagger.v3.oas.models.parameters.Parameter parameter : parameters) {
        String javaType = ((io.jooby.internal.openapi.Parameter) parameter).getJavaType();
        parameter.setSchema(ctx.schema(javaType));
      }
      for (Map.Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
        int statusCode = statusCode(entry.getKey().replace("default", "200"));
        Response response = (Response) entry.getValue();
        Schema defaultSchema = parseSchema(ctx, response);
        if (defaultSchema != null) {
          Content content = response.getContent();
          if (content == null) {
            content = new Content();
            response.setContent(content);
          }

          if (content.isEmpty()) {
            io.swagger.v3.oas.models.media.MediaType mediaTypeObject = new io.swagger.v3.oas.models.media.MediaType();
            String mediaType = operation.getProduces().stream()
                .findFirst()
                .orElse(MediaType.JSON);
            content.addMediaType(mediaType, mediaTypeObject);
          }
          if (statusCode > 0 && statusCode < 400) {
            for (io.swagger.v3.oas.models.media.MediaType mediaType : content.values()) {
              Schema schema = mediaType.getSchema();
              if (schema == null) {
                mediaType.setSchema(defaultSchema);
              }
            }
          }
        }
      }
    }

    // clean up empty param
    for (Operation operation : result) {
      if (operation.getParameters().isEmpty()) {
        operation.setParameters(null);
      }
    }
    return result;
  }

  private int statusCode(String code) {
    try {
      return Integer.parseInt(code);
    } catch (NumberFormatException x) {
      return -1;
    }
  }

  private Schema parseSchema(ExecutionContext ctx, Response response) {
    List<String> javaTypes = response.getJavaTypes();
    Schema schema;
    if (javaTypes.size() == 1) {
      schema = ctx.schema(javaTypes.get(0));
    } else if (javaTypes.size() > 1) {
      List<Schema> schemas = javaTypes.stream()
          .map(ctx::schema)
          .collect(Collectors.toList());
      schema = new ComposedSchema().oneOf(schemas);
    } else {
      schema = null;
    }
    return schema;
  }

  public List<Operation> parse(ExecutionContext ctx, String prefix, ClassNode node) {
    List<Operation> handlerList = new ArrayList<>();
    for (MethodNode method : node.methods) {
      handlerList.addAll(routeHandler(ctx, prefix, method));
    }
    return handlerList;
  }

  private List<Operation> routeHandler(ExecutionContext ctx, String prefix,
      MethodNode method) {
    List<Operation> handlerList = new ArrayList<>();
    /** Track the last router instruction and override with produces/consumes. */
    AbstractInsnNode instructionTo = null;
    for (AbstractInsnNode it : method.instructions) {
      if (it instanceof MethodInsnNode && ctx.process(it)) {
        MethodInsnNode node = (MethodInsnNode) it;
        Signature signature = Signature.create(node);
        if (ctx.isRouter(signature.getOwner().orElse(null))) {
          if (signature.matches("mvc")) {
            handlerList.addAll(AnnotationParser.parse(ctx, prefix, signature, (MethodInsnNode) it));
          } else if (signature.matches("<init>", TypeFactory.KT_FUN_1)) {
            handlerList.addAll(kotlinHandler(ctx, null, prefix, node));
          } else if (signature.matches("use", Router.class)) {
            handlerList.addAll(useRouter(ctx, prefix, node, findRouterInstruction(node)));
          } else if (signature.matches("use", String.class, Router.class)) {
            AbstractInsnNode routerInstruction = findRouterInstruction(node);
            String pattern = routePattern(node, node);
            handlerList.addAll(useRouter(ctx, path(prefix, pattern), node, routerInstruction));
          } else if (signature.matches("path", String.class, Runnable.class)) {
            //  router path (Ljava/lang/String;Ljava/lang/Runnable;)Lio/jooby/Route;
            if (node.owner.equals(TypeFactory.KOOBY.getInternalName())) {
              MethodInsnNode subrouteInsn = InsnSupport.prev(node)
                  .filter(MethodInsnNode.class::isInstance)
                  .findFirst()
                  .map(MethodInsnNode.class::cast)
                  .orElseThrow(() -> new IllegalStateException("Subroute definition not found"));
              String path = routePattern(node, subrouteInsn);
              handlerList.addAll(kotlinHandler(ctx, null, path(prefix, path), subrouteInsn));
            } else {
              InvokeDynamicInsnNode subrouteInsn = InsnSupport.prev(node)
                  .filter(InvokeDynamicInsnNode.class::isInstance)
                  .findFirst()
                  .map(InvokeDynamicInsnNode.class::cast)
                  .orElseThrow(() -> new IllegalStateException("Subroute definition not found"));
              String path = routePattern(node, subrouteInsn);
              MethodNode methodLink = findLambda(ctx, subrouteInsn);
              ctx.debugHandlerLink(methodLink);
              handlerList.addAll(routeHandler(ctx, path(prefix, path), methodLink));
            }
          } else if (Router.METHODS.contains(signature.getMethod().toUpperCase())
              && signature.matches(String.class, Route.Handler.class)) {
            instructionTo = node;
            //  router get (Ljava/lang/String;Lio/jooby/Route$Handler;)Lio/jooby/Route;
            AbstractInsnNode previous = node.getPrevious();
            String path = routePattern(node, previous);
            String httpMethod = signature.getMethod().toUpperCase();
            if (node.owner.equals(TypeFactory.KOOBY.getInternalName())) {
              handlerList.addAll(kotlinHandler(ctx, httpMethod, path(prefix, path), node));
            } else {
              if (previous instanceof InvokeDynamicInsnNode) {
                MethodNode handler = findLambda(ctx, (InvokeDynamicInsnNode) previous);
                ctx.debugHandler(handler);
                handlerList.add(
                    newRouteDescriptor(ctx, handler, httpMethod, path(prefix, path)));
              } else if (previous instanceof MethodInsnNode) {
                if (InsnSupport.opcode(Opcodes.INVOKESPECIAL).test(previous)) {
                  MethodInsnNode methodInsnNode = (MethodInsnNode) previous;
                  MethodNode handler = findRouteHandler(ctx, methodInsnNode);
                  ctx.debugHandler(handler);
                  handlerList.add(newRouteDescriptor(ctx, handler, httpMethod, path(prefix, path)));
                }
              } else if (previous instanceof VarInsnNode) {
                VarInsnNode varInsnNode = (VarInsnNode) previous;
                if (varInsnNode.getOpcode() == Opcodes.ALOAD) {
                  AbstractInsnNode astore = InsnSupport.prev(varInsnNode)
                      .filter(InsnSupport.varInsn(Opcodes.ASTORE, varInsnNode.var))
                      .findFirst()
                      .orElse(null);
                  if (astore != null) {
                    AbstractInsnNode varType = InsnSupport.prev(astore)
                        .filter(
                            e -> (e instanceof InvokeDynamicInsnNode
                                || e instanceof MethodInsnNode))
                        .findFirst()
                        .orElse(null);
                    if (varType instanceof MethodInsnNode) {
                      MethodNode handler = findRouteHandler(ctx, (MethodInsnNode) varType);
                      ctx.debugHandler(handler);
                      handlerList.add(
                          newRouteDescriptor(ctx, handler, httpMethod, path(prefix, path)));
                    } else if (varType instanceof InvokeDynamicInsnNode) {
                      MethodNode handler = findLambda(ctx, (InvokeDynamicInsnNode) varType);
                      ctx.debugHandler(handler);
                      handlerList.add(
                          newRouteDescriptor(ctx, handler, httpMethod, path(prefix, path)));
                    }
                  }
                }
              }
            }
          } else if (Router.METHODS.contains(signature.getMethod().toUpperCase())
              && signature.matches(TypeFactory.STRING, TypeFactory.KT_FUN_1)) {
            instructionTo = node;
            String path = routePattern(node, node.getPrevious());
            String httpMethod = signature.getMethod().toUpperCase();
            handlerList.addAll(kotlinHandler(ctx, httpMethod, path(prefix, path), node));
          } else if (Router.METHODS.contains(signature.getMethod().toUpperCase())
              && signature.matches(TypeFactory.STRING, TypeFactory.KT_FUN_2)) {
            instructionTo = node;
            String path = routePattern(node, node.getPrevious());
            String httpMethod = signature.getMethod().toUpperCase();
            handlerList.addAll(kotlinHandler(ctx, httpMethod, path(prefix, path), node));
          } else if (signature.getMethod().startsWith("coroutine")) {
            handlerList.addAll(kotlinHandler(ctx, null, prefix, node));
          }
        } else if (signature.matches(Route.class, "produces", MediaType[].class)) {
          if (instructionTo != null) {
            Operation route = handlerList.get(handlerList.size() - 1);
            InsnSupport.prev(it, instructionTo)
                .flatMap(mediaType())
                .forEach(route::addProduces);
            instructionTo = it;
          }
        } else if (signature.matches(Route.class, "consumes", MediaType[].class)) {
          if (instructionTo != null) {
            Operation route = handlerList.get(handlerList.size() - 1);
            InsnSupport.prev(it, instructionTo)
                .flatMap(mediaType())
                .forEach(route::addConsumes);
            instructionTo = it;
          }
        }
      }
    }
    return handlerList;
  }

  private static Function<AbstractInsnNode, Stream<String>> mediaType() {
    return e -> {
      if (e instanceof FieldInsnNode) {
        if (((FieldInsnNode) e).owner.equals("io/jooby/MediaType")) {
          return Stream.of(((FieldInsnNode) e).name);
        }
      } else if (e instanceof MethodInsnNode) {
        if (((MethodInsnNode) e).owner.equals("io/jooby/MediaType")
            && ((MethodInsnNode) e).name.equals("valueOf")) {
          return InsnSupport.prev(e)
              .filter(LdcInsnNode.class::isInstance)
              .findFirst()
              .map(i -> ((LdcInsnNode) i).cst.toString())
              .map(Stream::of)
              .orElse(Stream.of());
        }
      }
      return Stream.of();
    };
  }

  private AbstractInsnNode findRouterInstruction(MethodInsnNode node) {
    return InsnSupport.prev(node)
        .filter(e -> {
          if (e instanceof TypeInsnNode) {
            return e.getOpcode() != Opcodes.CHECKCAST;
          } else if (e instanceof LdcInsnNode) {
            return ((LdcInsnNode) e).cst instanceof Type;
          }
          return false;
        })
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Unsupported router type: " + InsnSupport.toString(node)));
  }

  private List<Operation> useRouter(ExecutionContext ctx, String prefix,
      MethodInsnNode node, AbstractInsnNode routerInstruction) {
    Type router;
    if (routerInstruction instanceof TypeInsnNode) {
      router = Type.getObjectType(((TypeInsnNode) routerInstruction).desc);
    } else if (routerInstruction instanceof LdcInsnNode) {
      router = (Type) ((LdcInsnNode) routerInstruction).cst;
    } else {
      throw new UnsupportedOperationException(InsnSupport.toString(node));
    }
    ClassNode classNode = ctx.classNode(router);
    return parse(ctx.newContext(router), prefix, classNode);
  }

  private List<Operation> kotlinHandler(ExecutionContext ctx, String httpMethod,
      String prefix,
      MethodInsnNode node) {
    List<Operation> handlerList = new ArrayList<>();
    String owner = InsnSupport.prev(node.getPrevious())
        .filter(it -> it instanceof FieldInsnNode || it instanceof MethodInsnNode)
        .findFirst()
        .map(e -> {
          if (e instanceof FieldInsnNode) {
            return ((FieldInsnNode) e).owner;
          }
          return ((MethodInsnNode) e).owner;
        })
        .orElseThrow(() -> new IllegalStateException(
            "Kotlin lambda not found: " + InsnSupport.toString(node)));

    ClassNode classNode = ctx.classNode(Type.getObjectType(owner));
    MethodNode apply = null;
    for (MethodNode method : classNode.methods) {
      Signature signature = Signature.create(method);
      if (signature.matches("invoke", TypeFactory.KOOBY)) {
        ctx.debugHandlerLink(method);
        handlerList.addAll(routeHandler(ctx, prefix, method));
      } else if (signature.matches("invoke", TypeFactory.COROUTINE_ROUTER)) {
        ctx.debugHandlerLink(method);
        handlerList.addAll(routeHandler(ctx, prefix, method));
      } else if (signature.matches("invoke", TypeFactory.HANDLER_CONTEXT)) {
        ctx.debugHandler(method);
        handlerList.add(newRouteDescriptor(ctx, method, httpMethod, prefix));
      } else if (signature.matches("invokeSuspend", Object.class)) {
        ctx.debugHandler(method);
        handlerList.add(newRouteDescriptor(ctx, method, httpMethod, prefix));
      } else if (signature.matches("apply", TypeFactory.CONTEXT)) {
        if (apply == null) {
          apply = method;
        } else {
          // Should be a more specific apply method
          if (returnTypePrecedence(method) > returnTypePrecedence(apply)) {
            apply = method;
          }
        }
      } else if (signature.matches("run")) {
        ctx.debugHandlerLink(method);
        handlerList.addAll(routeHandler(ctx, prefix, method));
      }
    }
    if (apply != null) {
      ctx.debugHandler(apply);
      handlerList.add(newRouteDescriptor(ctx, apply, httpMethod, prefix));
    }
    return handlerList;
  }

  private Operation newRouteDescriptor(ExecutionContext ctx, MethodNode node,
      String httpMethod, String prefix) {
    List<Parameter> arguments = ParameterParser.parse(ctx, node);
    Response response = new Response();
    List<String> returnTypes = ResponseParser.parse(ctx, node);
    response.setJavaTypes(returnTypes);
    Operation operation = new Operation(node, httpMethod, prefix, arguments,
        Collections.singletonList(response));

    boolean notSynthetic = (node.access & Opcodes.ACC_SYNTHETIC) == 0;
    if (notSynthetic) {
      operation.setOperationId(node.name);
    }
    return operation;
  }

  private int returnTypePrecedence(MethodNode method) {
    return Type.getReturnType(method.desc).getClassName().equals("java.lang.Object") ? 0 : 1;
  }

  /**
   * IMPORTANT: First lcdInsn must be the pattern. We don't support variable as pattern. Only
   * string literal or final variables.
   *
   * @param methodInsnNode
   * @param node
   * @return
   */
  private String routePattern(MethodInsnNode methodInsnNode, AbstractInsnNode node) {
    return InsnSupport.prev(node)
        .filter(it -> it instanceof LdcInsnNode && (((LdcInsnNode) it).cst instanceof String))
        .findFirst()
        .map(it -> ((LdcInsnNode) it).cst.toString())
        .orElseThrow(() -> new IllegalStateException(
            "Route pattern not found: " + InsnSupport.toString(methodInsnNode)));
  }

  private MethodNode findRouteHandler(ExecutionContext ctx, MethodInsnNode node) {
    Type owner = TypeFactory.fromInternalName(node.owner);
    return ctx.classNode(owner).methods.stream()
        .filter(m -> {
          Signature signature = new Signature(owner, "apply", m.desc);
          return Modifier.isPublic(m.access) && signature.matches(Context.class);
        })
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Handler not found: " + InsnSupport.toString(node)));
  }

  private MethodNode findLambda(ExecutionContext ctx, InvokeDynamicInsnNode node) {
    Handle handle = (Handle) node.bsmArgs[1];
    Type owner = TypeFactory.fromInternalName(handle.getOwner());
    try {
      return ctx.classNode(owner).methods.stream()
          .filter(n -> n.name.equals(handle.getName()))
          .findFirst()
          .orElseThrow(() ->
              new IllegalStateException("Handler not found: " + InsnSupport.toString(node))
          );
    } catch (Exception x) {
      // Fake a method node, required when we write things like:
      /// get("/", Context::getRequestPath);
      // method reference to a class outside application classpath.
      // Faked method has a return instruction for return type parser (no arguments).
      String suffix = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
      MethodNode method = new MethodNode(Opcodes.ACC_PRIVATE & Opcodes.ACC_SYNTHETIC,
          "fake$" + handle.getName() + "$" + suffix, handle.getDesc(), null, null);
      method.instructions = new InsnList();
      method.instructions.add(
          new MethodInsnNode(Opcodes.INVOKEVIRTUAL, handle.getOwner(), handle.getName(),
              handle.getDesc()));
      method.instructions.add(new InsnNode(Opcodes.ARETURN));
      return method;
    }
  }
}
