package org.jooby;

import com.google.common.annotations.Beta;

/**
 * Filter a server request in order to perform common tasks. Example of filters are:
 *
 * <h3>Auth filter example</h3>
 *
 * <pre>
 *   String token = req.header("token").stringValue();
 *   if (token != null) {
 *     // validate token...
 *     if (valid(token)) {
 *       chain.next(req, res);
 *     }
 *   } else {
 *     res.status(403);
 *   }
 * </pre>
 *
 * <h3>Logging filter example</h3>
 *
 * <pre>
 *   long start = System.currentTimeMillis();
 *   chain.next(req, res);
 *   long end = System.currentTimeMillis();
 *   log.info("Request: {} took {}ms", req.path(), end - start);
 * </pre>
 *
 * NOTE: Don't forget to call {@link Route.Chain#next(Request, Response)} if next router/filter
 * need to be executed.
 *
 * @author edgar
 * @since 0.1.0
 */
@Beta
public interface Filter {

  /**
   * The <code>handle</code> method of the Filter is called by the server each time a
   * request/response pair is passed through the chain due to a client request for a resource at
   * the end of the chain.
   * The {@link Route.Chain} passed in to this method allows the Filter to pass on the request and
   * response to the next entity in the chain.
   *
   * <p>
   * A typical implementation of this method would follow the following pattern:
   * </p>
   * <ul>
   * <li>Examine the request</li>
   * <li>Optionally wrap the request object with a custom implementation to filter content or
   * headers for input filtering</li>
   * <li>Optionally wrap the response object with a custom implementation to filter content or
   * headers for output filtering</li>
   * <li>
   * <ul>
   * <li><strong>Either</strong> invoke the next entity in the chain using the {@link Route.Chain}
   * object (<code>chain.next(req, res)</code>),</li>
   * <li><strong>or</strong> not pass on the request/response pair to the next entity in the filter
   * chain to block the request processing</li>
   * </ul>
   * <li>Directly set headers on the response after invocation of the next entity in the filter
   * chain.</li>
   * </ul>
   *
   * @param req A HTTP request.
   * @param res A HTTP response.
   * @param chain A route chain.
   * @throws Exception If something goes wrong.
   */
  void handle(Request req, Response res, Route.Chain chain) throws Exception;

}
