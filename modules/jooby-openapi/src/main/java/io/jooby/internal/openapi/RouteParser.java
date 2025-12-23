/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import static io.jooby.internal.openapi.AsmUtils.findAnnotationByType;
import static io.jooby.internal.openapi.RoutePath.path;
import static io.jooby.internal.openapi.StatusCodeParser.isSuccessCode;
import static io.jooby.internal.openapi.TypeFactory.JOOBY;
import static io.jooby.internal.openapi.TypeFactory.KOOBY;
import static io.jooby.internal.openapi.TypeFactory.KOOBYKT;
import static io.jooby.internal.openapi.TypeFactory.KT_FUN_1;
import static io.jooby.internal.openapi.TypeFactory.OBJECT;
import static io.jooby.internal.openapi.TypeFactory.STRING;
import static org.objectweb.asm.Opcodes.GETSTATIC;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.SneakyThrows;
import io.jooby.annotation.OpenApiRegister;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

public class RouteParser {

  public List<OperationExt> parse(ParserContext ctx, OpenAPIExt openapi) {
    List<OperationExt> operations = parse(ctx, null, ctx.classNode(ctx.getRouter()));

    operations.addAll(parseManuallyRegisteredControllers(ctx));

    String applicationName =
        Optional.ofNullable(ctx.getMainClass()).orElse(ctx.getRouter().getClassName());
    ClassNode application = ctx.classNode(Type.getObjectType(applicationName.replace(".", "/")));
    // JavaDoc
    addJavaDoc(ctx, ctx.getRouter().getClassName(), "", operations);

    // swagger/openapi:
    for (OperationExt operation : operations) {
      operation.setApplication(application);
      OpenAPIParser.parse(ctx, openapi, operation);
    }

    List<OperationExt> result = new ArrayList<>();
    for (OperationExt operation : operations) {
      List<String> patterns = Router.expandOptionalVariables(operation.getPath());
      if (patterns.size() == 1) {
        result.add(operation);
      } else {
        for (String pattern : patterns) {
          result.add(operation.copy(pattern));
        }
      }
    }

    // Initialize schema types
    for (OperationExt operation : result) {
      /** Parameters: */
      operation.setParameters(checkParameters(ctx, operation.getParameters()));
      /** Request body */
      checkRequestBody(ctx, operation);
      /** Responses: */
      checkResponses(ctx, operation);
    }

    uniqueOperationId(result);

    // finalize/cleanup/etc
    cleanup(result);
    return result;
  }

  private static void addJavaDoc(
      ParserContext ctx, String className, String prefix, List<OperationExt> operations) {
    // javadoc
    var offset = prefix == null || prefix.isEmpty() ? 0 : prefix.length();
    var javaDoc = ctx.javadoc().parse(className);
    for (OperationExt operation : operations) {
      // Script/lambda
      if (operation.getController() == null) {
        javaDoc
            .flatMap(
                doc -> doc.getScript(operation.getMethod(), operation.getPath().substring(offset)))
            .ifPresent(
                scriptDoc -> {
                  if (scriptDoc.getPath() != null) {
                    JavaDocSetter.setPath(operation, scriptDoc.getPath());
                  }
                  JavaDocSetter.set(operation, scriptDoc);
                });
      }
    }
  }

  private void checkResponses(ParserContext ctx, OperationExt operation) {
    // checkResponse(ctx, operation, 200, operation.getDefaultResponse());
    for (Map.Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
      checkResponse(ctx, operation, entry.getKey(), (ResponseExt) entry.getValue());
    }
  }

  private void checkResponse(
      ParserContext ctx, OperationExt operation, String statusCode, ResponseExt response) {
    Schema defaultSchema = parseSchema(ctx, response);
    if (defaultSchema != null) {
      Content content = response.getContent();
      if (content == null) {
        content = new Content();
        response.setContent(content);
      }

      if (content.isEmpty()) {
        var mediaTypeObject = new io.swagger.v3.oas.models.media.MediaType();
        String mediaType = operation.getProduces().stream().findFirst().orElse(MediaType.JSON);
        content.addMediaType(mediaType, mediaTypeObject);
      }
      if (isSuccessCode(statusCode)) {
        for (var mediaType : content.values()) {
          Optional.ofNullable(response.getExamples()).ifPresent(mediaType::setExample);
          var schema = mediaType.getSchema();
          if (schema == null) {
            mediaType.setSchema(defaultSchema);
          }
        }
      }
    }
  }

