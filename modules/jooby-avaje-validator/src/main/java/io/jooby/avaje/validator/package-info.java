/**
 * Avaje Validator Module: https://jooby.io/modules/avaje-validator.
 *
 * <pre>{@code
 * {
 *   install(new AvajeValidatorModule());
 *
 * }
 *
 * public class Controller {
 *
 *   @POST("/create")
 *   public void create(@Valid Bean bean) {
 *   }
 *
 * }
 * }</pre>
 *
 * <p>Supports validation of a single bean, list, array, or map.
 *
 * <p>The module also provides a built-in error handler that catches {@link
 * io.avaje.validation.ConstraintViolationException} and transforms it into a {@link
 * io.jooby.validation.ValidationResult}
 */
package io.jooby.avaje.validator;
