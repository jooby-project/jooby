package io.jooby.internal.openapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedList;
import java.util.List;

public class OperationExt extends io.swagger.v3.oas.models.Operation {

  @JsonIgnore
  private final MethodNode node;
  @JsonIgnore
  private String method;
  @JsonIgnore
  private final String pattern;
  @JsonIgnore
  private Boolean hidden;
  @JsonIgnore
  private LinkedList<String> produces = new LinkedList<>();
  @JsonIgnore
  private LinkedList<String> consumes = new LinkedList<>();

  public OperationExt(MethodNode node, String method, String pattern, List arguments,
      List<ResponseExt> response) {
    this.node = node;
    this.method = method.toUpperCase();
    this.pattern = pattern;
    setParameters(arguments);
    super.setResponses(apiResponses(response));
  }

  private static ApiResponses apiResponses(List<ResponseExt> responses) {
    ApiResponses result = new ApiResponses();
    for (ResponseExt rsp : responses) {
      result.addApiResponse(rsp.getCode(), rsp);
    }
    return result;
  }

  public MethodNode getNode() {
    return node;
  }

  @JsonIgnore
  public ResponseExt getResponse() {
    return (ResponseExt) getResponses().getDefault();
  }

  public ResponseExt getResponse(String code) {
    return (ResponseExt) getResponses().get(code);
  }

  @Override public void setResponses(ApiResponses responses) {
    ResponseExt defrsp = getResponse();
    for (ApiResponse response : responses.values()) {
      ResponseExt rsp = (ResponseExt) response;
      if (rsp.getJavaTypes().size() == 0) {
        int code = statusCode(rsp.getCode());
        if (code > 100 && code < 400) {
          rsp.setJavaTypes(defrsp.getJavaTypes());
        }
      }
    }
    super.setResponses(responses);
  }

  private int statusCode(String code) {
    if (code == null || "default".equalsIgnoreCase(code)) {
      return StatusCode.OK_CODE;
    }
    try {
      return Integer.parseInt(code);
    } catch (NumberFormatException x) {
      return -1;
    }
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getPattern() {
    return pattern;
  }

  public List<String> getProduces() {
    return produces;
  }

  public List<String> getConsumes() {
    return consumes;
  }

  public void addProduces(String value) {
    produces.addFirst(toMediaType(value));
  }

  public void addConsumes(String value) {
    consumes.addFirst(toMediaType(value));
  }

  private String toMediaType(String value) {
    return MediaType.valueOf(value).toString();
  }

  public Boolean getHidden() {
    return hidden;
  }

  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }

  public String toString() {
    return getMethod() + " " + getPattern();
  }

  public Parameter getParameter(int i) {
    if (i < getParameters().size()) {
      return getParameters().get(i);
    }
    return null;
  }
}
