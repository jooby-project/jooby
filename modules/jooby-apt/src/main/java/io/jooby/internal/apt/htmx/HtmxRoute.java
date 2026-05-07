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
    String layoutView =
        hxView != null
            ? AnnotationSupport.findAnnotationValue(hxView, "layout"::equals).stream()
                .findFirst()
                .orElse(null)
            : null;

    boolean isDynamicResponse =
        getReturnType().getRawType().toString().equals("io.jooby.htmx.HtmxResponse");
    String call = makeCall(kt, paramList.toString(), false, false);

    // 4. Controller Invocation (with Try/Catch if errorView is present)
    if (errorView != null) {
      buffer.add(statement(indent(indent), "try {"));
      indent += 2;
    }
    // 5. Response Processing
    if (isDynamicResponse) {
      // Guard for dynamic responses (e.g. POST/DELETE endpoints)
      buffer.add(
          statement(indent(indent), "if (!ctx.header(\"HX-Request\").booleanValue(false)) {"));
      if (kt) {
        buffer.add(
            statement(
                indent(indent + 2),
                "throw io.jooby.exception.BadRequestException(\"Direct browser access to this HTMX"
                    + " fragment is not allowed.\")"));
      } else {
        buffer.add(
            statement(
                indent(indent + 2),
                "throw new io.jooby.exception.BadRequestException(\"Direct browser access to this"
                    + " HTMX fragment is not allowed.\");"));
      }
      buffer.add(statement(indent(indent), "}"));

      buffer.add(statement(indent(indent), var(kt), "result_ = ", call, semicolon(kt)));

      if (errorView != null) {
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

      appendDeclarativeHeaders(buffer, kt, indent);

      buffer.add(statement(indent(indent), "return result_.send(ctx)", semicolon(kt)));
    } else {
      generateModelAndViewReturn(
          buffer, kt, indent, string(primaryView).toString(), call, errorView, layoutView);
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
      buffer.add(
          statement(indent(indent), "} catch (ex: io.jooby.htmx.HtmxDirectAccessException) {"));
      buffer.add(statement(indent(indent + 2), "throw ex"));
      buffer.add(statement(indent(indent), "} catch (ex: Exception) {"));
    } else {
      buffer.add(
          statement(indent(indent), "} catch (io.jooby.htmx.HtmxDirectAccessException ex) {"));
      buffer.add(statement(indent(indent + 2), "throw ex;"));
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
            "(",
            string(errorView),
            ", errorModel_)",
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
      String call,
      String errorView,
      String layoutView) {
    boolean isStandardView =
        getReturnType().is("io.jooby.ModelAndView")
            || getReturnType().is("io.jooby.MapModelAndView");
    boolean isHtmxView = getReturnType().is("io.jooby.htmx.HtmxModelAndView");
    boolean isView = isStandardView || isHtmxView;

    // Check if the developer explicitly added @HxView
    boolean hasHxView =
        io.jooby.internal.apt.AnnotationSupport.findAnnotationByName(
                method, "io.jooby.annotation.htmx.HxView")
            != null;

    // RULE: We apply the HTMX Guard Clause to EVERYTHING EXCEPT standard views lacking the @HxView
    // annotation.
    boolean requiresGuard = !isStandardView || hasHxView;

    var modelStr = "result_";

    // ==========================================
    // 1. THE BROWSER FULL-REFRESH GUARD
    // ==========================================
    if (requiresGuard) {
      buffer.add(
          statement(indent(indent), "if (!ctx.header(\"HX-Request\").booleanValue(false)) {"));
      if (layoutView != null && !layoutView.isEmpty()) {
        buffer.add(statement(indent(indent + 2), var(kt), "result_ = ", call, semicolon(kt)));

        // Inject the child view name as a request attribute (Safe for ANY model type: Map, Record,
        // POJO)
        buffer.add(
            statement(
                indent(indent + 2),
                "ctx.setAttribute(\"childView\", ",
                viewStr,
                ")",
                semicolon(kt)));

        // Extract the data model. If the controller returned a ModelAndView, unwrap it using
        // .getModel()
        String targetModel = isView ? modelStr + ".getModel()" : modelStr;

        // Return a BRAND NEW immutable ModelAndView pointing to the layout
        if (kt) {
          buffer.add(
              statement(
                  indent(indent + 2),
                  "return io.jooby.ModelAndView.of<Any>(",
                  string(layoutView),
                  ", ",
                  targetModel,
                  ")",
                  semicolon(kt)));
        } else {
          buffer.add(
              statement(
                  indent(indent + 2),
                  "return io.jooby.ModelAndView.of(",
                  string(layoutView),
                  ", ",
                  targetModel,
                  ")",
                  semicolon(kt)));
        }

      } else {
        // No layout defined: Reject direct access
        if (kt) {
          buffer.add(
              statement(
                  indent(indent + 2),
                  "throw io.jooby.htmx.HtmxDirectAccessException(\"Direct browser access to this"
                      + " HTMX fragment is not allowed.\")"));
        } else {
          buffer.add(
              statement(
                  indent(indent + 2),
                  "throw new io.jooby.htmx.HtmxDirectAccessException(\"Direct browser access to"
                      + " this HTMX fragment is not allowed.\");"));
        }
      }
      buffer.add(statement(indent(indent), "}"));
    }

    // Execute the controller method if it wasn't already handled and returned by the layout block
    // above
    buffer.add(statement(indent(indent), var(kt), "result_ = ", call, semicolon(kt)));

    appendDeclarativeHeaders(buffer, kt, indent);

    // ==========================================
    // 2. THE HTMX AJAX PIPELINE
    // ==========================================

    if (isView) {
      // Controller handled its own view creation
      buffer.add(statement(indent(indent), "return ", modelStr, semicolon(kt)));
      return;
    }

    var oobViews =
        extractRepeatableValues(
            "io.jooby.annotation.htmx.HxOob", "io.jooby.annotation.htmx.HxOobs");

    if (!oobViews.isEmpty() || errorView != null) {
      // Upgrade to HtmxModelAndView to support OOB responses
      if (kt) {
        buffer.add(
            statement(
                indent(indent),
                var(kt),
                "mv_ = io.jooby.htmx.HtmxModelAndView<Any>(",
                viewStr,
                ", ",
                modelStr,
                ")",
                semicolon(kt)));
      } else {
        buffer.add(
            statement(
                indent(indent),
                var(kt),
                "mv_ = new io.jooby.htmx.HtmxModelAndView<>(",
                viewStr,
                ", ",
                modelStr,
                ")",
                semicolon(kt)));
      }

      for (var oobView : oobViews) {
        buffer.add(statement(indent(indent), "mv_.addOob(", string(oobView), ")", semicolon(kt)));
      }

      if (errorView != null) {
        buffer.add(statement(indent(indent), "// clear error: ", errorView));
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

    // Fallback: Standard Jooby ModelAndView
    if (kt) {
      buffer.add(
          statement(
              indent(indent),
              "return io.jooby.ModelAndView.of<Any>(",
              viewStr,
              ", ",
              modelStr,
              ")",
              semicolon(kt)));
    } else {
      buffer.add(
          statement(
              indent(indent),
              "return io.jooby.ModelAndView.of(",
              viewStr,
              ", ",
              modelStr,
              ")",
              semicolon(kt)));
    }
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

    // NEW: Specialized trigger extraction
    appendTriggers(buffer, kt, indent);
  }

  private void appendTriggers(List<String> buffer, boolean kt, int indent) {
    // Use LinkedHashMap to ensure deterministic code generation order
    java.util.Map<String, List<String>> triggersByHeader = new java.util.LinkedHashMap<>();

    // 1. Process Single Annotation
    var singleMirror =
        AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.htmx.HxTrigger");
    if (singleMirror != null) {
      extractTriggerData(singleMirror, triggersByHeader);
    }

    // 2. Process Repeatable Container
    var containerMirror =
        AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.htmx.HxTriggers");
    if (containerMirror != null) {
      for (var entry : containerMirror.getElementValues().entrySet()) {
        if (entry.getKey().getSimpleName().contentEquals("value")) {
          var nestedList =
              (java.util.List<? extends javax.lang.model.element.AnnotationValue>)
                  entry.getValue().getValue();

          for (var nestedItem : nestedList) {
            if (nestedItem.getValue()
                instanceof javax.lang.model.element.AnnotationMirror nestedMirror) {
              extractTriggerData(nestedMirror, triggersByHeader);
            }
          }
        }
      }
    }

    // 3. Write out the grouped headers
    for (var entry : triggersByHeader.entrySet()) {
      String headerName = entry.getKey();
      String combinedValues = String.join(", ", entry.getValue());
      buffer.add(
          statement(
              indent(indent),
              "ctx.setResponseHeader(",
              string(headerName),
              ", ",
              string(combinedValues),
              ")",
              semicolon(kt)));
    }
  }

  private void extractTriggerData(
      AnnotationMirror mirror, java.util.Map<String, List<String>> map) {
    String eventName =
        AnnotationSupport.findAnnotationValue(mirror, "value"::equals).stream()
            .map(Object::toString)
            .findFirst()
            .orElse("");

    if (eventName.isEmpty()) return;

    // Default header if phase is omitted
    var headerName = "HX-Trigger";

    // Extract the phase enum if present
    var phaseValues = AnnotationSupport.findAnnotationValue(mirror, "phase"::equals);
    if (!phaseValues.isEmpty()) {
      var phaseRaw = phaseValues.getFirst();

      if (phaseRaw.endsWith("AFTER_SETTLE")) {
        headerName = "HX-Trigger-After-Settle";
      } else if (phaseRaw.endsWith("AFTER_SWAP")) {
        headerName = "HX-Trigger-After-Swap";
      }
    }

    map.computeIfAbsent(headerName, k -> new ArrayList<>()).add(eventName);
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
