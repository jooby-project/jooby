/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.grpc;

import java.util.HexFormat;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;

/**
 * Wraps a gRPC StreamObserver as an internal field and feeds it data from a standard Java
 * Flow.Publisher.
 */
public class GrpcRequestBridge implements Subscriber<byte[]> {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ClientCallStreamObserver<byte[]> internalObserver;
  private final GrpcDeframer deframer;
  private String path;
  private Subscription subscription;
  private AtomicBoolean completed = new AtomicBoolean(false);

  public GrpcRequestBridge(String path, StreamObserver<byte[]> internalObserver) {
    this.path = path;
    this.deframer = new GrpcDeframer();
    this.internalObserver = (ClientCallStreamObserver<byte[]>) internalObserver;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    this.subscription = subscription;
    // Demand the first chunk. In a pro bridge, you might demand 1
    // and only demand more when the gRPC observer is ready.
    subscription.request(1);
  }

  public void onNext(byte[] item) {
    try {

      deframer.process(
          item,
          msg -> {
            log.info("deframe {}", HexFormat.of().formatHex(msg));
            internalObserver.onNext(msg);
          });

      log.info("asking for more request(1)");
      internalObserver.request(1);
      //      subscription.request(1);
    } catch (Throwable t) {
      subscription.cancel();
      internalObserver.onError(t);
    }
  }

  private boolean isReflectionPath(String path) {
    return path.contains("ServerReflectionInfo");
  }

  @Override
  public void onError(Throwable throwable) {
    internalObserver.onError(throwable);
  }

  @Override
  public void onComplete() {
    if (completed.compareAndSet(false, true)) {
      internalObserver.onCompleted();
    }
  }
}
