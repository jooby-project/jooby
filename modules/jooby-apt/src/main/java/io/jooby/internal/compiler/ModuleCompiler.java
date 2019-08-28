/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.compiler;

import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.SneakyThrows;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.jooby.SneakyThrows.throwingConsumer;
import static java.util.Collections.singletonList;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_2;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

public class ModuleCompiler {
  private static final Type OBJ = getType(Object.class);
  private static final Type MVC_EXTENSION = getType(Extension.class);

  private static final Predicate<String> HTTP_ANNOTATION = it ->
      it.startsWith("io.jooby.annotations")
          || it.startsWith("javax.ws.rs");

  private static final Predicate<String> NULL_ANNOTATION = it -> it.endsWith("NonNull")
      || it.endsWith("NotNull")
      || it.endsWith("Nullable");

  private static final Predicate<String> ATTR_FILTER = HTTP_ANNOTATION.negate()
      .and(NULL_ANNOTATION.negate());

  private final String controllerClass;
  private final String moduleClass;
  private final String moduleInternalName;
  private final String moduleJava;
  private final Types types;
  private final Elements elements;

  public ModuleCompiler(ProcessingEnvironment env, String controllerClass) {
    this.controllerClass = controllerClass;
    this.moduleClass = this.controllerClass + "$Module";
    this.moduleJava = this.moduleClass + ".java";
    this.moduleInternalName = moduleClass.replace(".", "/");
    this.types = env.getTypeUtils();
    this.elements = env.getElementUtils();
  }

  public String getModuleClass() {
    return moduleClass;
  }

  public byte[] compile(List<HandlerCompiler> handlers) throws Exception {
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    // public class Controller$methodName implements Route.Handler {
    writer.visit(V1_8, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC, moduleInternalName, null,
        OBJ.getInternalName(),
        new String[]{MVC_EXTENSION.getInternalName()});
    writer.visitSource(moduleJava, null);

    new ConstructorWriter()
        .build(moduleClass, writer);

    install(writer, handlers);

    writer.visitEnd();
    return writer.toByteArray();
  }

  private void install(ClassWriter writer, List<HandlerCompiler> handlers) throws Exception {
    Method install = Extension.class.getDeclaredMethod("install", Jooby.class);
    MethodVisitor visitor = writer
        .visitMethod(ACC_PUBLIC, install.getName(), Type.getMethodDescriptor(install), null, null);
    visitor.visitParameter("app", 0);
    visitor.visitCode();
    Label sourceStart = new Label();
    visitor.visitLabel(sourceStart);

    for (HandlerCompiler handler : handlers) {
      visitor.visitVarInsn(ALOAD, 1);
      visitor.visitLdcInsn(handler.getPattern());
      visitor.visitTypeInsn(NEW, handler.getGeneratedInternalClass());
      visitor.visitInsn(DUP);
      visitor.visitVarInsn(ALOAD, 0);
      visitor.visitFieldInsn(GETFIELD, moduleInternalName, "provider",
          "Ljavax/inject/Provider;");
      visitor.visitMethodInsn(INVOKESPECIAL, handler.getGeneratedInternalClass(), "<init>",
          "(Ljavax/inject/Provider;)V", false);
      visitor.visitMethodInsn(INVOKEVIRTUAL, "io/jooby/Jooby", handler.getHttpMethod(),
          "(Ljava/lang/String;Lio/jooby/Route$Handler;)Lio/jooby/Route;", false);
      visitor.visitVarInsn(ASTORE, 2);
      visitor.visitVarInsn(ALOAD, 2);
      /**
       * ******************************************************************************************
       * Return Type:
       * ******************************************************************************************
       */
      setReturnType(visitor, handler);

      /**
       * ******************************************************************************************
       * Consumes and Produces
       * ******************************************************************************************
       */
      setContentType(visitor, "setConsumes", handler.getConsumes());
      setContentType(visitor, "setProduces", handler.getProduces());

      /**
       * Annotations
       */
      setAnnotations(visitor, handler);
    }
    visitor.visitInsn(RETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
  }

  private Map<String, Object> annotationMap(ExecutableElement method) {
    return annotationMap(method.getAnnotationMirrors(), null);
  }

  private Map<String, Object> annotationMap(List<? extends AnnotationMirror> annotations,
      String root) {
    Map<String, Object> result = new HashMap<>();
    for (AnnotationMirror annotation : annotations) {
      if (!ATTR_FILTER.test(annotation.getAnnotationType().toString())) {
        // Ignore core,jars annnotations
        continue;
      }
      String prefix = root == null
          ? annotation.getAnnotationType().asElement().getSimpleName().toString()
          : root;
      Map<? extends ExecutableElement, ? extends AnnotationValue> values = elements
          .getElementValuesWithDefaults(annotation);
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> attribute : values
          .entrySet()) {
        ExecutableElement key = attribute.getKey();
        String method = key.getSimpleName().toString();
        String name = method.equals("value") ? prefix : prefix + "." + method;
        Object value = annotationValue(attribute.getValue());
        if (value != null) {
          result.put(name, value);
        }
      }
    }
    return result;
  }

