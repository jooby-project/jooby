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

/**
 * HTTP status codes.
 *
 * <p>
 * This code has been borrowed from <a href="http://spring.io/">Spring</a>.
 * </p>
 *
 * @author Arjen Poutsma
 * @see <a href="http://www.iana.org/assignments/http-status-codes">HTTP StatusCode Code Registry</a>
 * @see <a href="http://en.wikipedia.org/wiki/List_of_HTTP_status_codes">List of HTTP status codes -
 *      Wikipedia</a>
 */
public class StatusCode {

  // 1xx Informational

  /**
   * {@code 100 Continue}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.1.1">HTTP/1.1</a>
   */
  public static final int CONTINUE_CODE = 100;

  public static final StatusCode CONTINUE = new StatusCode(CONTINUE_CODE, "Continue");
  /**
   * {@code 101 Switching Protocols}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.1.2">HTTP/1.1</a>
   */
  public static final int SWITCHING_PROTOCOLS_CODE = 101;

  public static final StatusCode SWITCHING_PROTOCOLS = new StatusCode(SWITCHING_PROTOCOLS_CODE,
      "Switching Protocols");
  /**
   * {@code 102 Processing}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2518#section-10.1">WebDAV</a>
   */
  public static final int PROCESSING_CODE = 102;

  public static final StatusCode PROCESSING = new StatusCode(PROCESSING_CODE, "Processing");
  /**
   * {@code 103 Checkpoint}.
   *
   * @see <a href="http://code.google.com/p/gears/wiki/ResumableHttpRequestsProposal">A proposal for
   *      supporting resumable POST/PUT HTTP requests in HTTP/1.0</a>
   */
  public static final int CHECKPOINT_CODE = 103;

  public static final StatusCode CHECKPOINT = new StatusCode(CHECKPOINT_CODE, "Checkpoint");

  // 2xx Success

  /**
   * {@code 200 OK}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.1">HTTP/1.1</a>
   */
  public static final int OK_CODE = 200;

  public static final StatusCode OK = new StatusCode(OK_CODE, "Success");

  /**
   * {@code 201 Created}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.2">HTTP/1.1</a>
   */
  public static final int CREATED_CODE = 201;

  public static final StatusCode CREATED = new StatusCode(CREATED_CODE, "Created");
  /**
   * {@code 202 Accepted}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.3">HTTP/1.1</a>
   */
  public static final int ACCEPTED_CODE = 202;

  public static final StatusCode ACCEPTED = new StatusCode(ACCEPTED_CODE, "Accepted");
  /**
   * {@code 203 Non-Authoritative Information}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.4">HTTP/1.1</a>
   */
  public static final int NON_AUTHORITATIVE_INFORMATION_CODE = 203;

  public static final StatusCode NON_AUTHORITATIVE_INFORMATION = new StatusCode(
      NON_AUTHORITATIVE_INFORMATION_CODE,
      "Non-Authoritative Information");
  /**
   * {@code 204 No Content}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.5">HTTP/1.1</a>
   */
  public static final int NO_CONTENT_CODE = 204;

  public static final StatusCode NO_CONTENT = new StatusCode(NO_CONTENT_CODE, "No Content");
  /**
   * {@code 205 Reset Content}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.6">HTTP/1.1</a>
   */
  public static final int RESET_CONTENT_CODE = 205;

  public static final StatusCode RESET_CONTENT = new StatusCode(RESET_CONTENT_CODE,
      "Reset Content");
  /**
   * {@code 206 Partial Content}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.7">HTTP/1.1</a>
   */
  public static final int PARTIAL_CONTENT_CODE = 206;

  public static final StatusCode PARTIAL_CONTENT = new StatusCode(PARTIAL_CONTENT_CODE,
      "Partial Content");
  /**
   * {@code 207 Multi-StatusCode}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-13">WebDAV</a>
   */
  public static final int MULTI_STATUS_CODE = 207;

  public static final StatusCode MULTI_STATUS = new StatusCode(MULTI_STATUS_CODE,
      "Multi-StatusCode");
  /**
   * {@code 208 Already Reported}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc5842#section-7.1">WebDAV Binding Extensions</a>
   */
  public static final int ALREADY_REPORTED_CODE = 208;

  public static final StatusCode ALREADY_REPORTED = new StatusCode(ALREADY_REPORTED_CODE,
      "Already Reported");
  /**
   * {@code 226 IM Used}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc3229#section-10.4.1">Delta encoding in HTTP</a>
   */
  public static final int IM_USED_CODE = 226;

