/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.htmx;

import static io.jooby.internal.apt.CodeBlock.*;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import io.jooby.internal.apt.AnnotationSupport;
import io.jooby.internal.apt.CodeBlock;
import io.jooby.internal.apt.WebRoute;

public class HtmxRoute extends WebRoute<HtmxRouter> {

  private final TypeElement httpMethodAnnotation;
  private String generatedName;

  public HtmxRoute(HtmxRouter router, ExecutableElement method, TypeElement httpMethodAnnotation) {
    super(router, method);
    this.httpMethodAnnotation = httpMethodAnnotation;
    this.generatedName = method.getSimpleName().toString();
  }

  public String getGeneratedName() {
    return generatedName;
  }

  public void setGeneratedName(String generatedName) {
    this.generatedName = generatedName;
  }

  public List<String> generateHandlerCall(boolean kt) {
    var buffer = new ArrayList<String>();
    var methodName = getGeneratedName();
    var paramList = new StringJoiner(", ", "(", ")");

    int indent = 2;

    // 1. Method Signature
    if (kt) {
      buffer.add(statement("fun ", methodName, "(ctx: io.jooby.Context): Any {"));
    } else {
      buffer.add(
          statement("public Object ", methodName, "(io.jooby.Context ctx) throws Exception {"));
    }

    // 2. Parameter Extraction
    for (var parameter : getParameters(true)) {
      // Check if parameter is our HtmxContext!
      if (parameter.getType().getRawType().toString().equals("io.jooby.htmx.HtmxContext")) {
        paramList.add((kt ? "" : "new ") + "io.jooby.htmx.HtmxContext(ctx)");
        continue;
      }

      var generatedParameter = parameter.generateMapping(kt);
      if (parameter.isRequireBeanValidation()) {
        generatedParameter =
            CodeBlock.of("io.jooby.validation.BeanValidator.apply(ctx, ", generatedParameter, ")");
      }
      paramList.add(generatedParameter);
    }

    // Fetch Controller Instance
    buffer.add(statement(indent(indent), var(kt), "c = this.factory.apply(ctx)", semicolon(kt)));

    // 3. Extract Annotation Metadata
    var hxView = AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.htmx.HxView");
    var hxError = findHxError();
    String primaryView =
        hxView != null
            ? AnnotationSupport.findAnnotationValue(hxView, "value"::equals).stream()
                .findFirst()
                .orElse(null)
            : null;
    String errorView =
        hxError != null
            ? AnnotationSupport.findAnnotationValue(hxError, "value"::equals).stream()
                .findFirst()
                .orElse(null)
            : null;
    String errorTarget =
        hxError != null
            ? AnnotationSupport.findAnnotationValue(hxError, "target"::equals).stream()
                .findFirst()
                .orElse(null)
            : null;

    // Strip quotes from APT extraction so string() works correctly below
    if (errorView != null) {
      errorView = errorView.replace("\"", "");
    }

    boolean isDynamicResponse =
        getReturnType().getRawType().toString().equals("io.jooby.htmx.HtmxResponse");
    String call = makeCall(kt, paramList.toString(), false, false);

    // 4. Controller Invocation (with Try/Catch if errorView is present)
    if (errorView != null) {
      buffer.add(statement(indent(indent), "try {"));
      indent += 2;
    }

    buffer.add(statement(indent(indent), var(kt), "result_ = ", call, semicolon(kt)));

    appendDeclarativeHeaders(buffer, kt, indent);

    // 5. Response Processing
    if (isDynamicResponse) {
      if (errorView != null) {
        // USE IDIOMATIC KOTLIN MAPS
        String emptyMap = kt ? "mapOf<String, Any>()" : "java.util.Map.of()";
        buffer.add(
            statement(
                indent(indent),
                "result_.addOob(",
                string(errorView),
                ", ",
                emptyMap,
                ")",
                semicolon(kt)));
      }
      buffer.add(statement(indent(indent), "return result_.send(ctx)", semicolon(kt)));
    } else {
      generateModelAndViewReturn(
          buffer, kt, indent, string(primaryView).toString(), "result_", errorView);
    }

    // 6. Error Handling block
    if (errorView != null) {
      generateErrorCatchBlock(buffer, kt, indent - 2, errorView, errorTarget);
    }

    buffer.add(statement("}", System.lineSeparator()));
    return buffer;
  }

  private AnnotationMirror findHxError() {
    var hxError =
        AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.htmx.HxError");
    if (hxError == null) {
      return AnnotationSupport.findAnnotationByName(
          method.getEnclosingElement(), "io.jooby.annotation.htmx.HxError");
    }
    return hxError;
  }

