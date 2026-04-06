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
 * install(new JsonRpcDispatcher());
 * services().put(JsonRpcService.class, new MyServiceRpc(new MyService()));
 * }</pre>
 *
 * @author Edgar Espina
 * @since 4.0.17
 */
@edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault
package io.jooby.jsonrpc;
