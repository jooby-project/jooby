/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby;

/**
 * HTTP status codes.
 *
 * <p>
 * This code has been kindly borrowed from <a href="http://spring.io/">Spring</a>
 * </p>
 * .
 *
 * @author Arjen Poutsma
 * @see <a href="http://www.iana.org/assignments/http-status-codes">HTTP Status Code Registry</a>
 * @see <a href="http://en.wikipedia.org/wiki/List_of_HTTP_status_codes">List of HTTP status codes -
 *      Wikipedia</a>
 */
public enum Status {

  // 1xx Informational

  /**
   * {@code 100 Continue}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.1.1">HTTP/1.1</a>
   */
  CONTINUE(100, "Continue"),
  /**
   * {@code 101 Switching Protocols}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.1.2">HTTP/1.1</a>
   */
  SWITCHING_PROTOCOLS(101, "Switching Protocols"),
  /**
   * {@code 102 Processing}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2518#section-10.1">WebDAV</a>
   */
  PROCESSING(102, "Processing"),
  /**
   * {@code 103 Checkpoint}.
   *
   * @see <a href="http://code.google.com/p/gears/wiki/ResumableHttpRequestsProposal">A proposal for
   *      supporting resumable POST/PUT HTTP requests in HTTP/1.0</a>
   */
  CHECKPOINT(103, "Checkpoint"),

  // 2xx Success

  /**
   * {@code 200 OK}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.1">HTTP/1.1</a>
   */
  OK(200, "OK"),
  /**
   * {@code 201 Created}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.2">HTTP/1.1</a>
   */
  CREATED(201, "Created"),
  /**
   * {@code 202 Accepted}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.3">HTTP/1.1</a>
   */
  ACCEPTED(202, "Accepted"),
  /**
   * {@code 203 Non-Authoritative Information}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.4">HTTP/1.1</a>
   */
  NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
  /**
   * {@code 204 No Content}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.5">HTTP/1.1</a>
   */
  NO_CONTENT(204, "No Content"),
  /**
   * {@code 205 Reset Content}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.6">HTTP/1.1</a>
   */
  RESET_CONTENT(205, "Reset Content"),
  /**
   * {@code 206 Partial Content}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.7">HTTP/1.1</a>
   */
  PARTIAL_CONTENT(206, "Partial Content"),
  /**
   * {@code 207 Multi-Status}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-13">WebDAV</a>
   */
  MULTI_STATUS(207, "Multi-Status"),
  /**
   * {@code 208 Already Reported}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc5842#section-7.1">WebDAV Binding Extensions</a>
   */
  ALREADY_REPORTED(208, "Already Reported"),
  /**
   * {@code 226 IM Used}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc3229#section-10.4.1">Delta encoding in HTTP</a>
   */
  IM_USED(226, "IM Used"),

  // 3xx Redirection

  /**
   * {@code 300 Multiple Choices}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.1">HTTP/1.1</a>
   */
  MULTIPLE_CHOICES(300, "Multiple Choices"),
  /**
   * {@code 301 Moved Permanently}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.2">HTTP/1.1</a>
   */
  MOVED_PERMANENTLY(301, "Moved Permanently"),
  /**
   * {@code 302 Found}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.3">HTTP/1.1</a>
   */
  FOUND(302, "Found"),
  /**
   * {@code 303 See Other}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.4">HTTP/1.1</a>
   */
  SEE_OTHER(303, "See Other"),
  /**
   * {@code 304 Not Modified}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.5">HTTP/1.1</a>
   */
  NOT_MODIFIED(304, "Not Modified"),
  /**
   * {@code 305 Use Proxy}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.6">HTTP/1.1</a>
   */
  USE_PROXY(305, "Use Proxy"),
  /**
   * {@code 307 Temporary Redirect}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.8">HTTP/1.1</a>
   */
  TEMPORARY_REDIRECT(307, "Temporary Redirect"),
  /**
   * {@code 308 Resume Incomplete}.
   *
   * @see <a href="http://code.google.com/p/gears/wiki/ResumableHttpRequestsProposal">A proposal for
   *      supporting resumable POST/PUT HTTP requests in HTTP/1.0</a>
   */
  RESUME_INCOMPLETE(308, "Resume Incomplete"),

  // --- 4xx Client Error ---

