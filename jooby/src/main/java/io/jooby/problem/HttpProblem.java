/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.problem;

import java.net.URI;
import java.time.Instant;
import java.util.*;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.StatusCode;

/**
 * Representation of <a href="https://www.rfc-editor.org/rfc/rfc7807">RFC7807</a> <a
 * href="https://www.rfc-editor.org/rfc/rfc9457">RFC9457</a> Problem schema.
 *
 * @author kliushnichenko
 * @since 3.4.2
 */
public class HttpProblem extends RuntimeException {

  private static final String INTERNAL_ERROR_TITLE = StatusCode.SERVER_ERROR.reason();
  private static final String INTERNAL_ERROR_DETAIL =
      "The server encountered an error or misconfiguration and was unable to complete your request";

  private static final URI DEFAULT_TYPE = URI.create("about:blank");

  private final String timestamp;
  private final URI type;
  private final String title;
  private final int status;
  private final String detail;
  private final URI instance;
  private final List<Error> errors;
  private final Map<String, Object> parameters;
  private final Map<String, Object> headers;

  protected HttpProblem(Builder builder) {
    super(createMessage(builder.title, builder.detail));

    this.timestamp = Instant.now().toString();
    this.type = builder.type == null ? DEFAULT_TYPE : builder.type;
    this.title = builder.title;
    this.status = builder.status;
    this.detail = builder.detail;
    this.instance = builder.instance;
    this.errors = Collections.unmodifiableList(builder.errors);
    this.parameters = Collections.unmodifiableMap(builder.parameters);
    this.headers = Collections.unmodifiableMap(builder.headers);
  }

  private static String createMessage(String title, String detail) {
    return Objects.isNull(detail) ? title : title + ": " + detail;
  }

  /**
   * Creates an HttpProblem with the specified status code and custom title.
   *
   * @param status the HTTP status code for this problem
   * @param title  the title describing the type of problem
   * @return an HttpProblem instance with the specified status and title
   */
  public static HttpProblem valueOf(StatusCode status, String title) {
    return builder().title(title).status(status).build();
  }

  /**
   * Creates an HttpProblem with the specified status code, custom title, and detail.
   *
   * @param status the HTTP status code for this problem
   * @param title  the title describing the type of problem
   * @param detail a human-readable explanation specific to this occurrence of the problem
   * @return an HttpProblem instance with the specified status, title, and detail
   */
  public static HttpProblem valueOf(StatusCode status, String title, String detail) {
    return builder().title(title).status(status).detail(detail).build();
  }

  /**
   * Creates an HttpProblem with the specified status code using the default title.
   * The title is automatically set to the status code's reason phrase.
   *
   * @param status the HTTP status code for this problem
   * @return an HttpProblem instance with the specified status and default title
   */
  public static HttpProblem valueOf(StatusCode status) {
    return builder().title(status.reason()).status(status).build();
  }

  /**
   * Creates an HttpProblem with BAD_REQUEST status (400) and custom title and detail.
   *
   * @param title  the title describing the type of problem
   * @param detail a human-readable explanation specific to this occurrence of the problem
   * @return an HttpProblem instance with BAD_REQUEST status
   */
  public static HttpProblem badRequest(String title, String detail) {
    return builder()
        .title(title)
        .status(StatusCode.BAD_REQUEST)
        .detail(detail)
        .build();
  }

  /**
   * Creates an HttpProblem with BAD_REQUEST status (400) using the default title.
   * The title is automatically set to the status code's reason phrase.
   *
   * @param detail a human-readable explanation specific to this occurrence of the problem
   * @return an HttpProblem instance with BAD_REQUEST status
   */
  public static HttpProblem badRequest(String detail) {
    return builder()
        .title(StatusCode.BAD_REQUEST.reason())
        .status(StatusCode.BAD_REQUEST)
        .detail(detail)
        .build();
  }

