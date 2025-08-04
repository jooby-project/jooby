/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocSupport.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.TokenUtil;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

public class ClassDoc extends JavaDocNode {
  private final Map<String, FieldDoc> fields = new LinkedHashMap<>();
  private final Map<String, MethodDoc> methods = new LinkedHashMap<>();
  private final Map<String, MethodDoc> scripts = new LinkedHashMap<>();
  private final List<Server> servers;
  private final List<Contact> contact;
  private final List<License> license;

  public ClassDoc(JavaDocParser ctx, DetailAST node, DetailAST javaDoc) {
    super(ctx, node, javaDoc);
    if (isRecord()) {
      defaultRecordMembers();
    } else if (isEnum()) {
      defaultEnumMembers();
    }
    this.servers = JavaDocTag.servers(this.javadoc);
    this.contact = JavaDocTag.contacts(this.javadoc);
    this.license = JavaDocTag.license(this.javadoc);
  }

  public List<Server> getServers() {
    return servers;
  }

  public List<Contact> getContact() {
    return contact;
  }

  public List<License> getLicense() {
    return license;
  }

  public String getVersion() {
    return tree(javadoc)
        .filter(javadocToken(JavadocTokenTypes.VERSION_LITERAL))
        .findFirst()
        .flatMap(
            version ->
                tree(version.getParent())
                    .filter(javadocToken(JavadocTokenTypes.DESCRIPTION))
                    .findFirst()
                    .flatMap(
                        it -> tree(it).filter(javadocToken(JavadocTokenTypes.TEXT)).findFirst())
                    .map(DetailNode::getText))
        .orElse(null);
  }

  public String getEnumDescription(String text) {
    if (isEnum()) {
      var sb = new StringBuilder();
      var summary = Optional.ofNullable(text).orElseGet(this::getSummary);
      if (summary != null) {
        sb.append(summary);
      }
      for (Map.Entry<String, FieldDoc> e : fields.entrySet()) {
        sb.append("\n  - ").append(e.getKey()).append(": ").append(e.getValue().getText());
      }
      return sb.toString().trim();
    }
    return text;
  }

  private void defaultRecordMembers() {
    JavaDocTag.javaDocTag(
        javadoc,
        tag -> {
          var isParam = tree(tag).anyMatch(javadocToken(JavadocTokenTypes.PARAM_LITERAL));
          var name =
              tree(tag)
                  .filter(javadocToken(JavadocTokenTypes.PARAMETER_NAME))
                  .findFirst()
                  .orElse(null);
          return isParam && name != null;
        },
        (tag, value) -> {
          var name =
              tree(tag)
                  .filter(javadocToken(JavadocTokenTypes.PARAMETER_NAME))
                  .findFirst()
                  .orElse(null);
          // name is never null bc previous filter
          Objects.requireNonNull(name, "name is null");
          /* Virtual Field */
          var memberDoc =
              tree(tag)
                  .filter(javadocToken(JavadocTokenTypes.DESCRIPTION))
                  .findFirst()
                  .orElse(EMPTY_NODE);
          var field =
              new FieldDoc(
                  context, createVirtualMember(name.getText(), TokenTypes.VARIABLE_DEF), memberDoc);
          addField(field);
          /* Virtual method */
          var method =
              new MethodDoc(
                  context, createVirtualMember(name.getText(), TokenTypes.METHOD_DEF), memberDoc);
          addMethod(method);
        });
  }

  private void defaultEnumMembers() {
    for (var constant : tree(node).filter(tokens(TokenTypes.ENUM_CONSTANT_DEF)).toList()) {
      /* Virtual Field */
      var name =
          tree(constant)
              .filter(tokens(TokenTypes.IDENT))
              .findFirst()
              .map(DetailAST::getText)
              .orElseThrow(() -> new IllegalStateException("Unnamed constant: " + constant));
      var comment =
          tree(constant)
              .filter(tokens(TokenTypes.BLOCK_COMMENT_BEGIN))
              .findFirst()
              .orElse(JavaDocNode.EMPTY_AST);
      var field =
          new FieldDoc(context, createVirtualMember(name, TokenTypes.VARIABLE_DEF), comment);
      addField(field);
    }
  }

