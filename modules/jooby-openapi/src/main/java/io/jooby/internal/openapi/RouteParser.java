package io.jooby.internal.openapi;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RouteParser {

  public List<RouteDescriptor> routes(ExecutionContext ctx) {
    return routes(ctx, null, ctx.classNode(ctx.getRouter()));
  }

  public List<RouteDescriptor> routes(ExecutionContext ctx, String prefix, ClassNode node) {
    List<RouteDescriptor> handlerList = new ArrayList<>();
    for (MethodNode method : node.methods) {
      handlerList.addAll(routeHandler(ctx, prefix, method));
    }
    return handlerList;
  }

  private List<RouteDescriptor> routeHandler(ExecutionContext ctx, String prefix,
      MethodNode method) {
    List<RouteDescriptor> handlerList = new ArrayList<>();
    for (AbstractInsnNode it : method.instructions) {
      if (it instanceof MethodInsnNode && ctx.process(it)) {
        MethodInsnNode node = (MethodInsnNode) it;
        Signature signature = Signature.create(node);
        if (!ctx.isRouter(signature.getOwner().orElse(null))) {
          // Not a router method
          continue;
        }
        if (signature.matches("<init>", TypeFactory.KT_FUN_1)) {
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
                          e -> (e instanceof InvokeDynamicInsnNode || e instanceof MethodInsnNode))
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
          String path = routePattern(node, node.getPrevious());
          String httpMethod = signature.getMethod().toUpperCase();
          handlerList.addAll(kotlinHandler(ctx, httpMethod, path(prefix, path), node));
        } else if (Router.METHODS.contains(signature.getMethod().toUpperCase())
            && signature.matches(TypeFactory.STRING, TypeFactory.KT_FUN_2)) {
          String path = routePattern(node, node.getPrevious());
          String httpMethod = signature.getMethod().toUpperCase();
          handlerList.addAll(kotlinHandler(ctx, httpMethod, path(prefix, path), node));
        } else if (signature.getMethod().startsWith("coroutine")) {
          handlerList.addAll(kotlinHandler(ctx, null, prefix, node));
        }
      }
    }
    return handlerList;
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

  private List<RouteDescriptor> useRouter(ExecutionContext ctx, String prefix,
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
      return routes(ctx.newContext(router), prefix, classNode);
  }

  private List<RouteDescriptor> kotlinHandler(ExecutionContext ctx, String httpMethod,
      String prefix,
      MethodInsnNode node) {
    List<RouteDescriptor> handlerList = new ArrayList<>();
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

  private RouteDescriptor newRouteDescriptor(ExecutionContext ctx, MethodNode node,
      String httpMethod, String prefix) {
    List<RouteArgument> arguments = RouteArgumentParser.parse(ctx, node);
    List<RouteReturnType> returnTypes = RouteReturnTypeParser.parse(ctx, node).stream()
        .map(RouteReturnType::new)
        .collect(Collectors.toList());
    return new RouteDescriptor(httpMethod, prefix, arguments, returnTypes);
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
        .filter(it -> it instanceof LdcInsnNode && (((LdcInsnNode)it).cst instanceof String))
        .findFirst()
        .map(it -> ((LdcInsnNode) it).cst.toString())
        .orElseThrow(() -> new IllegalStateException(
            "Route pattern not found: " + InsnSupport.toString(methodInsnNode)));
  }

  private String path(String prefix, String path) {
    String s1 = Router.leadingSlash(prefix);
    String s2 = Router.leadingSlash(path);
    if (s1.equals("/")) {
      return s2;
    }
    return s2.equals("/") ? s1 : s1 + s2;
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
