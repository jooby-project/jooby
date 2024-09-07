/**
 * Hibernate Validator Module: https://jooby.io/modules/hibernate-validator.
 *
 * <pre>{@code
 * {
 *   install(new HibernateValidatorModule());
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
 * jakarta.validation.ConstraintViolationException} and transforms it into a {@link
 * io.jooby.validation.ValidationResult}
 *
 * @author kliushnichenko
 * @since 3.3.1
 */
package io.jooby.hibernate.validator;
