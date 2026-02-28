/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jooby.Router;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;

public class OpenAPIExt extends OpenAPI {
  @JsonIgnore private List<OperationExt> operations = Collections.emptyList();

  @JsonIgnore private String source;

  public List<OperationExt> getOperations() {
    return operations;
  }

  public void setOperations(List<OperationExt> operations) {
    this.operations = operations;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String classname) {
    this.source = classname;
  }

  public void addSecuritySchemes(String name, SecurityScheme scheme) {
    getRequiredComponents().addSecuritySchemes(name, scheme);
  }

  @JsonIgnore
  public Components getRequiredComponents() {
    if (getComponents() == null) {
      setComponents(new Components());
    }
    return getComponents();
  }

  @JsonIgnore
  public Map<String, Schema> getRequiredSchemas() {
    var components = getRequiredComponents();
    if (components.getSchemas() == null) {
      components.setSchemas(new LinkedHashMap<>());
    }
    return components.getSchemas();
  }

  @Override
  public void setPaths(Paths paths) {
    var existingPaths = this.getPaths();
    if (existingPaths != null && !existingPaths.isEmpty()) {
      var mergePolicy =
          MergePolicy.parse(
              existingPaths.getExtensions(),
              MergePolicy.parse(getExtensions(), MergePolicy.IGNORE));
      super.setPaths(mergePaths(existingPaths, paths, mergePolicy));
    } else {
      super.setPaths(paths);
    }
  }

  private Paths mergePaths(Paths docPaths, Paths paths, MergePolicy mergePolicy) {
    for (var e : docPaths.entrySet()) {
      var pattern = e.getKey();
      var path = paths.get(pattern);
      if (path != null) {
        // Copy into generated path
        var docPath = e.getValue();
        setProperty(docPath, PathItem::getSummary, path, PathItem::setSummary);
        setProperty(docPath, PathItem::getDescription, path, PathItem::setDescription);
        setProperty(docPath, PathItem::getServers, path, PathItem::setServers);
        setProperty(docPath, PathItem::getParameters, path, PathItem::setParameters);
        setProperty(docPath, PathItem::get$ref, path, PathItem::set$ref);
        setProperty(docPath, PathItem::getExtensions, path, PathItem::setExtensions);
        // Operation
        mergeOperation(
            Router.GET, pattern, docPath.getGet(), path.getGet(), mergePolicy, path::setGet);
        mergeOperation(
            Router.POST, pattern, docPath.getPost(), path.getPost(), mergePolicy, path::setPost);
        mergeOperation(
            Router.PUT, pattern, docPath.getPut(), path.getPut(), mergePolicy, path::setPut);
        mergeOperation(
            Router.PATCH,
            pattern,
            docPath.getPatch(),
            path.getPatch(),
            mergePolicy,
            path::setPatch);
        mergeOperation(
            Router.DELETE,
            pattern,
            docPath.getDelete(),
            path.getDelete(),
            mergePolicy,
            path::setDelete);
        mergeOperation(
            Router.HEAD, pattern, docPath.getHead(), path.getHead(), mergePolicy, path::setHead);
        mergeOperation(
            Router.OPTIONS,
            pattern,
            docPath.getOptions(),
            path.getOptions(),
            mergePolicy,
            path::setOptions);
        mergeOperation(
            Router.TRACE,
            pattern,
            docPath.getTrace(),
            path.getTrace(),
            mergePolicy,
            path::setTrace);
      } else if (mergePolicy.handle("Unknown path: \"" + pattern + "\"")) {
        var newOperation = e.getValue();
        clearMergePolicy(newOperation, PathItem::getExtensions, PathItem::setExtensions);
        clearMergePolicy(newOperation.getGet(), Operation::getExtensions, Operation::setExtensions);
        clearMergePolicy(
            newOperation.getPost(), Operation::getExtensions, Operation::setExtensions);
        clearMergePolicy(newOperation.getPut(), Operation::getExtensions, Operation::setExtensions);
        clearMergePolicy(
            newOperation.getPatch(), Operation::getExtensions, Operation::setExtensions);
        clearMergePolicy(
            newOperation.getDelete(), Operation::getExtensions, Operation::setExtensions);
        clearMergePolicy(
            newOperation.getHead(), Operation::getExtensions, Operation::setExtensions);
        clearMergePolicy(
            newOperation.getOptions(), Operation::getExtensions, Operation::setExtensions);
        clearMergePolicy(
            newOperation.getTrace(), Operation::getExtensions, Operation::setExtensions);
        paths.put(e.getKey(), newOperation);
      }
    }
    return paths;
  }