  public static final StatusCode IM_USED = new StatusCode(IM_USED_CODE, "IM Used");

  // 3xx Redirection

  /**
   * {@code 300 Multiple Choices}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.1">HTTP/1.1</a>
   */
  public static final int MULTIPLE_CHOICES_CODE = 300;

  public static final StatusCode MULTIPLE_CHOICES = new StatusCode(MULTIPLE_CHOICES_CODE,
      "Multiple Choices");
  /**
   * {@code 301 Moved Permanently}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.2">HTTP/1.1</a>
   */
  public static final int MOVED_PERMANENTLY_CODE = 301;

  public static final StatusCode MOVED_PERMANENTLY = new StatusCode(MOVED_PERMANENTLY_CODE,
      "Moved Permanently");
  /**
   * {@code 302 Found}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.3">HTTP/1.1</a>
   */
  public static final int FOUND_CODE = 302;

  public static final StatusCode FOUND = new StatusCode(FOUND_CODE, "Found");
  /**
   * {@code 303 See Other}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.4">HTTP/1.1</a>
   */
  public static final int SEE_OTHER_CODE = 303;

  public static final StatusCode SEE_OTHER = new StatusCode(SEE_OTHER_CODE, "See Other");
  /**
   * {@code 304 Not Modified}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.5">HTTP/1.1</a>
   */
  public static final int NOT_MODIFIED_CODE = 304;

  public static final StatusCode NOT_MODIFIED = new StatusCode(NOT_MODIFIED_CODE, "Not Modified");
  /**
   * {@code 305 Use Proxy}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.6">HTTP/1.1</a>
   */
  public static final int USE_PROXY_CODE = 305;

  public static final StatusCode USE_PROXY = new StatusCode(USE_PROXY_CODE, "Use Proxy");
  /**
   * {@code 307 Temporary Redirect}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.8">HTTP/1.1</a>
   */
  public static final int TEMPORARY_REDIRECT_CODE = 307;

  public static final StatusCode TEMPORARY_REDIRECT = new StatusCode(TEMPORARY_REDIRECT_CODE,
      "Temporary Redirect");
  /**
   * {@code 308 Resume Incomplete}.
   *
   * @see <a href="http://code.google.com/p/gears/wiki/ResumableHttpRequestsProposal">A proposal for
   *      supporting resumable POST/PUT HTTP requests in HTTP/1.0</a>
   */
  public static final int RESUME_INCOMPLETE_CODE = 308;

  public static final StatusCode RESUME_INCOMPLETE = new StatusCode(RESUME_INCOMPLETE_CODE,
      "Resume Incomplete");

  // --- 4xx Client Error ---

  /**
   * {@code 400 Bad Request}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.1">HTTP/1.1</a>
   */
  public static final int BAD_REQUEST_CODE = 400;

  public static final StatusCode BAD_REQUEST = new StatusCode(BAD_REQUEST_CODE, "Bad Request");

  /**
   * {@code 401 Unauthorized}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.2">HTTP/1.1</a>
   */
  public static final int UNAUTHORIZED_CODE = 401;

  public static final StatusCode UNAUTHORIZED = new StatusCode(UNAUTHORIZED_CODE, "Unauthorized");
  /**
   * {@code 402 Payment Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.3">HTTP/1.1</a>
   */
  public static final int PAYMENT_REQUIRED_CODE = 402;

  public static final StatusCode PAYMENT_REQUIRED = new StatusCode(PAYMENT_REQUIRED_CODE,
      "Payment Required");
  /**
   * {@code 403 Forbidden}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.4">HTTP/1.1</a>
   */
  public static final int FORBIDDEN_CODE = 403;

  public static final StatusCode FORBIDDEN = new StatusCode(FORBIDDEN_CODE, "Forbidden");
  /**
   * {@code 404 Not Found}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.5">HTTP/1.1</a>
   */
  public static final int NOT_FOUND_CODE = 404;

  public static final StatusCode NOT_FOUND = new StatusCode(NOT_FOUND_CODE, "Not Found");
  /**
   * {@code 405 Method Not Allowed}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.6">HTTP/1.1</a>
   */
  public static final int METHOD_NOT_ALLOWED_CODE = 405;

