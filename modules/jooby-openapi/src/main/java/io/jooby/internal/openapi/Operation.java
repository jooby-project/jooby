package io.jooby.internal.openapi;

import io.jooby.StatusCode;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class Operation extends io.swagger.v3.oas.models.Operation {

  private final MethodNode node;
  private final String method;
  private final String pattern;
  private final List<Parameter> arguments;
  private Boolean hidden;

  public Operation(MethodNode node, String method, String pattern, List<Parameter> arguments,
      List<Response> response) {
    this.node = node;
    this.method = method.toUpperCase();
    this.pattern = pattern;
    this.arguments = arguments;
    super.setResponses(apiResponses(response));
  }

  private static ApiResponses apiResponses(List<Response> responses) {
    ApiResponses result = new ApiResponses();
    for (Response rsp : responses) {
      result.addApiResponse(rsp.getCode(), rsp);
    }
    return result;
  }

  public MethodNode getNode() {
    return node;
  }

  public List<Parameter> getArguments() {
    return arguments;
  }

  public Response getResponse() {
    return (Response) getResponses().getDefault();
  }

  public Response getResponse(String code) {
    return (Response) getResponses().get(code);
  }

  @Override public void setResponses(ApiResponses responses) {
    Response defrsp = getResponse();
    for (ApiResponse response : responses.values()) {
      Response rsp = (Response) response;
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

  public String getPattern() {
    return pattern;
  }

  //  public List<String> getProduces() {
  //    return produces;
  //  }
  //
  //  public List<String> getConsumes() {
  //    return consumes;
  //  }
  //
  //  public void addProduces(String value) {
  //    produces.addFirst(toMediaType(value));
  //  }
  //
  //  public void addConsumes(String value) {
  //    consumes.addFirst(toMediaType(value));
  //  }
  //
  //  private String toMediaType(String value) {
  //    return MediaType.valueOf(value).toString();
  //  }

  public Boolean getHidden() {
    return hidden;
  }

  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }

  public String toString() {
    return getMethod() + " " + getPattern();
  }
}
