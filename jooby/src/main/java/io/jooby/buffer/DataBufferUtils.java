/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import java.io.OutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.Nullable;

public class DataBufferUtils {
  private static final Logger logger = LoggerFactory.getLogger(DataBufferUtils.class);
  private static final Consumer<DataBuffer> RELEASE_CONSUMER = DataBufferUtils::release;
  private static final int DEFAULT_CHUNK_SIZE = 1024;

  /**
   * Release the given data buffer. If it is a {@link PooledDataBuffer} and has been {@linkplain
   * PooledDataBuffer#isAllocated() allocated}, this method will call {@link
   * PooledDataBuffer#release()}. If it is a {@link CloseableDataBuffer}, this method will call
   * {@link CloseableDataBuffer#close()}.
   *
   * @param dataBuffer the data buffer to release
   * @return {@code true} if the buffer was released; {@code false} otherwise.
   */
  public static boolean release(@Nullable DataBuffer dataBuffer) {
    if (dataBuffer instanceof PooledDataBuffer pooledDataBuffer) {
      if (pooledDataBuffer.isAllocated()) {
        try {
          return pooledDataBuffer.release();
        } catch (IllegalStateException ex) {
          if (logger.isDebugEnabled()) {
            logger.debug("Failed to release PooledDataBuffer: " + dataBuffer, ex);
          }
          return false;
        }
      }
    } else if (dataBuffer instanceof CloseableDataBuffer closeableDataBuffer) {
      try {
        closeableDataBuffer.close();
        return true;
      } catch (IllegalStateException ex) {
        if (logger.isDebugEnabled()) {
          logger.debug("Failed to release CloseableDataBuffer " + dataBuffer, ex);
        }
        return false;
      }
    }
    return false;
  }

  /**
   * Retain the given data buffer, if it is a {@link PooledDataBuffer}.
   *
   * @param dataBuffer the data buffer to retain
   * @return the retained buffer
   */
  @SuppressWarnings("unchecked")
  public static <T extends DataBuffer> T retain(T dataBuffer) {
    if (dataBuffer instanceof PooledDataBuffer pooledDataBuffer) {
      return (T) pooledDataBuffer.retain();
    } else {
      return dataBuffer;
    }
  }

  /**
   * Associate the given hint with the data buffer if it is a pooled buffer and supports leak
   * tracking.
   *
   * @param dataBuffer the data buffer to attach the hint to
   * @param hint the hint to attach
   * @return the input buffer
   * @since 5.3.2
   */
  @SuppressWarnings("unchecked")
  public static <T extends DataBuffer> T touch(T dataBuffer, Object hint) {
    if (dataBuffer instanceof TouchableDataBuffer touchableDataBuffer) {
      return (T) touchableDataBuffer.touch(hint);
    } else {
      return dataBuffer;
    }
  }

  /**
   * Create a new {@code Publisher<DataBuffer>} based on bytes written to a {@code OutputStream}.
   *
   * <ul>
   *   <li>The parameter {@code outputStreamConsumer} is invoked once per subscription of the
   *       returned {@code Publisher}, when the first item is {@linkplain
   *       Flow.Subscription#request(long) requested}.
   *   <li>{@link OutputStream#write(byte[], int, int) OutputStream.write()} invocations made by
   *       {@code outputStreamConsumer} are buffered until they exceed the default chunk size of
   *       1024, or when the stream is {@linkplain OutputStream#flush() flushed} and then result in
   *       a {@linkplain Flow.Subscriber#onNext(Object) published} item if there is {@linkplain
   *       Flow.Subscription#request(long) demand}.
   *   <li>If there is <em>no demand</em>, {@code OutputStream.write()} will block until there is.
   *   <li>If the subscription is {@linkplain Flow.Subscription#cancel() cancelled}, {@code
   *       OutputStream.write()} will throw a {@code IOException}.
   *   <li>The subscription is {@linkplain Flow.Subscriber#onComplete() completed} when {@code
   *       outputStreamHandler} completes.
   *   <li>Any exceptions thrown from {@code outputStreamHandler} will be dispatched to the
   *       {@linkplain Flow.Subscriber#onError(Throwable) Subscriber}.
   * </ul>
   *
   * @param outputStreamConsumer invoked when the first buffer is requested
   * @param executor used to invoke the {@code outputStreamHandler}
   * @return a {@code Publisher<DataBuffer>} based on bytes written by {@code outputStreamHandler}
   * @since 6.1
   */
  public static Flow.Publisher<DataBuffer> outputStreamPublisher(
      Consumer<OutputStream> outputStreamConsumer,
      DataBufferFactory bufferFactory,
      Executor executor) {

    return outputStreamPublisher(outputStreamConsumer, bufferFactory, executor, DEFAULT_CHUNK_SIZE);
  }

  /**
   * Creates a new {@code Publisher<DataBuffer>} based on bytes written to a {@code OutputStream}.
   *
   * <ul>
   *   <li>The parameter {@code outputStreamConsumer} is invoked once per subscription of the
   *       returned {@code Publisher}, when the first item is {@linkplain
   *       Flow.Subscription#request(long) requested}.
   *   <li>{@link OutputStream#write(byte[], int, int) OutputStream.write()} invocations made by
   *       {@code outputStreamHandler} are buffered until they reach or exceed {@code chunkSize}, or
   *       when the stream is {@linkplain OutputStream#flush() flushed} and then result in a
   *       {@linkplain Flow.Subscriber#onNext(Object) published} item if there is {@linkplain
   *       Flow.Subscription#request(long) demand}.
   *   <li>If there is <em>no demand</em>, {@code OutputStream.write()} will block until there is.
   *   <li>If the subscription is {@linkplain Flow.Subscription#cancel() cancelled}, {@code
   *       OutputStream.write()} will throw a {@code IOException}.
   *   <li>The subscription is {@linkplain Flow.Subscriber#onComplete() completed} when {@code
   *       outputStreamHandler} completes.
   *   <li>Any exceptions thrown from {@code outputStreamHandler} will be dispatched to the
   *       {@linkplain Flow.Subscriber#onError(Throwable) Subscriber}.
   * </ul>
   *
   * @param outputStreamConsumer invoked when the first buffer is requested
   * @param executor used to invoke the {@code outputStreamHandler}
   * @param chunkSize minimum size of the buffer produced by the publisher
   * @return a {@code Publisher<DataBuffer>} based on bytes written by {@code outputStreamHandler}
   * @since 6.1
   */
  public static Flow.Publisher<DataBuffer> outputStreamPublisher(
      Consumer<OutputStream> outputStreamConsumer,
      DataBufferFactory bufferFactory,
      Executor executor,
      int chunkSize) {

    Assert.notNull(outputStreamConsumer, "OutputStreamConsumer must not be null");
    Assert.notNull(bufferFactory, "BufferFactory must not be null");
    Assert.notNull(executor, "Executor must not be null");
    Assert.isTrue(chunkSize > 0, "Chunk size must be > 0");

    return new OutputStreamPublisher(outputStreamConsumer, bufferFactory, executor, chunkSize);
  }

  /** Return a consumer that calls {@link #release(DataBuffer)} on all passed data buffers. */
  public static Consumer<DataBuffer> releaseConsumer() {
    return RELEASE_CONSUMER;
  }
}