  public static final StatusCode METHOD_NOT_ALLOWED = new StatusCode(METHOD_NOT_ALLOWED_CODE,
      "Method Not Allowed");
  /**
   * {@code 406 Not Acceptable}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.7">HTTP/1.1</a>
   */
  public static final int NOT_ACCEPTABLE_CODE = 406;

  public static final StatusCode NOT_ACCEPTABLE = new StatusCode(NOT_ACCEPTABLE_CODE,
      "Not Acceptable");
  /**
   * {@code 407 Proxy Authentication Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.8">HTTP/1.1</a>
   */
  public static final int PROXY_AUTHENTICATION_REQUIRED_CODE = 407;

  public static final StatusCode PROXY_AUTHENTICATION_REQUIRED = new StatusCode(
      PROXY_AUTHENTICATION_REQUIRED_CODE,
      "Proxy Authentication Required");
  /**
   * {@code 408 Request Timeout}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.9">HTTP/1.1</a>
   */
  public static final int REQUEST_TIMEOUT_CODE = 408;

  public static final StatusCode REQUEST_TIMEOUT = new StatusCode(REQUEST_TIMEOUT_CODE,
      "Request Timeout");
  /**
   * {@code 409 Conflict}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.10">HTTP/1.1</a>
   */
  public static final int CONFLICT_CODE = 409;

  public static final StatusCode CONFLICT = new StatusCode(CONFLICT_CODE, "Conflict");
  /**
   * {@code 410 Gone}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.11">HTTP/1.1</a>
   */
  public static final int GONE_CODE = 410;

  public static final StatusCode GONE = new StatusCode(GONE_CODE, "Gone");
  /**
   * {@code 411 Length Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.12">HTTP/1.1</a>
   */
  public static final int LENGTH_REQUIRED_CODE = 411;

  public static final StatusCode LENGTH_REQUIRED = new StatusCode(LENGTH_REQUIRED_CODE,
      "Length Required");
  /**
   * {@code 412 Precondition failed}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.13">HTTP/1.1</a>
   */
  public static final int PRECONDITION_FAILED_CODE = 412;

  public static final StatusCode PRECONDITION_FAILED = new StatusCode(PRECONDITION_FAILED_CODE,
      "Precondition Failed");
  /**
   * {@code 413 Request Entity Too Large}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.14">HTTP/1.1</a>
   */
  public static final int REQUEST_ENTITY_TOO_LARGE_CODE = 413;

  public static final StatusCode REQUEST_ENTITY_TOO_LARGE = new StatusCode(
      REQUEST_ENTITY_TOO_LARGE_CODE,
      "Request Entity Too Large");
  /**
   * {@code 414 Request-URI Too Long}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.15">HTTP/1.1</a>
   */
  public static final int REQUEST_URI_TOO_LONG_CODE = 414;

  public static final StatusCode REQUEST_URI_TOO_LONG = new StatusCode(REQUEST_URI_TOO_LONG_CODE,
      "Request-URI Too Long");
  /**
   * {@code 415 Unsupported Media Type}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.16">HTTP/1.1</a>
   */
  public static final int UNSUPPORTED_MEDIA_TYPE_CODE = 415;

  public static final StatusCode UNSUPPORTED_MEDIA_TYPE = new StatusCode(
      UNSUPPORTED_MEDIA_TYPE_CODE,
      "Unsupported Media Type");
  /**
   * {@code 416 Requested Range Not Satisfiable}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.17">HTTP/1.1</a>
   */
  public static final int REQUESTED_RANGE_NOT_SATISFIABLE_CODE = 416;

  public static final StatusCode REQUESTED_RANGE_NOT_SATISFIABLE = new StatusCode(
      REQUESTED_RANGE_NOT_SATISFIABLE_CODE,
      "Requested range not satisfiable");
  /**
   * {@code 417 Expectation Failed}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.18">HTTP/1.1</a>
   */
  public static final int EXPECTATION_FAILED_CODE = 417;

  public static final StatusCode EXPECTATION_FAILED = new StatusCode(EXPECTATION_FAILED_CODE,
      "Expectation Failed");
  /**
   * {@code 418 I'm a teapot}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2324#section-2.3.2">HTCPCP/1.0</a>
   */
  public static final int I_AM_A_TEAPOT_CODE = 418;

