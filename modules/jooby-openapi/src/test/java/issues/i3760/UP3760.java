/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3760;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.*;

public class UP3760 {
  // --- Nullity Checks ---
  @NotNull(message = "ID cannot be null") public Long id;

  @Null(message = "Reset token must be null initially") public String resetToken;

  // --- String Content & Formatting ---
  @NotBlank(message = "Username cannot be empty or just whitespace") public String username;

  @NotEmpty(message = "Bio cannot be null or empty") public String bio;

  @Email(message = "Must be a valid email address") public String contactEmail;

  @Pattern(regexp = "^[A-Z0-9]+$", message = "Serial must be alphanumeric") public String serialNumber;

  @Size(min = 10, max = 200, message = "Comment must be between 10 and 200 characters") public String profileComment;

  // --- Numeric Range & Value ---
  @Min(value = 18, message = "Age must be at least 18") @Max(value = 120, message = "Age cannot exceed 120") public int age;

  @DecimalMin(value = "0.01", inclusive = true, message = "Price must be at least 0.01") @DecimalMax(value = "999.99", message = "Price cannot exceed 999.99") public BigDecimal price;

  @Digits(integer = 5, fraction = 2, message = "Numeric format: up to 5 digits and 2 decimals") public BigDecimal taxRate;

  @Positive(message = "Points must be strictly greater than 0") public int loyaltyPoints;

  @NegativeOrZero(message = "Adjustment must be zero or negative") public int balanceAdjustment;

  // --- Date and Time ---
  @Past(message = "Birth date must be in the past") public LocalDate dateOfBirth;

  @PastOrPresent(message = "Registration cannot be in the future") public LocalDateTime registeredAt;

  @Future(message = "Expiry date must be in the future") public LocalDate subscriptionExpiry;

  @FutureOrPresent(message = "Next billing must be today or later") public LocalDateTime nextBillingDate;

  // --- Boolean Logic ---
  @AssertTrue(message = "You must accept the terms and conditions") public boolean termsAccepted;

  @AssertFalse(message = "Internal flag must be disabled") public boolean isInternalAccount;

  // --- Collections ---
  @NotEmpty(message = "At least one role must be assigned") @Size(min = 1, max = 5, message = "A user can have between 1 and 5 roles") public List<String> roles;
}