  private void checkRequestBody(ParserContext ctx, OperationExt operation) {
    RequestBodyExt requestBody = operation.getRequestBody();
    if (requestBody != null) {
      if (requestBody.getContent() == null) {
        // default content
        var mediaType = new io.swagger.v3.oas.models.media.MediaType();
        mediaType.setSchema(ctx.schema(requestBody.getJavaType()));
        String mediaTypeName =
            operation.getConsumes().stream().findFirst().orElseGet(requestBody::getContentType);
        var content = new Content();
        content.addMediaType(mediaTypeName, mediaType);
        requestBody.setContent(content);
      }
      if (requestBody.getJavaType() != null) {
        ctx.schemaRef(requestBody.getJavaType())
            .map(ref -> ref.schema)
            .ifPresent(
                schema -> {
                  JakartaConstraints.apply(
                      ctx.classNode(TypeFactory.fromJavaName(requestBody.getJavaType())), schema);
                });
      }
      if (requestBody.getExamples() != null) {
        requestBody
            .getContent()
            .forEach((key, value) -> value.setExample(requestBody.getExamples()));
      }
    }
  }

  private List<Parameter> checkParameters(ParserContext ctx, List<Parameter> parameters) {
    List<Parameter> params = new ArrayList<>();
    for (Parameter parameter : parameters) {
      var paramExt = (ParameterExt) parameter;
      String javaType = paramExt.getJavaType();
      if (parameter.getSchema() == null) {
        Optional.ofNullable(ctx.schema(javaType))
            .ifPresent(
                schema -> {
                  schema.setDefault(paramExt.getDefaultValue());
                  parameter.setSchema(schema);
                });
      }
      if (paramExt.isPassword()) {
        parameter.getSchema().setFormat("password");
      }
      JakartaConstraints.apply(parameter.getSchema(), ((ParameterExt) parameter).getAnnotations());

      if (parameter.getIn().equals("query")) {
        boolean expand =
            ctx.schemaRef(javaType)
                .filter(
                    ref ->
                        "object".equals(ref.schema.getType())
                            || (ref.schema.getTypes() != null
                                && ref.schema.getTypes().contains("object")))
                .isPresent();
        if (expand) {
          SchemaRef ref = ctx.schemaRef(javaType).get();
          var doc = ctx.javadoc().parse(javaType).orElse(null);
          JakartaConstraints.apply(ctx.classNode(TypeFactory.fromJavaName(javaType)), ref.schema);
          for (Object e : ref.schema.getProperties().entrySet()) {
            String name = (String) ((Map.Entry) e).getKey();
            Schema s = (Schema) ((Map.Entry) e).getValue();
            var schemaMap = ctx.json().convertValue(s, Map.class);
            schemaMap.remove("description");
            var schemaNoDesc = ctx.json().convertValue(schemaMap, Schema.class);
            ParameterExt p = new ParameterExt();
            p.setName(name);
            p.setIn(parameter.getIn());
            p.setSchema(schemaNoDesc);
            // default doc
            p.setDescription(parameter.getDescription());
            if (doc != null) {
              var propertyDoc = doc.getPropertyDoc(name);
              if (propertyDoc != null) {
                p.setDescription(propertyDoc);
              }
            }
            params.add(p);
          }
        } else {
          params.add(parameter);
        }
      } else {
        params.add(parameter);
      }
    }
    return params;
  }

  private void uniqueOperationId(List<OperationExt> operations) {
    Map<String, AtomicInteger> names = new HashMap<>();
    for (OperationExt operation : operations) {
      String operationId = operationId(operation);
      int c = names.computeIfAbsent(operationId, k -> new AtomicInteger()).incrementAndGet();
      if (c > 1) {
        operation.setOperationId(operationId + c);
      } else {
        operation.setOperationId(operationId);
      }
    }
  }

  private String operationId(OperationExt operation) {
    return Optional.ofNullable(operation.getOperationId())
        .orElseGet(
            () -> operation.getMethod().toLowerCase() + patternToOperationId(operation.getPath()));
  }

  private String patternToOperationId(String pattern) {
    if (pattern.equals("/")) {
      return "";
    }
    return Stream.of(pattern.split("\\W+"))
        .filter(s -> s.length() > 0)
        .map(
            segment ->
                Character.toUpperCase(segment.charAt(0))
                    + (segment.length() > 1 ? segment.substring(1) : ""))
        .collect(Collectors.joining());
  }