  public static final StatusCode I_AM_A_TEAPOT = new StatusCode(I_AM_A_TEAPOT_CODE, "I'm a teapot");
  /**
   * {@code 422 Unprocessable Entity}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.2">WebDAV</a>
   */
  public static final int UNPROCESSABLE_ENTITY_CODE = 422;

  public static final StatusCode UNPROCESSABLE_ENTITY = new StatusCode(UNPROCESSABLE_ENTITY_CODE,
      "Unprocessable Entity");
  /**
   * {@code 423 Locked}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.3">WebDAV</a>
   */
  public static final int LOCKED_CODE = 423;

  public static final StatusCode LOCKED = new StatusCode(LOCKED_CODE, "Locked");
  /**
   * {@code 424 Failed Dependency}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.4">WebDAV</a>
   */
  public static final int FAILED_DEPENDENCY_CODE = 424;

  public static final StatusCode FAILED_DEPENDENCY = new StatusCode(FAILED_DEPENDENCY_CODE,
      "Failed Dependency");
  /**
   * {@code 426 Upgrade Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2817#section-6">Upgrading to TLS Within
   *      HTTP/1.1</a>
   */
  public static final int UPGRADE_REQUIRED_CODE = 426;

  public static final StatusCode UPGRADE_REQUIRED = new StatusCode(UPGRADE_REQUIRED_CODE,
      "Upgrade Required");
  /**
   * {@code 428 Precondition Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc6585#section-3">Additional HTTP StatusCode Codes</a>
   */
  public static final int PRECONDITION_REQUIRED_CODE = 428;

  public static final StatusCode PRECONDITION_REQUIRED = new StatusCode(PRECONDITION_REQUIRED_CODE,
      "Precondition Required");
  /**
   * {@code 429 Too Many Requests}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc6585#section-4">Additional HTTP StatusCode Codes</a>
   */
  public static final int TOO_MANY_REQUESTS_CODE = 429;

  public static final StatusCode TOO_MANY_REQUESTS = new StatusCode(TOO_MANY_REQUESTS_CODE,
      "Too Many Requests");
  /**
   * {@code 431 Request Header Fields Too Large}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc6585#section-5">Additional HTTP StatusCode Codes</a>
   */
  public static final int REQUEST_HEADER_FIELDS_TOO_LARGE_CODE = 431;

  public static final StatusCode REQUEST_HEADER_FIELDS_TOO_LARGE = new StatusCode(
      REQUEST_HEADER_FIELDS_TOO_LARGE_CODE,
      "Request Header Fields Too Large");

  // --- 5xx Server Error ---

  /**
   * {@code 500 Server Error}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.1">HTTP/1.1</a>
   */
  public static final int SERVER_ERROR_CODE = 500;

  public static final StatusCode SERVER_ERROR = new StatusCode(SERVER_ERROR_CODE, "Server Error");
  /**
   * {@code 501 Not Implemented}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.2">HTTP/1.1</a>
   */
  public static final int NOT_IMPLEMENTED_CODE = 501;

  public static final StatusCode NOT_IMPLEMENTED = new StatusCode(NOT_IMPLEMENTED_CODE,
      "Not Implemented");
  /**
   * {@code 502 Bad Gateway}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.3">HTTP/1.1</a>
   */
  public static final int BAD_GATEWAY_CODE = 502;

  public static final StatusCode BAD_GATEWAY = new StatusCode(BAD_GATEWAY_CODE, "Bad Gateway");
  /**
   * {@code 503 Service Unavailable}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.4">HTTP/1.1</a>
   */
  public static final int SERVICE_UNAVAILABLE_CODE = 503;

  public static final StatusCode SERVICE_UNAVAILABLE = new StatusCode(SERVICE_UNAVAILABLE_CODE,
      "Service Unavailable");
  /**
   * {@code 504 Gateway Timeout}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.5">HTTP/1.1</a>
   */
  public static final int GATEWAY_TIMEOUT_CODE = 504;

  public static final StatusCode GATEWAY_TIMEOUT = new StatusCode(GATEWAY_TIMEOUT_CODE,
      "Gateway Timeout");
  /**
   * {@code 505 HTTP Version Not Supported}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.6">HTTP/1.1</a>
   */
  public static final int HTTP_VERSION_NOT_SUPPORTED_CODE = 505;

