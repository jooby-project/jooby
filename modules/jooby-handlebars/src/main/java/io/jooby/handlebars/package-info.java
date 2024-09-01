/**
 * Handlebars templates for jooby:
 *
 * <pre>
 * import io.jooby.handlebars.HandlebarsModule;
 *
 * {
 *   install(new HandlebarsModule());
 *
 *   get("/", ctx -> {
 *     return new MapModelAndView("index.hbs")
 *         .put("name", "Jooby");
 *   });
 * }
 * </pre>
 *
 * <p>views/index.hbs
 *
 * <pre>
 *   Hello {{name}}!
 * </pre>
 *
 * @author edgar
 */
package io.jooby.handlebars;
