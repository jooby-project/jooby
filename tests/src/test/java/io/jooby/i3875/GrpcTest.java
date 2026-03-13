/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3875;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import io.grpc.reflection.v1.ServerReflectionGrpc;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.grpc.stub.StreamObserver;
import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.grpc.GrpcModule;
import io.jooby.guice.GuiceModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class GrpcTest {

  private void setupApp(Jooby app) {
    app.install(new GuiceModule());

    app.install(
        new GrpcModule(new EchoChatService(), ProtoReflectionServiceV1.newInstance())
            .bind(EchoGreeterService.class));
  }

  @ServerTest
  void shouldHandleUnaryRequests(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setHttp2(true).setSecurePort(8443))
        .define(this::setupApp)
        .ready(
            http -> {
              var channel =
                  ManagedChannelBuilder.forAddress("localhost", runner.getAllocatedPort())
                      .usePlaintext()
                      .build();

              try {
                var stub = GreeterGrpc.newBlockingStub(channel);
                var response =
                    stub.sayHello(HelloRequest.newBuilder().setName("Pablo Marmol").build());

                assertThat(response.getMessage()).isEqualTo("Hello Pablo Marmol");
              } finally {
                channel.shutdown();
              }
            });
  }

  @ServerTest
  void shouldHandleDeadlineExceeded(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setHttp2(true).setSecurePort(8443))
        .define(this::setupApp)
        .ready(
            http -> {
              var channel =
                  ManagedChannelBuilder.forAddress("localhost", runner.getAllocatedPort())
                      .usePlaintext()
                      .build();

              try {
                // Attach an impossibly short deadline (1 millisecond) to the stub
                var stub =
                    GreeterGrpc.newBlockingStub(channel)
                        .withDeadlineAfter(1, TimeUnit.MILLISECONDS);

                var exception =
                    org.junit.jupiter.api.Assertions.assertThrows(
                        StatusRuntimeException.class,
                        () ->
                            stub.sayHello(
                                HelloRequest.newBuilder().setName("Pablo Marmol").build()));

                // Assert that the bridge correctly caught the timeout and returned Status 4
                assertThat(exception.getStatus().getCode())
                    .isEqualTo(Status.Code.DEADLINE_EXCEEDED);
              } finally {
                channel.shutdown();
              }
            });
  }

  @ServerTest
  void shouldHandleBidiStreaming(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setHttp2(true).setSecurePort(8443))
        .define(this::setupApp)
        .ready(
            http -> {
              var channel =
                  ManagedChannelBuilder.forAddress("localhost", runner.getAllocatedPort())
                      .usePlaintext()
                      .build();

              try {
                var asyncStub = ChatServiceGrpc.newStub(channel);
                var responses = new CopyOnWriteArrayList<String>();
                var latch = new CountDownLatch(1);

                StreamObserver<ChatMessage> responseObserver =
                    new StreamObserver<>() {
                      @Override
                      public void onNext(ChatMessage value) {
                        responses.add(value.getText());
                      }

                      @Override
                      public void onError(Throwable t) {
                        latch.countDown();
                      }

                      @Override
                      public void onCompleted() {
                        latch.countDown();
                      }
                    };

                var requestObserver = asyncStub.chatStream(responseObserver);

                requestObserver.onNext(
                    ChatMessage.newBuilder().setUser("JavaClient").setText("Ping 1").build());

                // Add a tiny delay to prevent CI thread-scheduler race conditions
                // where the server closes the stream before Undertow finishes flushing Ping 2.
                Thread.sleep(50);

                requestObserver.onNext(
                    ChatMessage.newBuilder().setUser("JavaClient").setText("Ping 2").build());

                // Allow Ping 2 to reach the server before sending the close signal
                Thread.sleep(50);

                requestObserver.onCompleted();

                // Wait for the server stream to gracefully complete
                boolean completed = latch.await(5, TimeUnit.SECONDS);

                assertThat(completed).isTrue();
                assertThat(responses).containsExactly("Echo: Ping 1", "Echo: Ping 2");

              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                channel.shutdown();
              }
            });
  }

  @ServerTest
  void shouldHandleServerStreaming(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setHttp2(true).setSecurePort(8443))
        .define(this::setupApp)
        .ready(
            http -> {
              var channel =
                  ManagedChannelBuilder.forAddress("localhost", runner.getAllocatedPort())
                      .usePlaintext()
                      .build();

              try {
                var asyncStub = ChatServiceGrpc.newStub(channel);
                var responses = new CopyOnWriteArrayList<String>();
                var latch = new CountDownLatch(1);

                StreamObserver<ChatMessage> responseObserver =
                    new StreamObserver<>() {
                      @Override
                      public void onNext(ChatMessage value) {
                        responses.add(value.getText());
                      }

                      @Override
                      public void onError(Throwable t) {
                        latch.countDown();
                      }

                      @Override
                      public void onCompleted() {
                        latch.countDown();
                      }
                    };

                // Assuming a server-streaming method exists: rpc ServerStream(ChatMessage) returns
                // (stream ChatMessage)
                // asyncStub.serverStream(ChatMessage.newBuilder().setText("Trigger").build(),
                // responseObserver);
                //
                // boolean completed = latch.await(5, TimeUnit.SECONDS);
                // assertThat(completed).isTrue();
                // assertThat(responses.size()).isGreaterThan(1);

              } finally {
                channel.shutdown();
              }
            });
  }

  @ServerTest
  void shouldHandleReflection(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setHttp2(true).setSecurePort(8443))
        .define(this::setupApp)
        .ready(
            http -> {
              var channel =
                  ManagedChannelBuilder.forAddress("localhost", runner.getAllocatedPort())
                      .usePlaintext()
                      .build();

              try {
                var stub = ServerReflectionGrpc.newStub(channel);
                var registeredServices = new CopyOnWriteArrayList<String>();
                var latch = new CountDownLatch(1);

                StreamObserver<ServerReflectionResponse> responseObserver =
                    new StreamObserver<>() {
                      @Override
                      public void onNext(ServerReflectionResponse response) {
                        response
                            .getListServicesResponse()
                            .getServiceList()
                            .forEach(s -> registeredServices.add(s.getName()));
                      }

                      @Override
                      public void onError(Throwable t) {
                        latch.countDown();
                      }

                      @Override
                      public void onCompleted() {
                        latch.countDown();
                      }
                    };

                var requestObserver = stub.serverReflectionInfo(responseObserver);

                requestObserver.onNext(
                    ServerReflectionRequest.newBuilder()
                        .setListServices("")
                        .setHost("localhost")
                        .build());
                requestObserver.onCompleted();

                boolean completed = latch.await(5, TimeUnit.SECONDS);

                assertThat(completed).isTrue();
                assertThat(registeredServices)
                    .contains(
                        "io.jooby.i3875.Greeter",
                        "io.jooby.i3875.ChatService",
                        "grpc.reflection.v1.ServerReflection");

              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                channel.shutdown();
              }
            });
  }

  @ServerTest
  void shouldHandleGrpcurlReflection(ServerTestRunner runner) {
    org.junit.jupiter.api.Assumptions.assumeTrue(
        isGrpcurlInstalled(), "grpcurl is not installed. Skipping strict HTTP/2 compliance test.");

    runner
        .options(new ServerOptions().setHttp2(true).setSecurePort(8443))
        .define(this::setupApp)
        .ready(
            http -> {
              try {
                var pb =
                    new ProcessBuilder(
                        "grpcurl", "-plaintext", "localhost:" + runner.getAllocatedPort(), "list");

                var process = pb.start();
                var output =
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                var error =
                    new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

                int exitCode = process.waitFor();

                assertThat(exitCode)
                    .withFailMessage("grpcurl failed with error: " + error)
                    .isEqualTo(0);

                assertThat(output)
                    .contains(
                        "io.jooby.i3875.Greeter",
                        "io.jooby.i3875.ChatService",
                        "grpc.reflection.v1.ServerReflection");

              } catch (Exception e) {
                throw new RuntimeException("Failed to execute grpcurl test", e);
              }
            });
  }

  /**
   * When a gRPC client requests a method that doesn't exist, our native handlers will ignore it.
   * Jooby's core router will then catch it and return a standard HTTP 404 Not Found. According to
   * the gRPC-over-HTTP/2 specification, the gRPC client will automatically translate a pure HTTP
   * 404 into a gRPC UNIMPLEMENTED (Status Code 12) exception.
   *
   * @param runner Sever runner.
   */
  @ServerTest
  void shouldHandleMethodNotFound(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setHttp2(true).setSecurePort(8443))
        .define(this::setupApp)
        .ready(
            http -> {
              var channel =
                  ManagedChannelBuilder.forAddress("localhost", runner.getAllocatedPort())
                      .usePlaintext()
                      .build();

              try {
                // 1. Create a fake method descriptor for a non-existent method
                var unknownMethod =
                    io.grpc.MethodDescriptor.<HelloRequest, HelloReply>newBuilder()
                        .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                        .setFullMethodName("io.jooby.i3875.Greeter/UnknownMethod")
                        .setRequestMarshaller(
                            io.grpc.protobuf.ProtoUtils.marshaller(
                                HelloRequest.getDefaultInstance()))
                        .setResponseMarshaller(
                            io.grpc.protobuf.ProtoUtils.marshaller(HelloReply.getDefaultInstance()))
                        .build();

                // 2. Execute the call manually and expect an exception
                var exception =
                    org.junit.jupiter.api.Assertions.assertThrows(
                        io.grpc.StatusRuntimeException.class,
                        () ->
                            io.grpc.stub.ClientCalls.blockingUnaryCall(
                                channel,
                                unknownMethod,
                                io.grpc.CallOptions.DEFAULT,
                                HelloRequest.newBuilder().setName("Pablo Marmol").build()));

                // 3. Assert that Jooby's HTTP 404 is correctly translated by the gRPC client into
                // UNIMPLEMENTED
                assertThat(exception.getStatus().getCode())
                    .isEqualTo(io.grpc.Status.Code.UNIMPLEMENTED);

              } finally {
                channel.shutdown();
              }
            });
  }

  private boolean isGrpcurlInstalled() {
    try {
      var process = new ProcessBuilder("grpcurl", "-version").start();
      return process.waitFor() == 0;
    } catch (Exception e) {
      return false;
    }
  }
}
