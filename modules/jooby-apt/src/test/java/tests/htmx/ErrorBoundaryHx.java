/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.htmx;

import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import io.jooby.annotation.PathParam;
import io.jooby.annotation.htmx.HxError;
import io.jooby.annotation.htmx.HxView;
import jakarta.validation.Valid;

@Path("/users")
@HxError(value = "users/risk_form_top", target = "#risk-form-top-container")
public class ErrorBoundaryHx {

  /**
   * TEST: The Error Boundary Verifies: The APT generates a `try/catch` block. If `saveRiskProfile`
   * throws an exception, it catches it, sets 422 Unprocessable Entity, retargets, and re-renders
   * the input form.
   */
  @POST("/{id}/risk")
  @HxView(value = "users/risk_badge.hbs")
  @HxError(value = "users/risk_form", target = "#risk-form-container")
  public String saveRiskProfile(@PathParam String id, RiskDto3936 dto) {
    if (dto.score() < 0 || dto.score() > 100) {
      throw new IllegalArgumentException("Risk score must be between 0 and 100");
    }
    return "High";
  }

  /**
   * TEST: The Error Boundary Verifies: The APT generates a `try/catch` block. If `saveRiskProfile`
   * throws an exception, it catches it, sets 422 Unprocessable Entity, retargets, and re-renders
   * the input form.
   */
  @POST("/{id}/risk")
  @HxView(value = "users/risk_badge.hbs")
  public String saveRiskProfileBeanValidation(@PathParam String id, @Valid RiskDto3936 dto) {
    if (dto.score() < 0 || dto.score() > 100) {
      throw new IllegalArgumentException("Risk score must be between 0 and 100");
    }
    return "High";
  }
}