  public static final StatusCode HTTP_VERSION_NOT_SUPPORTED = new StatusCode(
      HTTP_VERSION_NOT_SUPPORTED_CODE,
      "HTTP Version not supported");
  /**
   * {@code 506 Variant Also Negotiates}
   *
   * @see <a href="http://tools.ietf.org/html/rfc2295#section-8.1">Transparent Content
   *      Negotiation</a>
   */
  public static final int VARIANT_ALSO_NEGOTIATES_CODE = 506;

  public static final StatusCode VARIANT_ALSO_NEGOTIATES = new StatusCode(
      VARIANT_ALSO_NEGOTIATES_CODE,
      "Variant Also Negotiates");
  /**
   * {@code 507 Insufficient Storage}
   *
   * @see <a href="http://tools.ietf.org/html/rfc4918#section-11.5">WebDAV</a>
   */
  public static final int INSUFFICIENT_STORAGE_CODE = 507;

  public static final StatusCode INSUFFICIENT_STORAGE = new StatusCode(INSUFFICIENT_STORAGE_CODE,
      "Insufficient Storage");
  /**
   * {@code 508 Loop Detected}
   *
   * @see <a href="http://tools.ietf.org/html/rfc5842#section-7.2">WebDAV Binding Extensions</a>
   */
  public static final int LOOP_DETECTED_CODE = 508;

  public static final StatusCode LOOP_DETECTED = new StatusCode(LOOP_DETECTED_CODE,
      "Loop Detected");
  /**
   * {@code 509 Bandwidth Limit Exceeded}
   */
  public static final int BANDWIDTH_LIMIT_EXCEEDED_CODE = 509;

  public static final StatusCode BANDWIDTH_LIMIT_EXCEEDED = new StatusCode(
      BANDWIDTH_LIMIT_EXCEEDED_CODE,
      "Bandwidth Limit Exceeded");
  /**
   * {@code 510 Not Extended}
   *
   * @see <a href="http://tools.ietf.org/html/rfc2774#section-7">HTTP Extension Framework</a>
   */
  public static final int NOT_EXTENDED_CODE = 510;

  public static final StatusCode NOT_EXTENDED = new StatusCode(NOT_EXTENDED_CODE, "Not Extended");
  /**
   * {@code 511 Network Authentication Required}.
   *
   * @see <a href="http://tools.ietf.org/html/rfc6585#section-6">Additional HTTP StatusCode Codes</a>
   */
  public static final int NETWORK_AUTHENTICATION_REQUIRED_CODE = 511;

  public static final StatusCode NETWORK_AUTHENTICATION_REQUIRED = new StatusCode(
      NETWORK_AUTHENTICATION_REQUIRED_CODE,
      "Network Authentication Required");

  private final int value;

  private final String reason;

