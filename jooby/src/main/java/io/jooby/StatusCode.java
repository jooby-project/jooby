/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP status codes.
 *
 * <p>
 * This code has been kindly borrowed from <a href="http://spring.io/">Spring</a>.
 * </p>
 *
 * @author Arjen Poutsma
 * @see <a href="http://www.iana.org/assignments/http-status-codes">HTTP StatusCode Code Registry</a>
 * @see <a href="http://en.wikipedia.org/wiki/List_of_HTTP_status_codes">List of HTTP status codes -
 *      Wikipedia</a>
 */
public class StatusCode {

  private static final Map<Integer, StatusCode> statusMap = new HashMap<>();

  // 1xx Informational

  /**
   * {@code 100 Continue}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.1.1">HTTP/1.1</a>
   */
  public static final StatusCode CONTINUE = new StatusCode(100, "Continue");
  /**
   * {@code 101 Switching Protocols}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.1.2">HTTP/1.1</a>
   */
  public static final StatusCode SWITCHING_PROTOCOLS = new StatusCode(101, "Switching Protocols");
  /**
   * {@code 102 Processing}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2518#section-10.1">WebDAV</a>
   */
  public static final StatusCode PROCESSING = new StatusCode(102, "Processing");
  /**
   * {@code 103 Checkpoint}.
   *
   * @see <a href="http://code.google.com/p/gears/wiki/ResumableHttpRequestsProposal">A proposal for
   *      supporting resumable POST/PUT HTTP requests in HTTP/1.0</a>
   */
  public static final StatusCode CHECKPOINT = new StatusCode(103, "Checkpoint");

  // 2xx Success

  /**
   * {@code 200 OK}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.1">HTTP/1.1</a>
   */
  public static final StatusCode OK = new StatusCode(200, "Success");

  /**
   * {@code 201 Created}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.2">HTTP/1.1</a>
   */
  public static final StatusCode CREATED = new StatusCode(201, "Created");
  /**
   * {@code 202 Accepted}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.3">HTTP/1.1</a>
   */
  public static final StatusCode ACCEPTED = new StatusCode(202, "Accepted");
  /**
   * {@code 203 Non-Authoritative Information}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.4">HTTP/1.1</a>
   */
  public static final StatusCode NON_AUTHORITATIVE_INFORMATION = new StatusCode(203,
      "Non-Authoritative Information");
  /**
   * {@code 204 No Content}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.5">HTTP/1.1</a>
   */
  public static final StatusCode NO_CONTENT = new StatusCode(204, "No Content");
  /**
   * {@code 205 Reset Content}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.6">HTTP/1.1</a>
   */
  public static final StatusCode RESET_CONTENT = new StatusCode(205, "Reset Content");
  /**
   * {@code 206 Partial Content}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.7">HTTP/1.1</a>
   */
  public static final StatusCode PARTIAL_CONTENT = new StatusCode(206, "Partial Content");
  /**
   * {@code 207 Multi-StatusCode}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-13">WebDAV</a>
   */
  public static final StatusCode MULTI_STATUS = new StatusCode(207, "Multi-StatusCode");
  /**
   * {@code 208 Already Reported}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc5842#section-7.1">WebDAV Binding Extensions</a>
   */
  public static final StatusCode ALREADY_REPORTED = new StatusCode(208, "Already Reported");
  /**
   * {@code 226 IM Used}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc3229#section-10.4.1">Delta encoding in HTTP</a>
   */
  public static final StatusCode IM_USED = new StatusCode(226, "IM Used");

  // 3xx Redirection

  /**
   * {@code 300 Multiple Choices}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.1">HTTP/1.1</a>
   */
  public static final StatusCode MULTIPLE_CHOICES = new StatusCode(300, "Multiple Choices");
  /**
   * {@code 301 Moved Permanently}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.2">HTTP/1.1</a>
   */
  public static final StatusCode MOVED_PERMANENTLY = new StatusCode(301, "Moved Permanently");
  /**
   * {@code 302 Found}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.3">HTTP/1.1</a>
   */
  public static final StatusCode FOUND = new StatusCode(302, "Found");
  /**
   * {@code 303 See Other}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.4">HTTP/1.1</a>
   */
  public static final StatusCode SEE_OTHER = new StatusCode(303, "See Other");
  /**
   * {@code 304 Not Modified}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.5">HTTP/1.1</a>
   */
  public static final StatusCode NOT_MODIFIED = new StatusCode(304, "Not Modified");
  /**
   * {@code 305 Use Proxy}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.6">HTTP/1.1</a>
   */
  public static final StatusCode USE_PROXY = new StatusCode(305, "Use Proxy");
  /**
   * {@code 307 Temporary Redirect}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.8">HTTP/1.1</a>
   */
  public static final StatusCode TEMPORARY_REDIRECT = new StatusCode(307, "Temporary Redirect");
  /**
   * {@code 308 Resume Incomplete}.
   *
   * @see <a href="http://code.google.com/p/gears/wiki/ResumableHttpRequestsProposal">A proposal for
   *      supporting resumable POST/PUT HTTP requests in HTTP/1.0</a>
   */
  public static final StatusCode RESUME_INCOMPLETE = new StatusCode(308, "Resume Incomplete");