  private <T> void clearMergePolicy(
      T src, Function<T, Map<String, Object>> getter, BiConsumer<T, Map<String, Object>> setter) {
    if (src != null) {
      Map<String, Object> extensions = getter.apply(src);
      if (extensions != null) {
        extensions.remove("x-merge-policy");
        if (extensions.isEmpty()) {
          extensions = null;
        }
        setter.accept(src, extensions);
      }
    }
  }

  private void mergeOperation(
      String method,
      String pattern,
      Operation src,
      Operation target,
      MergePolicy defaultMergePolicy,
      Consumer<Operation> appender) {
    if (src != null) {
      MergePolicy mergePolicy = MergePolicy.parse(src.getExtensions(), defaultMergePolicy);
      if (target != null) {
        setProperty(src, Operation::getTags, target, Operation::setTags);
        setProperty(src, Operation::getSummary, target, Operation::setSummary);
        setProperty(src, Operation::getDescription, target, Operation::setDescription);
        setProperty(src, Operation::getExternalDocs, target, Operation::setExternalDocs);
        setProperty(src, Operation::getOperationId, target, Operation::setOperationId);
        setProperty(src, Operation::getRequestBody, target, Operation::setRequestBody);
        setProperty(src, Operation::getResponses, target, Operation::setResponses);
        setProperty(src, Operation::getCallbacks, target, Operation::setCallbacks);
        setProperty(src, Operation::getDeprecated, target, Operation::setDeprecated);
        setProperty(src, Operation::getSecurity, target, Operation::setSecurity);
        setProperty(src, Operation::getServers, target, Operation::setServers);
        setProperty(src, Operation::getExtensions, target, Operation::setExtensions);

        // Parameter are sync in next line:
        // setProperty(src, Operation::getParameters, target, Operation::setParameters);
        var srcParameters =
            Optional.ofNullable(src.getParameters()).orElseGet(List::of).stream()
                .filter(Objects::nonNull)
                .toList();
        var targetParameters =
            Optional.ofNullable(target.getParameters()).orElseGet(List::of).stream()
                .filter(Objects::nonNull)
                .toList();
        for (var srcParameter : srcParameters) {
          targetParameters.stream()
              .filter(it -> it.getName().equals(srcParameter.getName()))
              .findFirst()
              .ifPresent(targetParameter -> mergeParameter(srcParameter, targetParameter));
        }
      } else if (mergePolicy.handle("Operation not found: " + method + " " + pattern)) {
        appender.accept(src);
      }
    }
  }

  private void mergeParameter(Parameter src, Parameter target) {
    setProperty(src, Parameter::getIn, target, Parameter::setIn);
    setProperty(src, Parameter::getDescription, target, Parameter::setDescription);
    setProperty(src, Parameter::getRequired, target, Parameter::setRequired);
    setProperty(src, Parameter::getDeprecated, target, Parameter::setDeprecated);
    setProperty(src, Parameter::getAllowEmptyValue, target, Parameter::setAllowEmptyValue);
    setProperty(src, Parameter::get$ref, target, Parameter::set$ref);
    setProperty(src, Parameter::getStyle, target, Parameter::setStyle);
    setProperty(src, Parameter::getExplode, target, Parameter::setExplode);
    setProperty(src, Parameter::getAllowReserved, target, Parameter::setAllowReserved);
    setProperty(src, Parameter::getSchema, target, Parameter::setSchema);
    setProperty(src, Parameter::getExamples, target, Parameter::setExamples);
    setProperty(src, Parameter::getExample, target, Parameter::setExample);
    setProperty(src, Parameter::getContent, target, Parameter::setContent);
    setProperty(src, Parameter::getExtensions, target, Parameter::setExtensions);
  }

  private <S, V> void setProperty(S src, Function<S, V> getter, S target, BiConsumer<S, V> setter) {
    var value = getter.apply(src);
    // Copy only non-null values
    if (value != null) {
      if (value instanceof Collection<?> collection) {
        // non-empty
        if (!collection.isEmpty()) {
          setter.accept(target, value);
        }
      } else if (value instanceof Map<?, ?> map) {
        // non-empty
        if (!map.isEmpty()) {
          setter.accept(target, value);
        }
      } else if (value instanceof CharSequence string) {
        // non-empty
        if (!string.isEmpty()) {
          setter.accept(target, value);
        }
      } else {
        setter.accept(target, value);
      }
    }
  }

  public OperationExt findOperation(String method, String pattern) {
    Predicate<OperationExt> filter = op -> op.getPath().equals(pattern);
    filter = filter.and(op -> op.getMethod().equals(method));
    return getOperations().stream()
        .filter(filter)
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Operation not found: " + method + " " + pattern));
  }

  public List<OperationExt> findOperationByTag(String tag) {
    return getOperations().stream().filter(it -> it.isOnTag(tag)).toList();
  }
}