  /**
   * {@code 400 Bad Request}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.1">HTTP/1.1</a>
   */
  BAD_REQUEST(400, "Bad Request"),
  /**
   * {@code 401 Unauthorized}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.2">HTTP/1.1</a>
   */
  UNAUTHORIZED(401, "Unauthorized"),
  /**
   * {@code 402 Payment Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.3">HTTP/1.1</a>
   */
  PAYMENT_REQUIRED(402, "Payment Required"),
  /**
   * {@code 403 Forbidden}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.4">HTTP/1.1</a>
   */
  FORBIDDEN(403, "Forbidden"),
  /**
   * {@code 404 Not Found}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.5">HTTP/1.1</a>
   */
  NOT_FOUND(404, "Not Found"),
  /**
   * {@code 405 Method Not Allowed}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.6">HTTP/1.1</a>
   */
  METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
  /**
   * {@code 406 Not Acceptable}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.7">HTTP/1.1</a>
   */
  NOT_ACCEPTABLE(406, "Not Acceptable"),
  /**
   * {@code 407 Proxy Authentication Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.8">HTTP/1.1</a>
   */
  PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
  /**
   * {@code 408 Request Timeout}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.9">HTTP/1.1</a>
   */
  REQUEST_TIMEOUT(408, "Request Timeout"),
  /**
   * {@code 409 Conflict}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.10">HTTP/1.1</a>
   */
  CONFLICT(409, "Conflict"),
  /**
   * {@code 410 Gone}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.11">HTTP/1.1</a>
   */
  GONE(410, "Gone"),
  /**
   * {@code 411 Length Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.12">HTTP/1.1</a>
   */
  LENGTH_REQUIRED(411, "Length Required"),
  /**
   * {@code 412 Precondition failed}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.13">HTTP/1.1</a>
   */
  PRECONDITION_FAILED(412, "Precondition Failed"),
  /**
   * {@code 413 Request Entity Too Large}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.14">HTTP/1.1</a>
   */
  REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
  /**
   * {@code 414 Request-URI Too Long}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.15">HTTP/1.1</a>
   */
  REQUEST_URI_TOO_LONG(414, "Request-URI Too Long"),
  /**
   * {@code 415 Unsupported Media Type}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.16">HTTP/1.1</a>
   */
  UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
  /**
   * {@code 416 Requested Range Not Satisfiable}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.17">HTTP/1.1</a>
   */
  REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested range not satisfiable"),
  /**
   * {@code 417 Expectation Failed}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.18">HTTP/1.1</a>
   */
  EXPECTATION_FAILED(417, "Expectation Failed"),
  /**
   * {@code 418 I'm a teapot}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2324#section-2.3.2">HTCPCP/1.0</a>
   */
  I_AM_A_TEAPOT(418, "I'm a teapot"),
  /**
   * {@code 422 Unprocessable Entity}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.2">WebDAV</a>
   */
  UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
  /**
   * {@code 423 Locked}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.3">WebDAV</a>
   */
  LOCKED(423, "Locked"),
  /**
   * {@code 424 Failed Dependency}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.4">WebDAV</a>
   */
  FAILED_DEPENDENCY(424, "Failed Dependency"),
  /**
   * {@code 426 Upgrade Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2817#section-6">Upgrading to TLS Within
   *      HTTP/1.1</a>
   */
  UPGRADE_REQUIRED(426, "Upgrade Required"),
  /**
   * {@code 428 Precondition Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc6585#section-3">Additional HTTP Status Codes</a>
   */
  PRECONDITION_REQUIRED(428, "Precondition Required"),
  /**
   * {@code 429 Too Many Requests}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc6585#section-4">Additional HTTP Status Codes</a>
   */
  TOO_MANY_REQUESTS(429, "Too Many Requests"),
  /**
   * {@code 431 Request Header Fields Too Large}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc6585#section-5">Additional HTTP Status Codes</a>
   */
  REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),

  // --- 5xx Server Error ---

  /**
   * {@code 500 Server Error}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.1">HTTP/1.1</a>
   */
  SERVER_ERROR(500, "Server Error"),
  /**
   * {@code 501 Not Implemented}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.2">HTTP/1.1</a>
   */
  NOT_IMPLEMENTED(501, "Not Implemented"),
  /**
   * {@code 502 Bad Gateway}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.3">HTTP/1.1</a>
   */
  BAD_GATEWAY(502, "Bad Gateway"),
  /**
   * {@code 503 Service Unavailable}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.4">HTTP/1.1</a>
   */
  SERVICE_UNAVAILABLE(503, "Service Unavailable"),
  /**
   * {@code 504 Gateway Timeout}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.5">HTTP/1.1</a>
   */
  GATEWAY_TIMEOUT(504, "Gateway Timeout"),
  /**
   * {@code 505 HTTP Version Not Supported}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.6">HTTP/1.1</a>
   */
  HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version not supported"),
  /**
   * {@code 506 Variant Also Negotiates}
   *
   * @see <a href="http://tools.ietf.org/html/rfc2295#section-8.1">Transparent Content
   *      Negotiation</a>
   */
  VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),
  /**
   * {@code 507 Insufficient Storage}
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.5">WebDAV</a>
   */
  INSUFFICIENT_STORAGE(507, "Insufficient Storage"),
  /**
   * {@code 508 Loop Detected}
   *
   * @see <a href="http://tools.ietf.org/html/rfc5842#section-7.2">WebDAV Binding Extensions</a>
   */
  LOOP_DETECTED(508, "Loop Detected"),
  /**
   * {@code 509 Bandwidth Limit Exceeded}
   */
  BANDWIDTH_LIMIT_EXCEEDED(509, "Bandwidth Limit Exceeded"),
  /**
   * {@code 510 Not Extended}
   *
   * @see <a href="http://tools.ietf.org/html/rfc2774#section-7">HTTP Extension Framework</a>
   */
  NOT_EXTENDED(510, "Not Extended"),
  /**
   * {@code 511 Network Authentication Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc6585#section-6">Additional HTTP Status Codes</a>
   */
  NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required");

  private final int value;

  private final String reason;

  private Status(final int value, final String reason) {
    this.value = value;
    this.reason = reason;
  }

  /**
   * @return Return the integer value of this status code.
   */
  public int value() {
    return this.value;
  }

  /**
   * @return the reason phrase of this status code.
   */
  public String reason() {
    return reason;
  }

  /**
   * Return a string representation of this status code.
   */
  @Override
  public String toString() {
    return reason() + " (" + value + ")";
  }

  /**
   * Return the enum constant of this type with the specified numeric value.
   *
   * @param statusCode the numeric value of the enum to be returned
   * @return the enum constant with the specified numeric value
   * @throws IllegalArgumentException if this enum has no constant for the specified numeric value
   */
  public static Status valueOf(final int statusCode) {
    for (Status status : values()) {
      if (status.value == statusCode) {
        return status;
      }
    }
    throw new IllegalArgumentException("No matching constant for [" + statusCode + "]");
  }

}
