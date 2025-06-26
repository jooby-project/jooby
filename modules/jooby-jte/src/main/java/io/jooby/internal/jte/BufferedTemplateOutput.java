/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jte;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

import gg.jte.TemplateOutput;
import io.jooby.output.Output;

public class BufferedTemplateOutput implements TemplateOutput {
  private final Output buffer;
  private final Charset charset;

  public BufferedTemplateOutput(Output buffer, Charset charset) {
    this.buffer = buffer;
    this.charset = charset;
  }

  @Override
  public void writeBinaryContent(byte[] value) {
    buffer.write(value);
  }

  @Override
  public void writeContent(String s) {
    buffer.write(s, charset);
  }

  @Override
  public void writeContent(String s, int beginIndex, int endIndex) {
    buffer.write(CharBuffer.wrap(s, beginIndex, endIndex), charset);
  }
}
