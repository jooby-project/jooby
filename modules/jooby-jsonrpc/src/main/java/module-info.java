/**
 * Global Tier 1 Dispatcher for JSON-RPC 2.0 requests.
 *
 * <p>This dispatcher acts as the central entry point for all JSON-RPC traffic. It manages the
 * lifecycle of a request by:
 *
 * <ul>
 *   <li>Parsing the incoming body into a {@link io.jooby.jsonrpc.JsonRpcRequest} (supporting both
 *       single and batch shapes).
 *   <li>Iterating through registered {@link io.jooby.jsonrpc.JsonRpcService} instances to find a
 *       matching namespace.
 *   <li>Handling <strong>Notifications</strong> (requests without an {@code id}) by suppressing
 *       responses.
 *   <li>Unifying batch results into a single JSON array or a single object response as per the
 *       spec.
 * </ul>
 *
 * <p>*
 *
 * <p>Usage:
 *
 * <pre>{@code
 * install(new Jackson3Module());
 *
 * install(new JsonRpcJackson3Module());
 *
 * install(new JsonRpcModule(new MyServiceRpc_()));
 * }</pre>
 *
 * @author Edgar Espina
 * @since 4.0.17
 */
module io.jooby.jsonrpc {
  exports io.jooby.jsonrpc;
  exports io.jooby.annotation.jsonrpc;

  requires io.jooby;
  requires static org.jspecify;
  requires typesafe.config;
  requires org.slf4j;
  requires static io.opentelemetry.api;
  requires static io.opentelemetry.context;
}