  // --- 4xx Client Error ---

  /**
   * {@code 400 Bad Request}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.1">HTTP/1.1</a>
   */
  public static final StatusCode BAD_REQUEST = new StatusCode(400, "Bad Request");

  /**
   * {@code 401 Unauthorized}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.2">HTTP/1.1</a>
   */
  public static final StatusCode UNAUTHORIZED = new StatusCode(401, "Unauthorized");
  /**
   * {@code 402 Payment Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.3">HTTP/1.1</a>
   */
  public static final StatusCode PAYMENT_REQUIRED = new StatusCode(402, "Payment Required");
  /**
   * {@code 403 Forbidden}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.4">HTTP/1.1</a>
   */
  public static final StatusCode FORBIDDEN = new StatusCode(403, "Forbidden");
  /**
   * {@code 404 Not Found}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.5">HTTP/1.1</a>
   */
  public static final StatusCode NOT_FOUND = new StatusCode(404, "Not Found");
  /**
   * {@code 405 Method Not Allowed}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.6">HTTP/1.1</a>
   */
  public static final StatusCode METHOD_NOT_ALLOWED = new StatusCode(405, "Method Not Allowed");
  /**
   * {@code 406 Not Acceptable}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.7">HTTP/1.1</a>
   */
  public static final StatusCode NOT_ACCEPTABLE = new StatusCode(406, "Not Acceptable");
  /**
   * {@code 407 Proxy Authentication Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.8">HTTP/1.1</a>
   */
  public static final StatusCode PROXY_AUTHENTICATION_REQUIRED = new StatusCode(407,
      "Proxy Authentication Required");
  /**
   * {@code 408 Request Timeout}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.9">HTTP/1.1</a>
   */
  public static final StatusCode REQUEST_TIMEOUT = new StatusCode(408, "Request Timeout");
  /**
   * {@code 409 Conflict}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.10">HTTP/1.1</a>
   */
  public static final StatusCode CONFLICT = new StatusCode(409, "Conflict");
  /**
   * {@code 410 Gone}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.11">HTTP/1.1</a>
   */
  public static final StatusCode GONE = new StatusCode(410, "Gone");
  /**
   * {@code 411 Length Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.12">HTTP/1.1</a>
   */
  public static final StatusCode LENGTH_REQUIRED = new StatusCode(411, "Length Required");
  /**
   * {@code 412 Precondition failed}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.13">HTTP/1.1</a>
   */
  public static final StatusCode PRECONDITION_FAILED = new StatusCode(412, "Precondition Failed");
  /**
   * {@code 413 Request Entity Too Large}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.14">HTTP/1.1</a>
   */
  public static final StatusCode REQUEST_ENTITY_TOO_LARGE = new StatusCode(413,
      "Request Entity Too Large");
  /**
   * {@code 414 Request-URI Too Long}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.15">HTTP/1.1</a>
   */
  public static final StatusCode REQUEST_URI_TOO_LONG = new StatusCode(414,
      "Request-URI Too Long");
  /**
   * {@code 415 Unsupported Media Type}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.16">HTTP/1.1</a>
   */
  public static final StatusCode UNSUPPORTED_MEDIA_TYPE = new StatusCode(415,
      "Unsupported Media Type");
  /**
   * {@code 416 Requested Range Not Satisfiable}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.17">HTTP/1.1</a>
   */
  public static final StatusCode REQUESTED_RANGE_NOT_SATISFIABLE = new StatusCode(416,
      "Requested range not satisfiable");
  /**
   * {@code 417 Expectation Failed}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.18">HTTP/1.1</a>
   */
  public static final StatusCode EXPECTATION_FAILED = new StatusCode(417, "Expectation Failed");
  /**
   * {@code 418 I'm a teapot}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2324#section-2.3.2">HTCPCP/1.0</a>
   */
  public static final StatusCode I_AM_A_TEAPOT = new StatusCode(418, "I'm a teapot");
  /**
   * {@code 422 Unprocessable Entity}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.2">WebDAV</a>
   */
  public static final StatusCode UNPROCESSABLE_ENTITY = new StatusCode(422,
      "Unprocessable Entity");
  /**
   * {@code 423 Locked}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.3">WebDAV</a>
   */
  public static final StatusCode LOCKED = new StatusCode(423, "Locked");
  /**
   * {@code 424 Failed Dependency}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.4">WebDAV</a>
   */
  public static final StatusCode FAILED_DEPENDENCY = new StatusCode(424, "Failed Dependency");
  /**
   * {@code 426 Upgrade Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2817#section-6">Upgrading to TLS Within
   *      HTTP/1.1</a>
   */
  public static final StatusCode UPGRADE_REQUIRED = new StatusCode(426, "Upgrade Required");
  /**
   * {@code 428 Precondition Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc6585#section-3">Additional HTTP StatusCode Codes</a>
   */
  public static final StatusCode PRECONDITION_REQUIRED = new StatusCode(428,
      "Precondition Required");
  /**
   * {@code 429 Too Many Requests}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc6585#section-4">Additional HTTP StatusCode Codes</a>
   */
  public static final StatusCode TOO_MANY_REQUESTS = new StatusCode(429, "Too Many Requests");
  /**
   * {@code 431 Request Header Fields Too Large}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc6585#section-5">Additional HTTP StatusCode Codes</a>
   */
  public static final StatusCode REQUEST_HEADER_FIELDS_TOO_LARGE = new StatusCode(431,
      "Request Header Fields Too Large");

