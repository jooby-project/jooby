package org.jooby;

/**
 * A route handler/callback.
 *
 * <pre>
 * public class MyApp extends Jooby {
 *   {
 *      get("/", (req, res) -> res.send("Hello"));
 *   }
 * }
 * </pre>
 *
 * Please note that a handler is allowed to throw errors. If a service throws an exception you
 * should NOT catch it, unless of course you want to apply logic or do something special.
 * In particular you should AVOID wrapping exception:
 * <pre>
 *   {
 *      get("/", (req, res) -> {
 *        Service service = req.getInstance(Service.class);
 *        try {
 *          service.doSomething();
 *        } catch (Exception ex) {
 *         throw new RuntimeException(ex);
 *        }
 *      });
 *   }
 * </pre>
 * Previous is bad example of exception handling and should avoid wrapping exception. If you do
 * that, exception become hard to ready and the stack trace get too damn long.
 * So, if you wont do anything with the exception: DONT' catch it. Jooby will catch, logged and send
 * an appropriated status code and response.
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Router {

  /**
   * Callback method for a HTTP request.
   *
   * @param request A HTTP request.
   * @param response A HTTP response.
   * @throws Exception If something goes wrong. The exception will be catched and hanlded by Jooby.
   */
  void handle(Request request, Response response) throws Exception;

}
