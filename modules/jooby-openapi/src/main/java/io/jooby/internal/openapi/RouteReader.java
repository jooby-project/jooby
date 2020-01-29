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
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class RouteReader {

  public List<RouteDescriptor> routes(ExecutionContext ctx) {
    return routeHandler(ctx, null);
  }

  private List<RouteDescriptor> routeHandler(ExecutionContext ctx, String prefix) {
    List<RouteDescriptor> handlerList = new ArrayList<>();
    for (MethodNode method : ctx.root.methods) {
      handlerList.addAll(routeHandler(ctx, prefix, method));
    }
    return handlerList;
  }

  private List<RouteDescriptor> routeHandler(ExecutionContext ctx, String prefix,
      MethodNode method) {
    List<RouteDescriptor> handlerList = new ArrayList<>();
    for (AbstractInsnNode it : method.instructions) {
      if (ctx.process(it) && it instanceof MethodInsnNode) {
        MethodInsnNode node = (MethodInsnNode) it;
        Signature signature = Signature.create(node);
        if (!ctx.isRouter(signature.getOwner().orElse(null))) {
          // Not a router method
          continue;
        }
        if (signature.matches("<init>", TypeFactory.KT_FUN_1)) {
          handlerList.addAll(kotlinHandler(ctx, prefix, node));
        } else if (signature.matches("path", String.class, Runnable.class)) {
          //  router path (Ljava/lang/String;Ljava/lang/Runnable;)Lio/jooby/Route;
          if (node.owner.equals(TypeFactory.KOOBY.getInternalName())) {
            MethodInsnNode subrouteInsn = InsnNode.prev(node)
                .filter(MethodInsnNode.class::isInstance)
                .findFirst()
                .map(MethodInsnNode.class::cast)
                .orElseThrow(() -> new IllegalStateException("Subroute definition not found"));
            String path = routePattern(node, subrouteInsn);
            handlerList.addAll(kotlinHandler(ctx, path(prefix, path), subrouteInsn));
          } else {
            InvokeDynamicInsnNode subrouteInsn = InsnNode.prev(node)
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
          if (node.owner.equals(TypeFactory.KOOBY.getInternalName())) {
            handlerList.addAll(kotlinHandler(ctx, path(prefix, path), node));
          } else {
            if (previous instanceof InvokeDynamicInsnNode) {
              MethodNode handler = findLambda(ctx, (InvokeDynamicInsnNode) previous);
              ctx.debugHandler(handler);
              handlerList.add(new RouteDescriptor(path(prefix, path), handler));
            } else if (previous instanceof MethodInsnNode) {
              if (InsnNode.opcode(Opcodes.INVOKESPECIAL).test(previous)) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) previous;
                MethodNode handler = findRouteHandler(ctx, methodInsnNode);
                ctx.debugHandler(handler);
                handlerList.add(new RouteDescriptor(path(prefix, path), handler));
              }
            } else if (previous instanceof VarInsnNode) {
              VarInsnNode varInsnNode = (VarInsnNode) previous;
              if (varInsnNode.getOpcode() == Opcodes.ALOAD) {
                AbstractInsnNode astore = InsnNode.prev(varInsnNode)
                    .filter(InsnNode.varInsn(Opcodes.ASTORE, varInsnNode.var))
                    .findFirst()
                    .orElse(null);
                if (astore != null) {
                  AbstractInsnNode varType = InsnNode.prev(astore)
                      .filter(
                          e -> (e instanceof InvokeDynamicInsnNode || e instanceof MethodInsnNode))
                      .findFirst()
                      .orElse(null);
                  if (varType instanceof MethodInsnNode) {
                    MethodNode handler = findRouteHandler(ctx, (MethodInsnNode) varType);
                    ctx.debugHandler(handler);
                    handlerList.add(new RouteDescriptor(path(prefix, path), handler));
                  } else if (varType instanceof InvokeDynamicInsnNode) {
                    MethodNode handler = findLambda(ctx, (InvokeDynamicInsnNode) varType);
                    ctx.debugHandler(handler);
                    handlerList.add(new RouteDescriptor(path(prefix, path), handler));
                  }
                }
              }
            }
          }
        } else if (Router.METHODS.contains(signature.getMethod().toUpperCase())
            && signature.matches(TypeFactory.STRING, TypeFactory.KT_FUN_1)) {
          String path = routePattern(node, node.getPrevious());
          handlerList.addAll(kotlinHandler(ctx, path(prefix, path), node));
        } else if (Router.METHODS.contains(signature.getMethod().toUpperCase())
            && signature.matches(TypeFactory.STRING, TypeFactory.KT_FUN_2)) {
          String path = routePattern(node, node.getPrevious());
          handlerList.addAll(kotlinHandler(ctx, path(prefix, path), node));
        } else if (signature.getMethod().startsWith("coroutine")) {
          handlerList.addAll(kotlinHandler(ctx, prefix, node));
        }
      }
    }
    return handlerList;
  }

  private List<RouteDescriptor> kotlinHandler(ExecutionContext ctx, String prefix,
      MethodInsnNode node) {
    List<RouteDescriptor> handlerList = new ArrayList<>();
    String owner = InsnNode.prev(node.getPrevious())
        .filter(it -> it instanceof FieldInsnNode || it instanceof MethodInsnNode)
        .findFirst()
        .map(e -> {
          if (e instanceof FieldInsnNode) {
            return ((FieldInsnNode) e).owner;
          }
          return ((MethodInsnNode) e).owner;
        })
        .orElseThrow(() -> new IllegalStateException(
            "Kotlin lambda not found: " + InsnNode.toString(node)));

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
        handlerList.add(new RouteDescriptor(prefix, method));
      } else if (signature.matches("invokeSuspend", Object.class)) {
        ctx.debugHandler(method);
        handlerList.add(new RouteDescriptor(prefix, method));
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
      handlerList.add(new RouteDescriptor(prefix, apply));
    }
    return handlerList;
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
    return InsnNode.prev(node)
        .filter(it -> it instanceof LdcInsnNode)
        .findFirst()
        .map(it -> ((LdcInsnNode) it).cst.toString())
        .orElseThrow(() -> new IllegalStateException(
            "Route pattern not found: " + InsnNode.toString(methodInsnNode)));
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
            () -> new IllegalStateException("Handler not found: " + InsnNode.toString(node)));
  }

  private MethodNode findLambda(ExecutionContext ctx, InvokeDynamicInsnNode node) {
    Handle handle = (Handle) node.bsmArgs[1];
    Type owner = TypeFactory.fromInternalName(handle.getOwner());
    return ctx.classNode(owner).methods.stream()
        .filter(n -> n.name.equals(handle.getName()))
        .findFirst()
        .orElseThrow(() ->
            new IllegalStateException("Handler not found: " + InsnNode.toString(node))
        );
  }
}
