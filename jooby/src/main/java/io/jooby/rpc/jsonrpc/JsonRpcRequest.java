/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rpc.jsonrpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Represents a JSON-RPC 2.0 Request object, and simultaneously acts as an iterable container for
 * batch requests.
 *
 * <p><b>Single vs. Batch Processing:</b> <br>
 * To seamlessly support JSON-RPC batching without complicating the generated routing layer, this
 * class implements {@code Iterable<JsonRpcRequest>}.
 *
 * <ul>
 *   <li>If the payload is a <b>single request</b>, iterating over this object yields exactly one
 *       element (itself).
 *   <li>If the payload is a <b>batch request</b>, the underlying JSON library's custom deserializer
 *       will flag this instance as a batch and populate it with the parsed requests. Iterating over
 *       it will yield each underlying request.
 * </ul>
 *
 * <p>This class is intentionally independent of any specific JSON library. The underlying JSON
 * provider (like Jackson or Avaje) is responsible for deserializing the {@code params} field into a
 * generic structure (e.g., a List or a Map) and populating the batch state.
 */
public class JsonRpcRequest implements Iterable<JsonRpcRequest> {

  /** A String specifying the version of the JSON-RPC protocol. MUST be exactly "2.0". */
  private String jsonrpc = "2.0";

  /** A String containing the name of the method to be invoked. */
  private String method;

  /**
   * A Structured value that holds the parameter values to be used during the invocation of the
   * method. Can be omitted, an Array (positional), or an Object (named).
   */
  private Object params;

  /**
   * An identifier established by the Client that MUST contain a String, Number, or NULL value if
   * included. If it is not included it is assumed to be a notification.
   */
  private Object id;

  // --- Batch State ---
  private boolean batch;
  private List<JsonRpcRequest> requests;

  public JsonRpcRequest() {}

  public String getJsonrpc() {
    return jsonrpc;
  }

  public void setJsonrpc(String jsonrpc) {
    this.jsonrpc = jsonrpc;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public Object getParams() {
    return params;
  }

  public void setParams(Object params) {
    this.params = params;
  }

  public Object getId() {
    return id;
  }

  public void setId(Object id) {
    this.id = id;
  }

  /**
   * Identifies if this request object is acting as a container for a JSON-RPC batch array.
   *
   * @return {@code true} if this represents a batch, {@code false} if it is a single request.
   */
  public boolean isBatch() {
    return batch;
  }

  public void setBatch(boolean batch) {
    this.batch = batch;
  }

  /**
   * Returns the underlying requests if this instance represents a batch.
   *
   * @return A list of requests, or {@code null} if this is a single request.
   */
  public List<JsonRpcRequest> getRequests() {
    return requests == null ? List.of() : requests;
  }

  /**
   * Populates this request as a batch container.
   *
   * @param requests The list of requests parsed from a JSON array.
   */
  public void setRequests(List<JsonRpcRequest> requests) {
    this.requests = requests;
    this.batch = true;
  }

  /**
   * Adds a request to this batch container. If this is the first request added, it automatically
   * converts this instance into a batch.
   *
   * @param request The JSON-RPC request to add.
   * @return This instance for method chaining.
   */
  public JsonRpcRequest add(JsonRpcRequest request) {
    if (this.requests == null) {
      this.requests = new ArrayList<>();
      this.batch = true;
    }
    this.requests.add(request);
    return this;
  }

  @Override
  public @NonNull Iterator<JsonRpcRequest> iterator() {
    if (batch) {
      return getRequests().iterator();
    }
    // If it's not a batch, it iterates over itself exactly once.
    return Collections.singletonList(this).iterator();
  }
}
