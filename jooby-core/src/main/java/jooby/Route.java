package jooby;

/**
 * React to HTTP Request and execute code. Routes are the heart of Jooby.
 *
 * <h1>Registering routes</h1>
 * <p>
 * It's pretty straight forward:
 * </p>
 *
 * <pre>
 * public class MyApp extends Jooby {
 *   {
 *      get("/", (req, resp) -> resp.send("Hello"));

 *      post("/", (req, resp) -> resp.send("Hello"));
 *
 *      put("/", (req, resp) -> resp.send("Hello"));
 *
 *      delete("/", (req, resp) -> resp.send("Hello"));
 *   }
 * }
 * </pre>
 *
 * <h1>Exception handling</h1>
 * <p>
 * In general you shouldn't catch exceptions (or at least runtime* exception) unless you really want
 * to do something with it.
 * </p>
 * <p>
 * Exception are hanlded by Jooby and you shouldn't cache them unless it is strictly necessary.
 * </p>
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Route {

  /**
   * Callback method for a HTTP request.
   *
   * @param request A HTTP request.
   * @param response A HTTP response.
   * @throws Exception If something goes wrong. The exception will be catched and hanlded by Jooby.
   */
  void handle(Request request, Response response) throws Exception;

}