  // --- 5xx Server Error ---

  /**
   * {@code 500 Server Error}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.1">HTTP/1.1</a>
   */
  public static final StatusCode SERVER_ERROR = new StatusCode(500, "Server Error");
  /**
   * {@code 501 Not Implemented}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.2">HTTP/1.1</a>
   */
  public static final StatusCode NOT_IMPLEMENTED = new StatusCode(501, "Not Implemented");
  /**
   * {@code 502 Bad Gateway}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.3">HTTP/1.1</a>
   */
  public static final StatusCode BAD_GATEWAY = new StatusCode(502, "Bad Gateway");
  /**
   * {@code 503 Service Unavailable}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.4">HTTP/1.1</a>
   */
  public static final StatusCode SERVICE_UNAVAILABLE = new StatusCode(503, "Service Unavailable");
  /**
   * {@code 504 Gateway Timeout}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.5">HTTP/1.1</a>
   */
  public static final StatusCode GATEWAY_TIMEOUT = new StatusCode(504, "Gateway Timeout");
  /**
   * {@code 505 HTTP Version Not Supported}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.6">HTTP/1.1</a>
   */
  public static final StatusCode HTTP_VERSION_NOT_SUPPORTED = new StatusCode(505,
      "HTTP Version not supported");
  /**
   * {@code 506 Variant Also Negotiates}
   *
   * @see <a href="http://tools.ietf.org/html/rfc2295#section-8.1">Transparent Content
   *      Negotiation</a>
   */
  public static final StatusCode VARIANT_ALSO_NEGOTIATES = new StatusCode(506,
      "Variant Also Negotiates");
  /**
   * {@code 507 Insufficient Storage}
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.5">WebDAV</a>
   */
  public static final StatusCode INSUFFICIENT_STORAGE = new StatusCode(507,
      "Insufficient Storage");
  /**
   * {@code 508 Loop Detected}
   *
   * @see <a href="http://tools.ietf.org/html/rfc5842#section-7.2">WebDAV Binding Extensions</a>
   */
  public static final StatusCode LOOP_DETECTED = new StatusCode(508, "Loop Detected");
  /**
   * {@code 509 Bandwidth Limit Exceeded}
   */
  public static final StatusCode BANDWIDTH_LIMIT_EXCEEDED = new StatusCode(509,
      "Bandwidth Limit Exceeded");
  /**
   * {@code 510 Not Extended}
   *
   * @see <a href="http://tools.ietf.org/html/rfc2774#section-7">HTTP Extension Framework</a>
   */
  public static final StatusCode NOT_EXTENDED = new StatusCode(510, "Not Extended");
  /**
   * {@code 511 Network Authentication Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc6585#section-6">Additional HTTP StatusCode Codes</a>
   */
  public static final StatusCode NETWORK_AUTHENTICATION_REQUIRED = new StatusCode(511,
      "Network Authentication Required");

  private final int value;

  private final String reason;

  private StatusCode(final int value, final String reason) {
    statusMap.put(Integer.valueOf(value), this);
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
  public static StatusCode valueOf(final int statusCode) {
    Integer key = Integer.valueOf(statusCode);
    StatusCode status = statusMap.get(key);
    return status == null ? new StatusCode(key, key.toString()) : status;
  }
}