  /**
   * Creates an HttpProblem with NOT_FOUND status (404) and custom title and detail.
   *
   * @param title  the title describing the type of problem
   * @param detail a human-readable explanation specific to this occurrence of the problem
   * @return an HttpProblem instance with NOT_FOUND status
   */
  public static HttpProblem notFound(String title, String detail) {
    return builder()
        .title(title)
        .status(StatusCode.NOT_FOUND)
        .detail(detail)
        .build();
  }

  /**
   * Creates an HttpProblem with NOT_FOUND status (404) using the default title.
   * The title is automatically set to the status code's reason phrase.
   *
   * @param detail a human-readable explanation specific to this occurrence of the problem
   * @return an HttpProblem instance with NOT_FOUND status
   */
  public static HttpProblem notFound(String detail) {
    return builder()
        .title(StatusCode.NOT_FOUND.reason())
        .status(StatusCode.NOT_FOUND)
        .detail(detail)
        .build();
  }

  /**
   * Creates an HttpProblem with UNPROCESSABLE_ENTITY status (422) and custom title and detail.
   *
   * @param title  the title describing the type of problem
   * @param detail a human-readable explanation specific to this occurrence of the problem
   * @return an HttpProblem instance with UNPROCESSABLE_ENTITY status
   */
  public static HttpProblem unprocessableEntity(String title, String detail) {
    return builder()
        .title(title)
        .status(StatusCode.UNPROCESSABLE_ENTITY)
        .detail(detail)
        .build();
  }

  /**
   * Creates an HttpProblem with UNPROCESSABLE_ENTITY status (422) using the default title.
   * The title is automatically set to the status code's reason phrase.
   *
   * @param detail a human-readable explanation specific to this occurrence of the problem
   * @return an HttpProblem instance with UNPROCESSABLE_ENTITY status
   */
  public static HttpProblem unprocessableEntity(String detail) {
    return builder()
        .title(StatusCode.UNPROCESSABLE_ENTITY.reason())
        .status(StatusCode.UNPROCESSABLE_ENTITY)
        .detail(detail)
        .build();
  }

