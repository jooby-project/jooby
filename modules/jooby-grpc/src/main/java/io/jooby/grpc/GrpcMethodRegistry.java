/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import java.util.HashMap;
import java.util.Map;

import io.grpc.BindableService;
import io.grpc.MethodDescriptor;

public class GrpcMethodRegistry {
  private final Map<String, MethodDescriptor<?, ?>> registry = new HashMap<>();

  public void registerService(BindableService service) {
    var serviceDef = service.bindService();
    for (var methodDef : serviceDef.getMethods()) {
      MethodDescriptor<?, ?> descriptor = methodDef.getMethodDescriptor();
      registry.put(descriptor.getFullMethodName(), descriptor);
    }
  }

  public MethodDescriptor<?, ?> get(String fullMethodName) {
    return registry.get(fullMethodName);
  }
}
