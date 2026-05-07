/**
 * Provides declarative HTMX support for Jooby MVC routes.
 *
 * <p>This package contains annotations processed at compile-time by the Jooby HTMX APT generator.
 * It allows developers to define partial HTML responses, out-of-band swaps, and dynamic client-side
 * behaviors directly on their route methods without polluting business logic with header
 * management.
 *
 * <h2>Core Concepts</h2>
 *
 * <ul>
 *   <li><b>Fragments:</b> Use {@link io.jooby.annotation.htmx.HxView} to define the HTML fragment
 *       to render.
 *   <li><b>Content Negotiation:</b> Define the {@code layout} attribute in {@code @HxView} to
 *       automatically handle direct browser navigation versus HTMX AJAX requests.
 *   <li><b>Behaviors:</b> Use annotations like {@link io.jooby.annotation.htmx.HxTrigger} or {@link
 *       io.jooby.annotation.htmx.HxTarget} to append {@code HX-} headers to the response.
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * @Path("/users")
 * public class UserController {
 *
 *     @POST
 *     @HxView(
 *         value = "users/row.hbs",
 *         layout = "layouts/main.hbs",
 *         errorView = "users/form.hbs",
 *         errorTarget = "#user-form"
 *     )
 *     @HxTrigger(value = "userListUpdated", phase = Phase.AFTER_SETTLE)
 *     @HxOob("widgets/total-count")
 *     public User saveUser(UserDto dto) {
 *         // Business logic here. The APT generator handles view resolution,
 *         // validation errors, and HTMX headers.
 *         return repository.save(dto);
 *     }
 * }
 * }</pre>
 *
 * @since 4.5.0
 * @author edgar
 */
@org.jspecify.annotations.NullMarked
package io.jooby.htmx;
