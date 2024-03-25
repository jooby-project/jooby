/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.Nullable;

public class DataBufferUtils {
  private static final Logger logger = LoggerFactory.getLogger(DataBufferUtils.class);
  private static final Consumer<DataBuffer> RELEASE_CONSUMER = DataBufferUtils::release;

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

  /** Return a consumer that calls {@link #release(DataBuffer)} on all passed data buffers. */
  public static Consumer<DataBuffer> releaseConsumer() {
    return RELEASE_CONSUMER;
  }
}