  private void generateErrorCatchBlock(
      List<String> buffer, boolean kt, int indent, String errorView, String errorTarget) {
    if (kt) {
      buffer.add(statement(indent(indent), "} catch (ex: Exception) {"));
    } else {
      buffer.add(statement(indent(indent), "} catch (Exception ex) {"));
    }

    buffer.add(
        statement(
            indent(indent + 2),
            var(kt),
            "statusCode_ = ctx.getRouter().errorCode(ex)",
            semicolon(kt)));

    buffer.add(
        statement(
            indent(indent + 2),
            var(kt),
            "validationResult_ = ctx.require(io.jooby.validation.ValidationExceptionMapper",
            clazz(kt),
            ").toResult(statusCode_, ex)",
            semicolon(kt)));

    buffer.add(statement(indent(indent + 2), "if (validationResult_ == null) {"));
    buffer.add(statement(indent(indent + 4), "throw ex", semicolon(kt)));
    buffer.add(statement(indent(indent + 2), "}"));

    buffer.add(
        statement(
            indent(indent + 2),
            "ctx.setResponseCode(io.jooby.StatusCode.UNPROCESSABLE_ENTITY)",
            semicolon(kt)));

    if (errorTarget != null && !errorTarget.isEmpty()) {
      buffer.add(
          statement(
              indent(indent + 2),
              "ctx.setResponseHeader(\"HX-Retarget\", \"" + errorTarget + "\")",
              semicolon(kt)));
    }

    // USE IDIOMATIC KOTLIN MUTABLE MAPS
    if (kt) {
      buffer.add(
          statement(
              indent(indent + 2),
              var(kt),
              "errorModel_ = mutableMapOf<String, Any>()",
              semicolon(kt)));
    } else {
      buffer.add(
          statement(
              indent(indent + 2),
              "java.util.Map<String, Object> errorModel_ = new java.util.HashMap<>()",
              semicolon(kt)));
    }

    buffer.add(
        statement(
            indent(indent + 2),
            "errorModel_.put(\"validationResult\", validationResult_)",
            semicolon(kt)));
    var inferType = kt ? "<Any>" : "";
    buffer.add(
        statement(
            indent(indent + 2),
            "return io.jooby.ModelAndView.of",
            inferType,
            "(\"" + errorView + "\", errorModel_)",
            semicolon(kt)));

    buffer.add(statement(indent(indent), "}"));
  }

  public List<String> generateMapping(boolean kt, String routerName, boolean isLastRoute) {
    List<String> block = new ArrayList<>();
    var methodName = getGeneratedName();
    var returnType = getReturnType();
    var paramString = String.join(", ", getJavaMethodSignature(kt));
    var javadocLink = seeControllerMethodJavadoc(kt, routerName);
    var attributeGenerator =
        new io.jooby.internal.apt.RouteAttributesGenerator(context, hasBeanValidation);

    var dslMethod = httpMethodAnnotation.getSimpleName().toString().toLowerCase();
    var paths = context.path(router.getTargetType(), method, httpMethodAnnotation);

    for (var path : paths) {
      var lastLine = isLastRoute && paths.get(paths.size() - 1).equals(path);
      block.add(javadocLink);

      String handlerRef =
          kt
              ? (isSuspendFun() ? "{ ctx -> " + methodName + "(ctx) }" : "this::" + methodName)
              : "this::" + methodName;

      block.add(
          statement(
              isSuspendFun() ? "" : "app.",
              dslMethod,
              "(",
              string(leadingSlash(path)),
              ", ",
              handlerRef,
              ")"));

      if (context.nonBlocking(getReturnType().getRawType()) || isSuspendFun()) {
        block.add(statement(indent(2), ".setNonBlocking(true)"));
      }

      attributeGenerator
          .toSourceCode(kt, this, 2)
          .ifPresent(
              attributes -> block.add(statement(indent(2), ".setAttributes(", attributes, ")")));

      var lineSep =
          lastLine ? System.lineSeparator() : System.lineSeparator() + System.lineSeparator();

      if (context.generateMvcMethod()) {
        block.add(
            CodeBlock.of(
                indent(2),
                ".setMvcMethod(",
                kt ? "" : "new ",
                "io.jooby.Route.MvcMethod(",
                routerName,
                clazz(kt),
                ", ",
                string(getMethodName()),
                ", ",
                type(kt, returnType.getRawType().toString()),
                clazz(kt),
                paramString.isEmpty() ? "" : ", " + paramString,
                "))",
                semicolon(kt),
                lineSep));
      } else {
        var lastStatement = block.getLast();
        if (lastStatement.endsWith(System.lineSeparator())) {
          lastStatement =
              lastStatement.substring(0, lastStatement.length() - System.lineSeparator().length());
        }
        block.set(block.size() - 1, lastStatement + semicolon(kt) + lineSep);
      }
    }
    return block;
  }

