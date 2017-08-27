package org.jooby.apitool.raml;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import org.jooby.MediaType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RamlMethod {

  private final transient String name;

  private String displayName;

  private String description;

  private Map<String, RamlParameter> queryParameters;

  private Map<String, RamlParameter> formParameters;

  private Map<String, RamlParameter> headers;

  private RamlType body;

  private List<String> mediaType;

  private Map<Integer, RamlResponse> responses;

  public RamlMethod(String name) {
    this.name = name;
  }

  @JsonIgnore
  public RamlType getBody() {
    return body;
  }

  public void setBody(final RamlType body) {
    this.body = body;
  }

  public RamlMethod body(final RamlType body) {
    this.body = body;
    return this;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(final String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public Map<String, RamlParameter> getQueryParameters() {
    return queryParameters;
  }

  public void setQueryParameters(
      final Map<String, RamlParameter> queryParameters) {
    this.queryParameters = queryParameters;
  }

  public Map<String, RamlParameter> getHeaders() {
    return headers;
  }

  public void setHeaders(final Map<String, RamlParameter> headers) {
    this.headers = headers;
  }

  public Map<Integer, RamlResponse> getResponses() {
    return responses;
  }

  public void setResponses(final Map<Integer, RamlResponse> responses) {
    this.responses = responses;
  }

  @JsonIgnore
  public List<String> getMediaType() {
    return mediaType;
  }

  public void setMediaType(final List<String> mediaType) {
    this.mediaType = mediaType == null ? null : (mediaType.isEmpty() ? null : mediaType);
  }

  @JsonIgnore
  public Map<String, RamlParameter> getFormParameters() {
    return formParameters;
  }

  public void setFormParameters(
      final Map<String, RamlParameter> formParameters) {
    this.formParameters = formParameters;
  }

  public RamlParameter queryParameter(String name) {
    if (queryParameters == null) {
      queryParameters = new LinkedHashMap<>();
    }
    RamlParameter param = queryParameters.get(name);
    if (param == null) {
      param = new RamlParameter(name);
      queryParameters.put(name, param);
    }
    return param;
  }

  public RamlParameter formParameter(String name) {
    if (formParameters == null) {
      formParameters = new LinkedHashMap<>();
    }
    RamlParameter param = formParameters.get(name);
    if (param == null) {
      param = new RamlParameter(name);
      formParameters.put(name, param);
    }
    return param;
  }

  public RamlParameter headerParameter(String name) {
    if (headers == null) {
      headers = new LinkedHashMap<>();
    }
    RamlParameter param = headers.get(name);
    if (param == null) {
      param = new RamlParameter(name);
      headers.put(name, param);
    }
    return param;
  }

  public RamlResponse response(Integer status) {
    if (responses == null) {
      responses = new LinkedHashMap<>();
    }
    RamlResponse response = responses.get(status);
    if (response == null) {
      response = new RamlResponse();
      responses.put(status, response);
    }
    return response;
  }

  @JsonAnyGetter
  Map<String, Object> attributes() {
    Map<String, Object> attributes = new LinkedHashMap<>();
    Optional.ofNullable(formParameters).ifPresent(form -> {
      attributes.put("body",
          ImmutableMap.of(MediaType.multipart.name(), ImmutableMap.of("properties", form)));
    });
    Optional.ofNullable(body).ifPresent(body -> {
      if (mediaType != null) {
        attributes.put("body", mediaType.stream()
            .collect(Collectors.toMap(Function.identity(), it -> body.getRef())));
      } else {
        attributes.put("body", body.getRef());
      }
    });
    return attributes;
  }
}