  private static DetailAstImpl createVirtualMember(String name, int tokenType) {
    var publicMod = new DetailAstImpl();
    publicMod.initialize(
        TokenTypes.LITERAL_PUBLIC, TokenUtil.getTokenName(TokenTypes.LITERAL_PUBLIC));
    var modifiers = new DetailAstImpl();
    modifiers.initialize(TokenTypes.MODIFIERS, TokenUtil.getTokenName(tokenType));
    modifiers.addChild(publicMod);
    var memberName = new DetailAstImpl();
    memberName.initialize(TokenTypes.IDENT, name);
    var member = new DetailAstImpl();
    member.initialize(tokenType, TokenUtil.getTokenName(tokenType));
    memberName.addChild(modifiers);
    member.addChild(memberName);
    return member;
  }

  public void addMethod(MethodDoc method) {
    this.methods.put(toMethodSignature(method), method);
  }

  public void addScript(String pattern, MethodDoc method) {
    this.scripts.put(pattern, method);
  }

  public void addField(FieldDoc field) {
    this.fields.put(field.getName(), field);
  }

  public Optional<FieldDoc> getField(String name) {
    return Optional.ofNullable(fields.get(name));
  }

  public Optional<MethodDoc> getMethod(String name, List<String> parameterNames) {
    return Optional.ofNullable(methods.get(toMethodSignature(name, parameterNames)));
  }

  public Optional<MethodDoc> getScript(String pattern) {
    return Optional.ofNullable(scripts.get(pattern));
  }

  private String toMethodSignature(MethodDoc method) {
    return toMethodSignature(method.getName(), method.getParameterNames());
  }

  private String toMethodSignature(String methodName, List<String> parameterNames) {
    return methodName + parameterNames.stream().collect(Collectors.joining(", ", "(", ")"));
  }

  public String getSimpleName() {
    return getSimpleName(node);
  }

  protected String getSimpleName(DetailAST node) {
    return node.findFirstToken(TokenTypes.IDENT).getText();
  }

  public String getName() {
    var classScope =
        Stream.concat(
                Stream.of(node),
                backward(node)
                    .filter(
                        tokens(
                            TokenTypes.CLASS_DEF,
                            TokenTypes.INTERFACE_DEF,
                            TokenTypes.ENUM_DEF,
                            TokenTypes.RECORD_DEF)))
            .map(this::getSimpleName)
            .toList();
    var packageScope =
        backward(node)
            .filter(tokens(TokenTypes.COMPILATION_UNIT))
            .findFirst()
            .flatMap(it -> tree(it).filter(tokens(TokenTypes.PACKAGE_DEF)).findFirst())
            .map(it -> tree(it).filter(tokens(TokenTypes.IDENT)).map(DetailAST::getText).toList())
            .orElse(List.of());
    return Stream.concat(packageScope.stream(), classScope.stream())
        .collect(Collectors.joining("."));
  }

  public boolean isRecord() {
    return tree(node).anyMatch(tokens(TokenTypes.RECORD_DEF));
  }

  public boolean isEnum() {
    return tree(node).anyMatch(tokens(TokenTypes.ENUM_DEF));
  }

  public String getPropertyDoc(String name) {
    var getterDoc =
        Stream.of(name, getterName(name))
            .map(n -> methods.get(toMethodSignature(n, List.of())))
            .filter(Objects::nonNull)
            .findFirst()
            .map(MethodDoc::getText)
            .orElse(null);
    if (getterDoc == null) {
      var field = fields.get(name);
      return field == null ? null : field.getText();
    }
    return getterDoc;
  }

  private String getterName(String name) {
    return "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }

  @Override
  public String toString() {
    return "fields: "
        + String.join(", ", fields.keySet())
        + "\nmethods: "
        + String.join(", ", methods.keySet());
  }
}
