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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
        if (signature.isKoobyInit()) {
          handlerList.addAll(kotlinHandler(ctx, prefix, node));
        } else if (signature.matches("path", String.class, Runnable.class)) {
          //  router path (Ljava/lang/String;Ljava/lang/Runnable;)Lio/jooby/Route;
          InvokeDynamicInsnNode subrouteInsn = InsnNode.prev(node)
              .filter(InvokeDynamicInsnNode.class::isInstance)
              .findFirst()
              .map(InvokeDynamicInsnNode.class::cast)
              .orElseThrow(() -> new IllegalStateException("Subroute definition not found"));
          String path = routePattern(node, subrouteInsn);
          handlerList.addAll(routeHandler(ctx, path(prefix, path), findLambda(ctx, subrouteInsn)));
        } else if (Router.METHODS.contains(signature.getMethod().toUpperCase())
            && signature.isRouteHandler()) {
          //  router get (Ljava/lang/String;Lio/jooby/Route$Handler;)Lio/jooby/Route;
          AbstractInsnNode previous = node.getPrevious();
          String path = routePattern(node, previous);
          if (previous instanceof InvokeDynamicInsnNode) {
            MethodNode handler = findLambda(ctx, (InvokeDynamicInsnNode) previous);
            handlerList.add(new RouteDescriptor(path(prefix, path), handler));
          } else if (previous instanceof MethodInsnNode) {
            if (InsnNode.opcode(Opcodes.INVOKESPECIAL).test(previous)) {
              MethodInsnNode methodInsnNode = (MethodInsnNode) previous;
              MethodNode handler = findRouteHandler(ctx, methodInsnNode);
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
                  handlerList.add(new RouteDescriptor(path(prefix, path), handler));
                } else if (varType instanceof InvokeDynamicInsnNode) {
                  MethodNode handler = findLambda(ctx, (InvokeDynamicInsnNode) varType);
                  handlerList.add(new RouteDescriptor(path(prefix, path), handler));
                }
              }
            }
          }
        }
      }
    }
    return handlerList;
  }

  private List<RouteDescriptor> kotlinHandler(ExecutionContext ctx, String prefix, MethodInsnNode node) {
    List<RouteDescriptor> handlerList = new ArrayList<>();
    FieldInsnNode fieldInsn = InsnNode.prev(node)
        .filter(InsnNode.opcode(Opcodes.GETSTATIC))
        .map(FieldInsnNode.class::cast)
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Kotlin lambda not found: " + InsnNode.toString(node)));
    ClassNode classNode = ctx.classNode(Type.getObjectType(fieldInsn.owner));
    for (MethodNode method : classNode.methods) {
      handlerList.addAll(routeHandler(ctx, prefix, method));
    }
    return handlerList;
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
    String s = Router.leadingSlash(prefix);
    return s.equals("/") ? Router.leadingSlash(path) : s + Router.leadingSlash(path);
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
