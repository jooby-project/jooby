/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class StatusCodeTest {

  @ParameterizedTest(name = "Code {0} should return StatusCode {1}")
  @MethodSource("statusCodes")
  public void valueOf(int code, StatusCode expected) {
    StatusCode actual = StatusCode.valueOf(code);
    assertEquals(expected.value(), actual.value());

    // For known constants, verify it returns the exact same object reference
    // For the default case (999), it will be a new instance, so we only check the value
    if (code != 999) {
      assertEquals(expected, actual);
    }
  }

  private static Stream<Arguments> statusCodes() {
    return Stream.of(
        Arguments.of(StatusCode.CONTINUE_CODE, StatusCode.CONTINUE),
        Arguments.of(StatusCode.SWITCHING_PROTOCOLS_CODE, StatusCode.SWITCHING_PROTOCOLS),
        Arguments.of(StatusCode.PROCESSING_CODE, StatusCode.PROCESSING),
        Arguments.of(StatusCode.CHECKPOINT_CODE, StatusCode.CHECKPOINT),
        Arguments.of(StatusCode.OK_CODE, StatusCode.OK),
        Arguments.of(StatusCode.CREATED_CODE, StatusCode.CREATED),
        Arguments.of(StatusCode.ACCEPTED_CODE, StatusCode.ACCEPTED),
        Arguments.of(
            StatusCode.NON_AUTHORITATIVE_INFORMATION_CODE,
            StatusCode.NON_AUTHORITATIVE_INFORMATION),
        Arguments.of(StatusCode.NO_CONTENT_CODE, StatusCode.NO_CONTENT),
        Arguments.of(StatusCode.RESET_CONTENT_CODE, StatusCode.RESET_CONTENT),
        Arguments.of(StatusCode.PARTIAL_CONTENT_CODE, StatusCode.PARTIAL_CONTENT),
        Arguments.of(StatusCode.MULTI_STATUS_CODE, StatusCode.MULTI_STATUS),
        Arguments.of(StatusCode.ALREADY_REPORTED_CODE, StatusCode.ALREADY_REPORTED),
        Arguments.of(StatusCode.IM_USED_CODE, StatusCode.IM_USED),
        Arguments.of(StatusCode.MULTIPLE_CHOICES_CODE, StatusCode.MULTIPLE_CHOICES),
        Arguments.of(StatusCode.MOVED_PERMANENTLY_CODE, StatusCode.MOVED_PERMANENTLY),
        Arguments.of(StatusCode.FOUND_CODE, StatusCode.FOUND),
        Arguments.of(StatusCode.SEE_OTHER_CODE, StatusCode.SEE_OTHER),
        Arguments.of(StatusCode.NOT_MODIFIED_CODE, StatusCode.NOT_MODIFIED),
        Arguments.of(StatusCode.USE_PROXY_CODE, StatusCode.USE_PROXY),
        Arguments.of(StatusCode.TEMPORARY_REDIRECT_CODE, StatusCode.TEMPORARY_REDIRECT),
        Arguments.of(StatusCode.RESUME_INCOMPLETE_CODE, StatusCode.RESUME_INCOMPLETE),
        Arguments.of(StatusCode.BAD_REQUEST_CODE, StatusCode.BAD_REQUEST),
        Arguments.of(StatusCode.UNAUTHORIZED_CODE, StatusCode.UNAUTHORIZED),
        Arguments.of(StatusCode.PAYMENT_REQUIRED_CODE, StatusCode.PAYMENT_REQUIRED),
        Arguments.of(StatusCode.FORBIDDEN_CODE, StatusCode.FORBIDDEN),
        Arguments.of(StatusCode.NOT_FOUND_CODE, StatusCode.NOT_FOUND),
        Arguments.of(StatusCode.METHOD_NOT_ALLOWED_CODE, StatusCode.METHOD_NOT_ALLOWED),
        Arguments.of(StatusCode.NOT_ACCEPTABLE_CODE, StatusCode.NOT_ACCEPTABLE),
        Arguments.of(
            StatusCode.PROXY_AUTHENTICATION_REQUIRED_CODE,
            StatusCode.PROXY_AUTHENTICATION_REQUIRED),
        Arguments.of(StatusCode.REQUEST_TIMEOUT_CODE, StatusCode.REQUEST_TIMEOUT),
        Arguments.of(StatusCode.CONFLICT_CODE, StatusCode.CONFLICT),
        Arguments.of(StatusCode.GONE_CODE, StatusCode.GONE),
        Arguments.of(StatusCode.LENGTH_REQUIRED_CODE, StatusCode.LENGTH_REQUIRED),
        Arguments.of(StatusCode.PRECONDITION_FAILED_CODE, StatusCode.PRECONDITION_FAILED),
        Arguments.of(StatusCode.REQUEST_ENTITY_TOO_LARGE_CODE, StatusCode.REQUEST_ENTITY_TOO_LARGE),
        Arguments.of(StatusCode.REQUEST_URI_TOO_LONG_CODE, StatusCode.REQUEST_URI_TOO_LONG),
        Arguments.of(StatusCode.UNSUPPORTED_MEDIA_TYPE_CODE, StatusCode.UNSUPPORTED_MEDIA_TYPE),
        Arguments.of(
            StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE_CODE,
            StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE),
        Arguments.of(StatusCode.EXPECTATION_FAILED_CODE, StatusCode.EXPECTATION_FAILED),
        Arguments.of(StatusCode.I_AM_A_TEAPOT_CODE, StatusCode.I_AM_A_TEAPOT),
        Arguments.of(StatusCode.UNPROCESSABLE_ENTITY_CODE, StatusCode.UNPROCESSABLE_ENTITY),
        Arguments.of(StatusCode.LOCKED_CODE, StatusCode.LOCKED),
        Arguments.of(StatusCode.FAILED_DEPENDENCY_CODE, StatusCode.FAILED_DEPENDENCY),
        Arguments.of(StatusCode.UPGRADE_REQUIRED_CODE, StatusCode.UPGRADE_REQUIRED),
        Arguments.of(StatusCode.PRECONDITION_REQUIRED_CODE, StatusCode.PRECONDITION_REQUIRED),
        Arguments.of(StatusCode.TOO_MANY_REQUESTS_CODE, StatusCode.TOO_MANY_REQUESTS),
        Arguments.of(
            StatusCode.REQUEST_HEADER_FIELDS_TOO_LARGE_CODE,
            StatusCode.REQUEST_HEADER_FIELDS_TOO_LARGE),
        Arguments.of(StatusCode.CLIENT_CLOSED_REQUEST_CODE, StatusCode.CLIENT_CLOSED_REQUEST),
        Arguments.of(StatusCode.SERVER_ERROR_CODE, StatusCode.SERVER_ERROR),
        Arguments.of(StatusCode.NOT_IMPLEMENTED_CODE, StatusCode.NOT_IMPLEMENTED),
        Arguments.of(StatusCode.BAD_GATEWAY_CODE, StatusCode.BAD_GATEWAY),
        Arguments.of(StatusCode.SERVICE_UNAVAILABLE_CODE, StatusCode.SERVICE_UNAVAILABLE),
        Arguments.of(StatusCode.GATEWAY_TIMEOUT_CODE, StatusCode.GATEWAY_TIMEOUT),
        Arguments.of(
            StatusCode.HTTP_VERSION_NOT_SUPPORTED_CODE, StatusCode.HTTP_VERSION_NOT_SUPPORTED),
        Arguments.of(StatusCode.VARIANT_ALSO_NEGOTIATES_CODE, StatusCode.VARIANT_ALSO_NEGOTIATES),
        Arguments.of(StatusCode.INSUFFICIENT_STORAGE_CODE, StatusCode.INSUFFICIENT_STORAGE),
        Arguments.of(StatusCode.LOOP_DETECTED_CODE, StatusCode.LOOP_DETECTED),
        Arguments.of(StatusCode.BANDWIDTH_LIMIT_EXCEEDED_CODE, StatusCode.BANDWIDTH_LIMIT_EXCEEDED),
        Arguments.of(StatusCode.NOT_EXTENDED_CODE, StatusCode.NOT_EXTENDED),
        Arguments.of(
            StatusCode.NETWORK_AUTHENTICATION_REQUIRED_CODE,
            StatusCode.NETWORK_AUTHENTICATION_REQUIRED),

        // Default / Custom code branch coverage
        Arguments.of(999, StatusCode.valueOf(999)));
  }
}