  private void cleanup(List<OperationExt> operations) {
    var it = operations.iterator();
    while (it.hasNext()) {
      var operation = it.next();
      if (operation.getHidden() == Boolean.TRUE) {
        it.remove();
      } else {
        if (operation.getParameters().isEmpty()) {
          operation.setParameters(null);
        }
      }
    }
  }

  private Schema parseSchema(ParserContext ctx, ResponseExt response) {
    List<String> javaTypes = response.getJavaTypes();
    Schema schema;
    if (javaTypes.size() == 1) {
      schema = ctx.schema(javaTypes.get(0));
    } else if (javaTypes.size() > 1) {
      List<Schema> schemas =
          javaTypes.stream().map(ctx::schema).filter(Objects::nonNull).collect(Collectors.toList());
      schema = schemas.isEmpty() ? null : new ComposedSchema().oneOf(schemas);
    } else {
      schema = null;
    }
    return schema;
  }

  public List<OperationExt> parse(ParserContext ctx, String prefix, ClassNode node) {
    List<OperationExt> handlerList = new ArrayList<>();
    for (MethodNode method : node.methods) {
      handlerList.addAll(routeHandler(ctx, prefix, method));
    }
    return handlerList;
  }

  private List<OperationExt> parseManuallyRegisteredControllers(ParserContext ctx) {
    List<OperationExt> handlerList = new ArrayList<>();
    ClassNode classNode = ctx.classNode(ctx.getRouter());
    findAnnotationByType(classNode.visibleAnnotations, OpenApiRegister.class).stream()
        .map(AsmUtils::toMap)
        .forEach(
            annotationMap -> {
              for (Type registeredClass : ((List<Type>) annotationMap.get("value"))) {
                handlerList.addAll(AnnotationParser.parse(ctx, null, registeredClass));
              }
            });

    return handlerList;
  }