  private StatusCode(final int value, final String reason) {
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

  @Override public boolean equals(Object obj) {
    if (obj instanceof StatusCode) {
      return this.value == ((StatusCode) obj).value;
    }
    return false;
  }

  @Override public int hashCode() {
    return value;
  }

  /**
   * Return the enum constant of this type with the specified numeric value.
   *
   * @param statusCode the numeric value of the enum to be returned
   * @return the enum constant with the specified numeric value
   * @throws IllegalArgumentException if this enum has no constant for the specified numeric value
   */
  public static StatusCode valueOf(final int statusCode) {
    switch (statusCode) {
      case CONTINUE_CODE:
        return CONTINUE;
      case SWITCHING_PROTOCOLS_CODE:
        return SWITCHING_PROTOCOLS;
      case PROCESSING_CODE:
        return PROCESSING;
      case CHECKPOINT_CODE:
        return CHECKPOINT;
      case OK_CODE:
        return OK;
      case CREATED_CODE:
        return CREATED;
      case ACCEPTED_CODE:
        return ACCEPTED;
      case NON_AUTHORITATIVE_INFORMATION_CODE:
        return NON_AUTHORITATIVE_INFORMATION;
      case NO_CONTENT_CODE:
        return NO_CONTENT;
      case RESET_CONTENT_CODE:
        return RESET_CONTENT;
      case PARTIAL_CONTENT_CODE:
        return PARTIAL_CONTENT;
      case MULTI_STATUS_CODE:
        return MULTI_STATUS;
      case ALREADY_REPORTED_CODE:
        return ALREADY_REPORTED;
      case IM_USED_CODE:
        return IM_USED;
      case MULTIPLE_CHOICES_CODE:
        return MULTIPLE_CHOICES;
      case MOVED_PERMANENTLY_CODE:
        return MOVED_PERMANENTLY;
      case FOUND_CODE:
        return FOUND;
      case SEE_OTHER_CODE:
        return SEE_OTHER;
      case NOT_MODIFIED_CODE:
        return NOT_MODIFIED;
      case USE_PROXY_CODE:
        return USE_PROXY;
      case TEMPORARY_REDIRECT_CODE:
        return TEMPORARY_REDIRECT;
      case RESUME_INCOMPLETE_CODE:
        return RESUME_INCOMPLETE;
      case BAD_REQUEST_CODE:
        return BAD_REQUEST;
      case UNAUTHORIZED_CODE:
        return UNAUTHORIZED;
      case PAYMENT_REQUIRED_CODE:
        return PAYMENT_REQUIRED;
      case FORBIDDEN_CODE:
        return FORBIDDEN;
      case NOT_FOUND_CODE:
        return NOT_FOUND;
      case METHOD_NOT_ALLOWED_CODE:
        return METHOD_NOT_ALLOWED;
      case NOT_ACCEPTABLE_CODE:
        return NOT_ACCEPTABLE;
      case PROXY_AUTHENTICATION_REQUIRED_CODE:
        return PROXY_AUTHENTICATION_REQUIRED;
      case REQUEST_TIMEOUT_CODE:
        return REQUEST_TIMEOUT;
      case CONFLICT_CODE:
        return CONFLICT;
      case GONE_CODE:
        return GONE;
      case LENGTH_REQUIRED_CODE:
        return LENGTH_REQUIRED;
      case PRECONDITION_FAILED_CODE:
        return PRECONDITION_FAILED;
      case REQUEST_ENTITY_TOO_LARGE_CODE:
        return REQUEST_ENTITY_TOO_LARGE;
      case REQUEST_URI_TOO_LONG_CODE:
        return REQUEST_URI_TOO_LONG;
      case UNSUPPORTED_MEDIA_TYPE_CODE:
        return UNSUPPORTED_MEDIA_TYPE;
      case REQUESTED_RANGE_NOT_SATISFIABLE_CODE:
        return REQUESTED_RANGE_NOT_SATISFIABLE;
      case EXPECTATION_FAILED_CODE:
        return EXPECTATION_FAILED;
      case I_AM_A_TEAPOT_CODE:
        return I_AM_A_TEAPOT;
      case UNPROCESSABLE_ENTITY_CODE:
        return UNPROCESSABLE_ENTITY;
      case LOCKED_CODE:
        return LOCKED;
      case FAILED_DEPENDENCY_CODE:
        return FAILED_DEPENDENCY;
      case UPGRADE_REQUIRED_CODE:
        return UPGRADE_REQUIRED;
      case PRECONDITION_REQUIRED_CODE:
        return PRECONDITION_REQUIRED;
      case TOO_MANY_REQUESTS_CODE:
        return TOO_MANY_REQUESTS;
      case REQUEST_HEADER_FIELDS_TOO_LARGE_CODE:
        return REQUEST_HEADER_FIELDS_TOO_LARGE;
      case SERVER_ERROR_CODE:
        return SERVER_ERROR;
      case NOT_IMPLEMENTED_CODE:
        return NOT_IMPLEMENTED;
      case BAD_GATEWAY_CODE:
        return BAD_GATEWAY;
      case SERVICE_UNAVAILABLE_CODE:
        return SERVICE_UNAVAILABLE;
      case GATEWAY_TIMEOUT_CODE:
        return GATEWAY_TIMEOUT;
      case HTTP_VERSION_NOT_SUPPORTED_CODE:
        return HTTP_VERSION_NOT_SUPPORTED;
      case VARIANT_ALSO_NEGOTIATES_CODE:
        return VARIANT_ALSO_NEGOTIATES;
      case INSUFFICIENT_STORAGE_CODE:
        return INSUFFICIENT_STORAGE;
      case LOOP_DETECTED_CODE:
        return LOOP_DETECTED;
      case BANDWIDTH_LIMIT_EXCEEDED_CODE:
        return BANDWIDTH_LIMIT_EXCEEDED;
      case NOT_EXTENDED_CODE:
        return NOT_EXTENDED;
      case NETWORK_AUTHENTICATION_REQUIRED_CODE:
        return NETWORK_AUTHENTICATION_REQUIRED;
      default:
       return new StatusCode(statusCode, Integer.toString(statusCode));
    }
  }
}
