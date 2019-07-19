/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.jooby.Extension;
import io.jooby.Jooby;

import javax.inject.Provider;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public class SourceFile {

  private final MethodSpec.Builder main;

  private final String pkg;

  private final String name;

  private TypeSpec.Builder root;

  public SourceFile(TypeElement owner) {
    this.name = owner.getSimpleName().toString();
    this.pkg = owner.getQualifiedName().toString().replace("." + name, "");
    root = TypeSpec.classBuilder(name + "Module")
        .addSuperinterface(Extension.class)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    ParameterizedTypeName providerType = ParameterizedTypeName
        .get(ClassName.get(Provider.class), ClassName.get(pkg, name));
    FieldSpec provider = FieldSpec
        .builder(providerType, "provider", Modifier.PRIVATE, Modifier.FINAL)
        .build();
    root.addField(provider);

    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(providerType, "provider")
        .addStatement("this.$1N = $1N", "provider")
        .build();
    root.addMethod(constructor);

    main = MethodSpec.methodBuilder("install")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(void.class)
        .addParameter(Jooby.class, "app");
  }

  public void newRoute(String method, String path, ExecutableElement executable) {
    CodeBlock statement = CodeBlock.builder()
        .add("app.$L($S, ctx -> {\n", method.toLowerCase(), path)
        .add("return provider.get().$L($L);", executable.getSimpleName().toString(),
            args(executable))
        .add("\n})")
        .build();
    main.addStatement(statement);
  }

  public String getSourceName() {
    return name + "Module";
  }

  private String args(ExecutableElement method) {
    StringBuilder args = new StringBuilder();
    String SEP = ", ";
    for (VariableElement parameter : method.getParameters()) {
      String name = parameter.getSimpleName().toString();
      TypeMirror typeMirror = parameter.asType();
      String parameterType;
      if (typeMirror instanceof DeclaredType) {
        parameterType = ((DeclaredType) typeMirror).asElement().toString();
        //        List<? extends TypeMirror> typeArguments = ((DeclaredType) typeMirror).getTypeArguments();
        //        parameterType = typeArguments.get(typeArguments.size() - 1).toString();
      } else {
        parameterType = typeMirror.toString();
      }
      /** Type injection: */
      if ("io.jooby.Context".equals(parameterType)) {
        args.append("ctx");
      } else if ("io.jooby.QueryString".equals(parameterType)) {
        args.append("ctx.query()");
      } else if ("io.jooby.Formdata".equals(parameterType)) {
        args.append("ctx.form()");
      } else if ("io.jooby.Multipart".equals(parameterType)) {
        args.append("ctx.multipart()");
      } else if ("io.jooby.FlashMap".equals(parameterType)) {
        args.append("ctx.flash()");
      } else {

      }
      args.append(SEP);
    }
    if (args.length() > 0) {
      args.setLength(args.length() - SEP.length());
    }
    return args.toString();
  }

  public JavaFile build() {
    return JavaFile.builder(pkg, root.addMethod(main.build()).build())
        .skipJavaLangImports(true)
        .indent("  ")
        .build();
  }

  @Override public String toString() {
    return build().toString();
  }
}