  /**
   * Creates an HttpProblem with SERVER_ERROR status (500) using predefined title and detail.
   * This is typically used for unexpected server errors or misconfigurations.
   *
   * @return an HttpProblem instance with SERVER_ERROR status and predefined error message
   */
  public static HttpProblem internalServerError() {
    return builder()
        .title(INTERNAL_ERROR_TITLE)
        .status(StatusCode.SERVER_ERROR)
        .detail(INTERNAL_ERROR_DETAIL)
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * A problem occurrence timestamp in ISO-8601 representation.
   *
   * @return a timestamp when problem has been arisen
   */
  public String getTimestamp() {
    return timestamp;
  }

  /**
   * A URI reference that identifies the problem type. Consumers MUST use the "type" URI (after
   * resolution, if necessary) as the problem type's primary identifier. When this member is not
   * present, its value is assumed to be "about:blank". If the type URI is a locator (e.g., those
   * with a "http" or "https" scheme), de-referencing it SHOULD provide human-readable documentation
   * for the problem type (e.g., using HTML). However, consumers SHOULD NOT automatically
   * dereference the type URI, unless they do so when providing information to developers (e.g.,
   * when a debugging tool is in use)
   *
   * @return a URI that identifies this problem's type
   */
  public URI getType() {
    return type;
  }

  /**
   * A short, human-readable summary of the problem type. It SHOULD NOT change from occurrence to
   * occurrence of the problem, except for purposes of localization.
   *
   * @return a short, human-readable summary of this problem
   */
  public String getTitle() {
    return title;
  }

  /**
   * The HTTP status code generated by the origin server for this occurrence of the problem.
   *
   * @return the HTTP status code
   */
  public int getStatus() {
    return status;
  }

  /**
   * A human-readable explanation specific to this occurrence of the problem.
   *
   * @return A human-readable explanation of this problem
   */
  public @Nullable String getDetail() {
    return detail;
  }

  /**
   * A URI that identifies the specific occurrence of the problem. It may or may not yield further
   * information if de-referenced.
   *
   * @return an absolute URI that identifies this specific problem
   */
  public @Nullable URI getInstance() {
    return instance;
  }

  /**
   * Optional, additional attributes of the problem.
   *
   * @return additional parameters
   */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public Map<String, Object> getHeaders() {
    return this.headers;
  }

  public List<Error> getErrors() {
    return this.errors;
  }

  public boolean hasParameters() {
    return !parameters.isEmpty();
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  @Override
  public String toString() {
    return "HttpProblem{"
        + "timestamp='"
        + timestamp
        + '\''
        + ", type="
        + type
        + ", title='"
        + title
        + '\''
        + ", status="
        + status
        + ", detail='"
        + detail
        + '\''
        + ", instance="
        + instance
        + (hasErrors() ? ", errors=" + errors : "")
        + (hasParameters() ? ", parameters=" + parameters : "")
        + '}';
  }

  public static class Builder {
    private URI type;
    private String title;
    private int status;
    private String detail;
    private URI instance;
    private final Map<String, Object> parameters = new LinkedHashMap<>();
    private final Map<String, Object> headers = new LinkedHashMap<>();
    private final List<Error> errors = new LinkedList<>();

    Builder() {
    }

    public Builder type(@Nullable final URI type) {
      this.type = type;
      return this;
    }

    public Builder title(final String title) {
      this.title = title;
      return this;
    }

    public Builder status(final StatusCode status) {
      this.status = status.value();
      return this;
    }

    public Builder detail(@Nullable final String detail) {
      this.detail = detail;
      return this;
    }

    public Builder instance(@Nullable final URI instance) {
      this.instance = instance;
      return this;
    }

    public Builder header(String headerName, Object value) {
      headers.put(headerName, value);
      return this;
    }

    public Builder error(final Error error) {
      errors.add(error);
      return this;
    }

    public Builder errors(final List<? extends Error> errors) {
      this.errors.addAll(errors);
      return this;
    }

    /**
     * @param key   additional info parameter name
     * @param value additional info parameter value
     * @return this for chaining
     */
    public Builder param(final String key, @Nullable final Object value) {
      parameters.put(key, value);
      return this;
    }

    public HttpProblem build() {
      if (this.title == null || this.title.isEmpty() || this.title.isBlank()) {
        throw new RuntimeException("The problem 'title' should be specified");
      }
      if (this.status == 0) {
        throw new RuntimeException("The problem 'status' should be specified");
      }
      if (this.status < 400) {
        throw new RuntimeException(
            "Illegal status code "
                + this.status
                + ". "
                + "Problem details designed to serve 4xx and 5xx status codes");
      }
      return new HttpProblem(this);
    }
  }

  /**
   * Represents an individual error within an HTTP Problem response.
   * This class encapsulates error details following RFC 7807/RFC 9457 standards
   * for providing structured error information in API responses.
   */
  public static class Error {
    private final String detail;
    private final String pointer;

    /**
     * Creates a new Error instance with the specified detail and pointer.
     *
     * @param detail  a human-readable explanation of the specific error
     * @param pointer a JSON Pointer (RFC 6901) that identifies the location
     *                in the request document where the error occurred
     */
    public Error(String detail, String pointer) {
      this.detail = detail;
      this.pointer = pointer;
    }

    /**
     * Returns the human-readable explanation of this specific error.
     *
     * @return the error detail message
     */
    public String getDetail() {
      return detail;
    }

    /**
     * Returns a reference that identifies the location where this error occurred.
     * This is typically a JSON Pointer (RFC 6901).
     *
     * @return the pointer to the location of the error
     */
    public String getPointer() {
      return pointer;
    }
  }
}