  private List<OperationExt> routeHandler(ParserContext ctx, String prefix, MethodNode method) {
    List<OperationExt> handlerList = new ArrayList<>();
    /** Track the last router instruction and override with produces/consumes. */
    AbstractInsnNode instructionTo = null;
    int routeIndex = -1;
    for (AbstractInsnNode it : method.instructions) {
      if (it instanceof MethodInsnNode node && ctx.process(it)) {
        Signature signature = Signature.create(node);
        if (ctx.isRouter(signature.getOwner().orElse(null))) {
          if (signature.matches("mvc")) {
            handlerList.addAll(AnnotationParser.parse(ctx, prefix, signature, (MethodInsnNode) it));
          } else if (signature.matches("<init>", KT_FUN_1)) {
            handlerList.addAll(kotlinHandler(ctx, null, prefix, node, false));
          } else if (signature.matches(KOOBY)
              && signature.getDescriptor() != null
              && Type.getReturnType(signature.getDescriptor()) == Type.VOID_TYPE) {
            handlerList.addAll(kotlinHandler(ctx, null, prefix, node, true));
          } else if (signature.matches("mount", Router.class)) {
            handlerList.addAll(mountHandler(ctx, prefix, null, node));
          } else if (signature.matches("install", String.class, SneakyThrows.Supplier.class)) {
            String pattern = routePattern(node, node);
            handlerList.addAll(installApp(ctx, path(prefix, pattern), node, node));
          } else if (signature.matches("install", SneakyThrows.Supplier.class)) {
            handlerList.addAll(installApp(ctx, prefix, node, node));
          } else if (signature.matches("mount", String.class, Router.class)) {
            handlerList.addAll(
                mountHandler(ctx, prefix, routePatternNode(node, node).cst.toString(), node));
          } else if (signature.matches("path", String.class, Runnable.class)
              || signature.matches("routes", Runnable.class)) {
            boolean routes = signature.matches("routes", Runnable.class);
            routeIndex = handlerList.size();
            instructionTo = node;
            //  router path (Ljava/lang/String;Ljava/lang/Runnable;)Lio/jooby/Route;
            if (node.owner.equals(TypeFactory.KOOBY.getInternalName())) {
              MethodInsnNode subrouteInsn =
                  InsnSupport.prev(node)
                      .filter(MethodInsnNode.class::isInstance)
                      .findFirst()
                      .map(MethodInsnNode.class::cast)
                      .orElseThrow(
                          () -> new IllegalStateException("Subroute definition not found"));
              String path = routes ? "/" : routePattern(node, subrouteInsn);
              handlerList.addAll(kotlinHandler(ctx, null, path(prefix, path), subrouteInsn, false));
            } else {
              InvokeDynamicInsnNode subrouteInsn =
                  InsnSupport.prev(node)
                      .filter(InvokeDynamicInsnNode.class::isInstance)
                      .findFirst()
                      .map(InvokeDynamicInsnNode.class::cast)
                      .orElseThrow(
                          () -> new IllegalStateException("Subroute definition not found"));
              String path = routes ? "/" : routePattern(node, subrouteInsn);
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
              handlerList.addAll(kotlinHandler(ctx, httpMethod, path(prefix, path), node, false));
            } else {
              if (previous instanceof InvokeDynamicInsnNode) {
                MethodNode handler = findLambda(ctx, (InvokeDynamicInsnNode) previous);
                ctx.debugHandler(handler);
                handlerList.add(newRouteDescriptor(ctx, handler, httpMethod, path(prefix, path)));
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
                  AbstractInsnNode astore =
                      InsnSupport.prev(varInsnNode)
                          .filter(InsnSupport.varInsn(Opcodes.ASTORE, varInsnNode.var))
                          .findFirst()
                          .orElse(null);
                  if (astore != null) {
                    AbstractInsnNode varType =
                        InsnSupport.prev(astore)
                            .filter(
                                e ->
                                    (e instanceof InvokeDynamicInsnNode
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
              && signature.matches(STRING, KT_FUN_1)) {
            instructionTo = node;
            String path = routePattern(node, node.getPrevious());
            String httpMethod = signature.getMethod().toUpperCase();
            handlerList.addAll(kotlinHandler(ctx, httpMethod, path(prefix, path), node, false));
          } else if (Router.METHODS.contains(signature.getMethod().toUpperCase())
              && signature.matches(STRING, TypeFactory.KT_FUN_2)) {
            instructionTo = node;
            String path = routePattern(node, node.getPrevious());
            String httpMethod = signature.getMethod().toUpperCase();
            handlerList.addAll(kotlinHandler(ctx, httpMethod, path(prefix, path), node, false));
          } else if (signature.getMethod().startsWith("coroutine")) {
            handlerList.addAll(kotlinHandler(ctx, null, prefix, node, false));
          }
        } else if (signature.matches(KOOBYKT, "runApp")) {
          handlerList.addAll(
              kotlinRunApp(
                  ctx,
                  prefix,
                  node,
                  signature.endsWith("runApp", TypeFactory.KT_FUN_0)
                      || signature.endsWith("runApp", TypeFactory.KT_FUN_0_ARRAY)));
        } else if (signature.matches(Route.class, "produces", MediaType[].class)) {
          if (instructionTo != null) {
            OperationExt route = handlerList.get(handlerList.size() - 1);
            InsnSupport.prev(it, instructionTo).flatMap(mediaType()).forEach(route::addProduces);
            instructionTo = it;
          }
        } else if (signature.matches(Route.class, "consumes", MediaType[].class)) {
          if (instructionTo != null) {
            OperationExt route = handlerList.get(handlerList.size() - 1);
            InsnSupport.prev(it, instructionTo).flatMap(mediaType()).forEach(route::addConsumes);
            instructionTo = it;
          }
        } else if (signature.matches(Route.class, "summary", String.class)) {
          instructionTo =
              parseText(it, instructionTo, handlerList.get(handlerList.size() - 1)::setSummary);
        } else if (signature.matches(Route.class, "description", String.class)) {
          instructionTo =
              parseText(it, instructionTo, handlerList.get(handlerList.size() - 1)::setDescription);
        } else if (signature.matches(Route.class, "tags", String[].class)) {
          instructionTo = parseTags(it, instructionTo, handlerList.get(handlerList.size() - 1));
        } else if (signature.matches(Route.Set.class, "summary", String.class)) {
          instructionTo =
              parseText(it, instructionTo, handlerList.get(handlerList.size() - 1)::setPathSummary);
        } else if (signature.matches(Route.Set.class, "description", String.class)) {
          instructionTo =
              parseText(
                  it, instructionTo, handlerList.get(handlerList.size() - 1)::setPathDescription);
        } else if (signature.matches(Route.Set.class, "tags", String[].class)) {
          if (routeIndex >= 0) {
            for (int i = routeIndex; i < handlerList.size(); i++) {
              instructionTo = parseTags(it, instructionTo, handlerList.get(i));
            }
            routeIndex = -1;
          }
        }
      }
    }
    return handlerList;
  }

  private List<OperationExt> mountHandler(
      ParserContext ctx, String prefix, String pattern, MethodInsnNode node) {
    var ktAnonymousRouter = kotlinAnonymousRouter(node);
    var path = pattern == null ? prefix : path(prefix, pattern);
    if (ktAnonymousRouter != null) {
      return routeHandler(
          ctx,
          path,
          ctx.findMethodNode(
              Type.getObjectType(ktAnonymousRouter.getOwner()), ktAnonymousRouter.getName()));
    } else {
      var routerInstruction = findRouterInstruction(node);
      return mountRouter(ctx, path, node, routerInstruction);
    }
  }

  private Handle kotlinAnonymousRouter(AbstractInsnNode node) {
    return InsnSupport.prev(node)
        .filter(InvokeDynamicInsnNode.class::isInstance)
        .map(InvokeDynamicInsnNode.class::cast)
        .filter(it -> it.name.equals("invoke") && Type.getReturnType(it.desc).equals(KT_FUN_1))
        .map(it -> (Handle) it.bsmArgs[1])
        .findFirst()
        .orElse(null);
  }

  private AbstractInsnNode parseText(
      AbstractInsnNode start, AbstractInsnNode end, Consumer<String> consumer) {
    if (end != null) {
      InsnSupport.prev(start, end)
          .filter(LdcInsnNode.class::isInstance)
          .findFirst()
          .map(LdcInsnNode.class::cast)
          .map(i -> i.cst.toString())
          .ifPresent(consumer);
    }
    return start;
  }

  private AbstractInsnNode parseTags(
      AbstractInsnNode start, AbstractInsnNode end, OperationExt route) {
    if (end != null) {
      InsnSupport.prev(start, end)
          .filter(LdcInsnNode.class::isInstance)
          .map(LdcInsnNode.class::cast)
          .map(i -> i.cst.toString())
          .forEach(route::addTagsItem);
    }
    return start;
  }

  private List<OperationExt> kotlinRunApp(
      ParserContext ctx, String prefix, MethodInsnNode node, boolean supplier) {
    List<OperationExt> handlerList = new ArrayList<>();
    Type type = null;
    for (AbstractInsnNode it : InsnSupport.prev(node).toList()) {
      if (it instanceof FieldInsnNode) {
        FieldInsnNode getstatic = (FieldInsnNode) it;
        if (getstatic.getOpcode() == GETSTATIC) {
          type = Type.getObjectType(getstatic.owner);
          break;
        }
      } else if (it instanceof InvokeDynamicInsnNode invokeDynamic) {
        var handle = (Handle) invokeDynamic.bsmArgs[1];
        type = Type.getObjectType(handle.getOwner());
        break;
      }
    }
    if (type == null) {
      throw new IllegalStateException("io.jooby.runApp(String[]) parsing failure");
    }
    ClassNode classNode = ctx.classNode(type);
    if (supplier) {
      /** runApp(args, ::App); */
      var signature = classNode.signature;
      var mainClass =
          Type.getType(signature.substring(signature.lastIndexOf('<') + 1, signature.length() - 2));
      ctx.setMainClass(mainClass.getClassName());
      classNode = ctx.classNode(mainClass);
    }
    handlerList.addAll(parse(ctx, prefix, classNode));
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
              .stream();
        }
      }
      return Stream.of();
    };
  }

  private AbstractInsnNode findRouterInstruction(AbstractInsnNode node) {
    return InsnSupport.prev(node)
        .filter(
            e -> {
              if (e instanceof TypeInsnNode) {
                return e.getOpcode() != Opcodes.CHECKCAST;
              } else if (e instanceof LdcInsnNode) {
                return ((LdcInsnNode) e).cst instanceof Type;
              }
              return false;
            })
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Unsupported router type: " + node));
  }

  private List<OperationExt> mountRouter(
      ParserContext ctx, String prefix, MethodInsnNode node, AbstractInsnNode routerInstruction) {
    Type router;
    if (routerInstruction instanceof TypeInsnNode) {
      router = Type.getObjectType(((TypeInsnNode) routerInstruction).desc);
    } else if (routerInstruction instanceof LdcInsnNode) {
      router = (Type) ((LdcInsnNode) routerInstruction).cst;
    } else {
      throw new UnsupportedOperationException(InsnSupport.toString(node));
    }
    ClassNode classNode = ctx.classNode(router);
    var operations = parse(ctx.newContext(router), prefix, classNode);
    addJavaDoc(ctx, router.getClassName(), prefix, operations);
    return operations;
  }

  private List<OperationExt> installApp(
      ParserContext ctx, String prefix, MethodInsnNode node, AbstractInsnNode ins) {
    Type router;
    AbstractInsnNode previous = ins.getPrevious();
    if (previous instanceof InvokeDynamicInsnNode) {
      InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) previous;
      Handle handle = (Handle) idin.bsmArgs[1];
      router = TypeFactory.fromInternalName(handle.getOwner());
      if (!handle.getName().equals("<init>")) {
        MethodNode lambda = findLambda(ctx, idin);
        router =
            ReturnTypeParser.parse(ctx, lambda).stream()
                .findFirst()
                .map(TypeFactory::fromJavaName)
                .orElseThrow(() -> new UnsupportedOperationException(InsnSupport.toString(node)));
      }
    } else if (node.owner.equals("io/jooby/kt/Kooby")) {
      router = kotlinSupplier(ctx, node, previous);
    } else {
      throw new UnsupportedOperationException(InsnSupport.toString(node));
    }
    ClassNode classNode = ctx.classNode(router);
    var operations = parse(ctx.newContext(router), prefix, classNode);
    addJavaDoc(ctx, router.getClassName(), prefix, operations);
    return operations;
  }

  private Type kotlinSupplier(ParserContext ctx, MethodInsnNode node, AbstractInsnNode ins) {
    FieldInsnNode frame =
        InsnSupport.prev(ins)
            .filter(FieldInsnNode.class::isInstance)
            .map(FieldInsnNode.class::cast)
            .filter(it -> it.getOpcode() == GETSTATIC)
            .findFirst()
            .orElse(null);
    Type type = null;
    if (frame != null) {
      ClassNode lambdaClass = ctx.classNode(TypeFactory.fromInternalName(frame.owner));
      type =
          findMethods(
                  lambdaClass,
                  "invoke",
                  (method, signature) -> (method.access & Opcodes.ACC_PUBLIC) != 0)
              .stream()
              .map(
                  it -> {
                    // visitMethod(ACC_PUBLIC | ACC_FINAL, "invoke", "()LReturnType", null, null);
                    String desc = Optional.ofNullable(it.signature).orElse(it.desc);
                    return Type.getReturnType(desc);
                  })
              .filter(it -> !it.equals(OBJECT))
              .findFirst()
              .orElseGet(
                  () ->
                      // SneakyThrows.Supplier
                      findMethods(
                              lambdaClass,
                              "tryGet",
                              (method, signature) ->
                                  Type.getReturnType(method.desc).equals(JOOBY)
                                      || Type.getReturnType(method.desc).equals(KOOBY))
                          .stream()
                          .findFirst()
                          .map(
                              it ->
                                  ReturnTypeParser.parseIgnoreSignature(ctx, it).stream()
                                      .findFirst()
                                      .map(TypeFactory::fromJavaName)
                                      .orElse(null))
                          .orElseGet(
                              () ->
                                  findMethods(lambdaClass, "<init>", (method, signature) -> true)
                                      .stream()
                                      .findFirst()
                                      .map(
                                          init ->
                                              InsnSupport.next(init.instructions.getFirst())
                                                  .filter(LdcInsnNode.class::isInstance)
                                                  .map(LdcInsnNode.class::cast)
                                                  .filter(it -> Type.class.isInstance(it.cst))
                                                  .map(it -> (Type) it.cst)
                                                  .findFirst()
                                                  .orElse(null))
                                      .orElse(null)));
    }
    if (type == null) {
      throw new UnsupportedOperationException(InsnSupport.toString(node));
    }
    return type;
  }

  private List<MethodNode> findMethods(
      ClassNode clazz, String name, BiPredicate<MethodNode, Signature> predicate) {
    List<MethodNode> result = new ArrayList<>();
    for (MethodNode method : clazz.methods) {
      if (method.name.equals(name)) {
        Signature signature = Signature.create(method);
        if (predicate.test(method, signature)) {
          result.add(method);
        }
      }
    }
    return result;
  }

  private List<OperationExt> kotlinHandler(
      ParserContext ctx,
      String httpMethod,
      String prefix,
      MethodInsnNode node,
      boolean extensionMethod) {
    List<OperationExt> handlerList = new ArrayList<>();
    List<String> lookup;
    // Extension method must be defined in router but not in Kooby
    if (extensionMethod) {
      lookup = List.of(node.owner, node.name, node.desc);
    } else {
      // [0] - Owner
      // [1] - Method name. Optional
      // [2] - Method descriptor. Optional
      lookup =
          InsnSupport.prev(node.getPrevious())
              .map(
                  it -> {
                    if (it instanceof InvokeDynamicInsnNode invokeDynamic) {
                      Object[] args = invokeDynamic.bsmArgs;
                      if (args.length > 1 && args[1] instanceof Handle handle) {
                        return Arrays.asList(handle.getOwner(), handle.getName(), handle.getDesc());
                      }
                    }
                    if (it instanceof FieldInsnNode fieldInsnNode) {
                      return Collections.singletonList(fieldInsnNode.owner);
                    }
                    if (it instanceof MethodInsnNode methodInsnNode) {
                      Signature signature = Signature.create(methodInsnNode);
                      if (!signature.matches("<init>", KT_FUN_1)) {
                        return Collections.singletonList(((MethodInsnNode) it).owner);
                      }
                    }
                    return null;
                  })
              .filter(Objects::nonNull)
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Kotlin lambda not found: " + InsnSupport.toString(node)));
    }

    ClassNode classNode = ctx.classNode(Type.getObjectType(lookup.get(0)));
    MethodNode apply = null;
    if (lookup.size() > 1) {
      MethodNode method =
          classNode.methods.stream()
              .filter(it -> it.name.equals(lookup.get(1)) && it.desc.equals(lookup.get(2)))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Kotlin lambda not found: " + InsnSupport.toString(node)));
      ctx.debugHandlerLink(method);
      boolean synthetic = (method.access & Opcodes.ACC_PRIVATE) != 0;
      if (synthetic && (method.name.startsWith("invoke$") || method.name.contains("$lambda"))) {
        method = ktFunRef160(ctx, method);
      }
      if (httpMethod == null) {
        handlerList.addAll(routeHandler(ctx, prefix, method));
      } else {
        handlerList.add(newRouteDescriptor(ctx, method, httpMethod, prefix));
      }
    } else {
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
        } else if (signature.matches("invoke", TypeFactory.CONTEXT)) {
          // fun reference
          MethodNode ref = kotlinFunctionReference(ctx, classNode, method);
          ctx.debugHandler(ref);
          handlerList.add(newRouteDescriptor(ctx, ref, httpMethod, prefix));
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
    }
    if (apply != null) {
      // almost there can be one of two: 1) lambda itself or 2) method reference
      Signature signature = Signature.create(node);
      if (signature.matches(String.class, Route.Handler.class)) {
        apply = ktFunRef160(ctx, apply);
      }
      ctx.debugHandler(apply);
      handlerList.add(newRouteDescriptor(ctx, apply, httpMethod, prefix));
    }
    return handlerList;
  }

  private MethodNode ktFunRef160(ParserContext ctx, MethodNode method) {
    MethodInsnNode call =
        InsnSupport.prev(method.instructions.getLast())
            .filter(MethodInsnNode.class::isInstance)
            .map(MethodInsnNode.class::cast)
            .filter(it -> Signature.create(it).matches(Context.class))
            .findFirst()
            .orElse(null);
    if (call != null) {
      ClassNode owner = ctx.classNodeOrNull(Type.getObjectType(call.owner));
      if (owner != null) {
        MethodNode methodRef =
            owner.methods.stream()
                .filter(it -> it.name.equals(call.name) && it.desc.equals(call.desc))
                .findFirst()
                .orElse(null);
        if (methodRef != null) {
          return methodRef;
        }
      }
    }
    // fallback to what we found previously
    return method;
  }

  private MethodNode kotlinFunctionReference(
      ParserContext ctx, ClassNode classNode, MethodNode node) {
    MethodInsnNode ref =
        InsnSupport.prev(node.instructions.getLast())
            .filter(MethodInsnNode.class::isInstance)
            .map(MethodInsnNode.class::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Kotlin reference function not found"));
    String refname =
        ref.name.equals("invoke")
            ? classNode.methods.stream()
                .filter(m -> m.name.equals("getName"))
                .findFirst()
                .map(
                    m ->
                        InsnSupport.next(m.instructions.getFirst())
                            .filter(LdcInsnNode.class::isInstance)
                            .findFirst()
                            .map(LdcInsnNode.class::cast)
                            .map(n -> n.cst.toString())
                            .orElse(ref.name))
                .orElse(ref.name)
            : ref.name;
    MethodNode method =
        ctx.classNode(Type.getObjectType(ref.owner)).methods.stream()
            .filter(m -> m.name.equals(ref.name) && m.desc.equals(ref.desc))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Kotlin reference function not found"));
    method.name = refname;
    return method;
  }

  private OperationExt newRouteDescriptor(
      ParserContext ctx, MethodNode node, String httpMethod, String prefix) {
    Optional<RequestBodyExt> requestBody = RequestParser.requestBody(ctx, node);
    List<ParameterExt> arguments = RequestParser.parameters(node, prefix);
    ResponseExt response = new ResponseExt();
    List<String> returnTypes = ReturnTypeParser.parse(ctx, node);
    response.setJavaTypes(returnTypes);
    OperationExt operation = new OperationExt(node, httpMethod, prefix, arguments, response);

    boolean notSynthetic = (node.access & Opcodes.ACC_SYNTHETIC) == 0;
    boolean lambda =
        node.name.equals("apply")
            || node.name.equals("invoke")
            || node.name.startsWith("invoke$")
            || node.name.contains("$lambda")
            || node.name.startsWith("fake$");
    if (notSynthetic && !lambda) {
      operation.setOperationId(node.name);
    }
    requestBody.ifPresent(operation::setRequestBody);
    return operation;
  }

  private int returnTypePrecedence(MethodNode method) {
    return Type.getReturnType(method.desc).getClassName().equals("java.lang.Object") ? 0 : 1;
  }

  /**
   * IMPORTANT: First lcdInsn must be the pattern. We don't support variable as pattern. Only string
   * literal or final variables.
   *
   * @param methodInsnNode
   * @param node
   * @return
   */
  private LdcInsnNode routePatternNode(MethodInsnNode methodInsnNode, AbstractInsnNode node) {
    return InsnSupport.prev(node)
        .filter(it -> it instanceof LdcInsnNode && (((LdcInsnNode) it).cst instanceof String))
        .findFirst()
        .map(LdcInsnNode.class::cast)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Route pattern not found: " + InsnSupport.toString(methodInsnNode)));
  }

  private String routePattern(MethodInsnNode methodInsnNode, AbstractInsnNode node) {
    return routePatternNode(methodInsnNode, node).cst.toString();
  }

  private MethodNode findRouteHandler(ParserContext ctx, MethodInsnNode node) {
    Type owner = TypeFactory.fromInternalName(node.owner);
    return ctx.classNode(owner).methods.stream()
        .filter(
            m -> {
              Signature signature = new Signature(owner, "apply", m.desc);
              return Modifier.isPublic(m.access) && signature.matches(Context.class);
            })
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Handler not found: " + InsnSupport.toString(node)));
  }

  private MethodNode findLambda(ParserContext ctx, InvokeDynamicInsnNode node) {
    Handle handle = (Handle) node.bsmArgs[1];
    Type owner = TypeFactory.fromInternalName(handle.getOwner());
    if (owner.equals(TypeFactory.CONTEXT)) {
      return contextReference(handle);
    } else {
      return ctx.classNode(owner).methods.stream()
          .filter(n -> n.name.equals(handle.getName()) && n.desc.equals(handle.getDesc()))
          .findFirst()
          .orElseThrow(
              () -> new IllegalStateException("Handler not found: " + InsnSupport.toString(node)));
    }
  }

  private MethodNode contextReference(Handle handle) {
    // Fake a method node, required when we write things like:
    /// get("/", Context::getRequestPath);
    // method reference to a class outside application classpath.
    // Faked method has a return instruction for return type parser (no arguments).
    String suffix = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
    MethodNode method =
        new MethodNode(
            Opcodes.ACC_PRIVATE & Opcodes.ACC_SYNTHETIC,
            "fake$" + handle.getName() + "$" + suffix,
            handle.getDesc(),
            null,
            null);
    method.instructions = new InsnList();
    method.instructions.add(
        new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL, handle.getOwner(), handle.getName(), handle.getDesc()));
    method.instructions.add(new InsnNode(Opcodes.ARETURN));
    return method;
  }
}