  private Object annotationValue(AnnotationValue annotationValue) {
    Object value = annotationValue.getValue();
    if (value instanceof AnnotationMirror) {
      Map<String, Object> annotation = annotationMap(singletonList((AnnotationMirror) value), null);
      return annotation.isEmpty() ? null : annotation;
    } else if (value instanceof List) {
      List<AnnotationValue> values = (List) value;
      if (values.size() > 0) {
        List<Object> result = new ArrayList<>();
        for (AnnotationValue it : values) {
          result.add(annotationValue(it));
        }
        return result;
      }
      return null;
    }
    return value;
  }

  private void setAnnotations(MethodVisitor visitor, HandlerCompiler handler)
      throws NoSuchMethodException {
    Method target = Route.class.getDeclaredMethod("attribute", String.class, Object.class);
    Map<String, Object> attributes = annotationMap(handler.getExecutable());
    for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
      String name = attribute.getKey();
      Object value = attribute.getValue();

      if (value instanceof Map) {
        // ignore annotation attribute
        continue;
      }

      visitor.visitVarInsn(ALOAD, 2);
      visitor.visitLdcInsn(name);
      if (value instanceof List) {
        List values = (List) value;
        if (values.size() > 0) {
          ArrayWriter.write(visitor, values.get(0).getClass(), values, throwingConsumer(v ->
              annotationSingleValue(visitor, v)
          ));
          visitor.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList",
              "([Ljava/lang/Object;)Ljava/util/List;", false);
        }
      } else {
        annotationSingleValue(visitor, value);
      }
      visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(target.getDeclaringClass()),
          target.getName(), Type.getMethodDescriptor(target), false);
      visitor.visitInsn(POP);
    }
  }

  private void annotationSingleValue(MethodVisitor visitor, Object value)
      throws NoSuchMethodException {
    if (value instanceof String) {
      visitor.visitLdcInsn(value);
    } else if (value instanceof Boolean) {
      annotationBoolean(visitor, (Boolean) value, false, ICONST_0, ICONST_1);
    } else if (value instanceof Character) {
      annotationCharacter(visitor, (Character) value, false,
          ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5);
    } else if (value instanceof Short) {
      annotationNumber(visitor, (Number) value, true,
          ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5);
    } else if (value instanceof Integer) {
      annotationNumber(visitor, (Number) value, true,
          ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5);
    } else if (value instanceof Long) {
      annotationNumber(visitor, (Number) value, false, LCONST_0, LCONST_1);
    } else if (value instanceof Float) {
      annotationNumber(visitor, (Number) value, false, FCONST_0, FCONST_1,
          FCONST_2);
    } else if (value instanceof Double) {
      annotationNumber(visitor, (Number) value, false, DCONST_0, DCONST_1);
    } else if (value instanceof TypeMirror) {
      TypeDefinition typeDef = new TypeDefinition(types, (TypeMirror) value);
      if (typeDef.isPrimitive()) {
        Method wrapper = Primitives.wrapper(typeDef);
        visitor.visitFieldInsn(GETSTATIC, Type.getInternalName(wrapper.getDeclaringClass()),
            "TYPE", "Ljava/lang/Class;");
      } else {
        visitor.visitLdcInsn(typeDef.toJvmType());
      }
    }

    Method wrapper = Primitives.wrapper(value.getClass());
    if (wrapper != null) {
      visitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(wrapper.getDeclaringClass()),
          wrapper.getName(), Type.getMethodDescriptor(wrapper), false);
    }
  }

  private void annotationBoolean(MethodVisitor visitor, Boolean value, boolean checkRange,
      Integer... constants) {
    annotationPrimitive(visitor, value, checkRange,
        b -> b.booleanValue() ? 1 : 0, constants);
  }

  private void annotationCharacter(MethodVisitor visitor, Character value, boolean checkRange,
      Integer... constants) {
    annotationPrimitive(visitor, value, checkRange,
        c -> (int) c.charValue(), constants);
  }

  private void annotationNumber(MethodVisitor visitor, Number value, boolean checkRange,
      Integer... constants) {
    annotationPrimitive(visitor, value, checkRange, Number::intValue, constants);
  }

  private <T> void annotationPrimitive(MethodVisitor visitor, T value,
      boolean checkRange, SneakyThrows.Function<T, Integer> intMapper, Integer... constants) {
    int v = intMapper.apply(value).intValue();
    if (v >= 0 && v <= constants.length) {
      visitor.visitInsn(constants[v].intValue());
    } else {
      if (checkRange) {
        if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
          visitor.visitIntInsn(Opcodes.BIPUSH, v);
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
          visitor.visitIntInsn(Opcodes.SIPUSH, v);
        } else {
          visitor.visitLdcInsn(value);
        }
      } else {
        visitor.visitLdcInsn(value);
      }
    }
  }

  private void setReturnType(MethodVisitor visitor, HandlerCompiler handler)
      throws NoSuchMethodException {
    TypeDefinition returnType = handler.getReturnType();
    if (returnType.isVoid()) {
      visitor.visitLdcInsn(Type.getType(Context.class));
    } else if (returnType.isPrimitive()) {
      Method wrapper = Primitives.wrapper(returnType);
      visitor.visitFieldInsn(GETSTATIC, Type.getInternalName(wrapper.getDeclaringClass()), "TYPE",
          "Ljava/lang/Class;");
    } else if (returnType.isRawType()) {
      visitor.visitLdcInsn(handler.getReturnType().toJvmType());
    } else {
      visitor.visitLdcInsn(returnType.toJvmType());

      List<TypeDefinition> args = returnType.getArguments();

      ArrayWriter.write(visitor, java.lang.reflect.Type.class, args, type ->
          visitor.visitLdcInsn(type.toJvmType())
      );

      Method reified = Reified.class.getMethod("getParameterized", java.lang.reflect.Type.class,
          java.lang.reflect.Type[].class);
      visitor.visitMethodInsn(INVOKESTATIC, "io/jooby/Reified", reified.getName(),
          getMethodDescriptor(reified), false);
      Method reifiedToType = Reified.class.getDeclaredMethod("getType");
      visitor.visitMethodInsn(INVOKEVIRTUAL, "io/jooby/Reified", reifiedToType.getName(),
          getMethodDescriptor(reifiedToType), false);
    }
    Method setReturnType = Route.class
        .getDeclaredMethod("setReturnType", java.lang.reflect.Type.class);
    visitor.visitMethodInsn(INVOKEVIRTUAL, "io/jooby/Route", setReturnType.getName(),
        getMethodDescriptor(setReturnType), false);
    visitor.visitInsn(POP);
  }

  private void setContentType(MethodVisitor visitor, String methodName, List<String> mediaTypes) {
    if (mediaTypes.size() > 0) {
      visitor.visitVarInsn(ALOAD, 2);
      ArrayWriter.write(visitor, MediaType.class, mediaTypes, mediaType -> {
        visitor.visitLdcInsn(mediaType);
        visitor.visitMethodInsn(INVOKESTATIC, "io/jooby/MediaType", "valueOf",
            "(Ljava/lang/String;)Lio/jooby/MediaType;", false);
      });
      visitor.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList",
          "([Ljava/lang/Object;)Ljava/util/List;", false);
      visitor.visitMethodInsn(INVOKEVIRTUAL, "io/jooby/Route", methodName,
          "(Ljava/util/Collection;)Lio/jooby/Route;", false);
      visitor.visitInsn(POP);
    }
  }
}