  private void generateModelAndViewReturn(
      List<String> buffer,
      boolean kt,
      int indent,
      String viewStr,
      String modelStr,
      String errorView) {
    boolean isView =
        getReturnType().is("io.jooby.ModelAndView")
            || getReturnType().is("io.jooby.MapModelAndView")
            || getReturnType().is("io.jooby.htmx.HtmxModelAndView");

    if (isView) {
      buffer.add(statement(indent(indent), "return ", modelStr, semicolon(kt)));
      return;
    }

    var oobViews =
        extractRepeatableValues(
            "io.jooby.annotation.htmx.HxOob", "io.jooby.annotation.htmx.HxOobs");

    if (!oobViews.isEmpty() || errorView != null) {
      buffer.add(
          statement(
              indent(indent),
              var(kt),
              "mv_ = ",
              kt ? "" : "new ",
              "io.jooby.htmx.HtmxModelAndView(",
              viewStr,
              ", ",
              modelStr,
              ")",
              semicolon(kt)));

      for (var oobView : oobViews) {
        buffer.add(statement(indent(indent), "mv_.addOob(", string(oobView), ")", semicolon(kt)));
      }

      // MAGIC REPAIRED: Add the empty map parameter correctly!
      if (errorView != null) {
        String emptyMap = kt ? "mapOf<String, Any>()" : "java.util.Map.of()";
        buffer.add(
            statement(
                indent(indent),
                "mv_.addOob(",
                string(errorView),
                ", ",
                emptyMap,
                ")",
                semicolon(kt)));
      }

      buffer.add(statement(indent(indent), "return mv_", semicolon(kt)));
      return;
    }

    var inferType = kt ? "<Any>" : "";
    buffer.add(
        statement(
            indent(indent),
            "return io.jooby.ModelAndView.of",
            inferType,
            "(",
            viewStr,
            ", ",
            modelStr,
            ")",
            semicolon(kt)));
  }

  private void appendDeclarativeHeaders(List<String> buffer, boolean kt, int indent) {
    writeStringHeader(buffer, kt, indent, "io.jooby.annotation.htmx.HxTarget", "HX-Retarget");
    writeStringHeader(buffer, kt, indent, "io.jooby.annotation.htmx.HxSwap", "HX-Reswap");
    writeStringHeader(buffer, kt, indent, "io.jooby.annotation.htmx.HxPushUrl", "HX-Push-Url");
    writeStringHeader(buffer, kt, indent, "io.jooby.annotation.htmx.HxRedirect", "HX-Redirect");

    if (AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.htmx.HxRefresh")
        != null) {
      buffer.add(
          statement(
              indent(indent),
              "ctx.setResponseHeader(",
              string("HX-Refresh"),
              ", true)",
              semicolon(kt)));
    }

    List<String> triggers =
        extractRepeatableValues(
            "io.jooby.annotation.htmx.HxTrigger", "io.jooby.annotation.htmx.HxTriggers");

    if (!triggers.isEmpty()) {
      String combinedTriggers = String.join(", ", triggers);
      buffer.add(
          statement(
              indent(indent),
              "ctx.setResponseHeader(",
              string("HX-Trigger"),
              ", ",
              string(combinedTriggers),
              ")",
              semicolon(kt)));
    }
  }

  private void writeStringHeader(
      List<String> buffer, boolean kt, int indent, String annotationFqn, String headerName) {
    var annotation = AnnotationSupport.findAnnotationByName(method, annotationFqn);
    if (annotation != null) {
      String value =
          AnnotationSupport.findAnnotationValue(annotation, "value"::equals).stream()
              .findFirst()
              .orElse("");
      value = value.replace("\"", "");

      if (!value.isEmpty()) {
        buffer.add(
            statement(
                indent(indent),
                "ctx.setResponseHeader(",
                string(headerName),
                ", ",
                string(value),
                ")",
                semicolon(kt)));
      }
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> extractRepeatableValues(
      String singleAnnotationFqn, String containerAnnotationFqn) {
    List<String> values = new ArrayList<>();

    var singleMirror = AnnotationSupport.findAnnotationByName(method, singleAnnotationFqn);
    if (singleMirror != null) {
      AnnotationSupport.findAnnotationValue(singleMirror, "value"::equals).stream()
          .map(Object::toString)
          .map(s -> s.replace("\"", ""))
          .findFirst()
          .ifPresent(values::add);
    }

    var containerMirror = AnnotationSupport.findAnnotationByName(method, containerAnnotationFqn);
    if (containerMirror != null) {
      for (var entry : containerMirror.getElementValues().entrySet()) {
        if (entry.getKey().getSimpleName().contentEquals("value")) {
          var nestedList =
              (java.util.List<? extends javax.lang.model.element.AnnotationValue>)
                  entry.getValue().getValue();

          for (var nestedItem : nestedList) {
            if (nestedItem.getValue()
                instanceof javax.lang.model.element.AnnotationMirror nestedMirror) {
              AnnotationSupport.findAnnotationValue(nestedMirror, "value"::equals).stream()
                  .map(Object::toString)
                  .map(s -> s.replace("\"", ""))
                  .findFirst()
                  .ifPresent(values::add);
            }
          }
        }
      }
    }

    return values;
  }
}
