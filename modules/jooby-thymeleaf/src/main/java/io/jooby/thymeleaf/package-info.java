/**
 * Thymeleaf module: https://jooby.io/modules/thymeleaf.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new ThymeleafModule());
 *
 *   get("/", ctx -> {
 *     User user = ...;
 *     return new ModelAndView("index.html")
 *         .put("user", user);
 *   });
 * }
 * }</pre>
 *
 * The template engine looks for a file-system directory: <code>views</code> in the current user
 * directory. If the directory doesn't exist, it looks for the same directory in the project
 * classpath.
 *
 * <p>Template engine supports the following file extensions: <code>.thl</code>, <code>.thl.html
 * </code> and <code>.html</code>.
 *
 * <p>You can specify a different template location:
 *
 * <pre>{@code
 * {
 *
 *    install(new ThymeleafModule("mypath"));
 *
 * }
 * }</pre>
 *
 * The <code>mypath</code> location works in the same way: file-system or fallback to classpath.
 *
 * <p>Direct access to {@link org.thymeleaf.TemplateEngine} is available via require call:
 *
 * <pre>{@code
 * {
 *
 *   TemplateEngine engine = require(TemplateEngine.class);
 *
 * }
 * }</pre>
 *
 * Complete documentation is available at: https://jooby.io/modules/thymeleaf.
 *
 * @author edgar
 * @since 2.0.0
 */
@org.jspecify.annotations.NullMarked
package io.jooby.thymeleaf;
