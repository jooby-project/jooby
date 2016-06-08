package org.jooby;

import java.util.Map;
import java.util.function.Function;

import org.jooby.internal.handlers.FlashScopeHandler;
import org.jooby.mvc.Flash;

import com.google.inject.Binder;
import com.typesafe.config.Config;

/**
 * <h1>flash scope</h1>
 * <p>
 * The flash scope is designed to transport success and error messages, between requests. The flash
 * scope is similar to {@link Session} but lifecycle is shorter: data are kept for only one request.
 * </p>
 * <p>
 * The flash scope is implemented as client side cookie, so it helps to keep application stateless.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   use(new FlashScope());
 *
 *   get("/", req -> {
 *     return req.ifFlash("success").orElse("Welcome!");
 *   });
 *
 *   post("/", req -> {
 *     req.flash("success", "The item has been created");
 *     return Results.redirect("/");
 *   });
 * }
 * }</pre>
 *
 * {@link FlashScope} is also available on mvc routes via {@link Flash} annotation:
 *
 * <pre>{@code
 * &#64;Path("/")
 * public class Controller {
 *
 *   &#64;GET
 *   public Object flashScope(@Flash Map<String, String> flash) {
 *     ...
 *   }
 *
 *   &#64;GET
 *   public Object flashAttr(@Flash String foo) {
 *     ...
 *   }
 *
 *   &#64;GET
 *   public Object optionlFlashAttr(@Flash Optional<String> foo) {
 *     ...
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Worth to mention that flash attributes are accessible from template engine by prefixing
 * attributes with <code>flash.</code>. Here is a <code>handlebars.java</code> example:
 * </p>
 *
 * <pre>{@code
 * {{#if flash.success}}
 *   {{flash.success}}
 * {{else}}
 *   Welcome!
 * {{/if}}
 * }</pre>
 *
 * @author edgar
 * @since 1.0.0.CR4
 */
public class FlashScope implements Jooby.Module {

  public static final String NAME = "flash";

  private Function<String, Map<String, String>> decoder = Cookie.URL_DECODER;

  private Function<Map<String, String>, String> encoder = Cookie.URL_ENCODER;

  private String cookie = "flash";

  private String method = "*";

  private String path = "*";

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    env.routes().use(method, path, new FlashScopeHandler(cookie, decoder, encoder))
        .name("flash-scope");
  }

}
