/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import com.fizzed.rocker.RockerOutput;
import io.jooby.output.Output;

/**
 * Rocker output that uses a byte array to render the output.
 *
 * @author edgar
 */
public interface BufferedRockerOutput extends RockerOutput<BufferedRockerOutput> {
  /**
   * Rocker output as jooby output.
   *
   * @return Rocker output as jooby output.
   */
  Output toOutput();
}
