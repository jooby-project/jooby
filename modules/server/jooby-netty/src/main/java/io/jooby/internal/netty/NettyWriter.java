/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;

public class NettyWriter extends Writer {

  private Charset charset;

  private OutputStream out;

  public NettyWriter(OutputStream out, Charset charset) {
    this.out = out;
    this.charset = charset;
  }

  @Override public void write(char[] cbuf, int off, int len) throws IOException {
    byte[] bytes = new String(cbuf, off, len).getBytes(charset);
    out.write(bytes, 0, bytes.length);
  }

  @Override public void write(String str) throws IOException {
    byte[] bytes = str.getBytes(charset);
    out.write(bytes, 0, bytes.length);
  }

  @Override public void write(String str, int off, int len) throws IOException {
    write(str.substring(off, len));
  }

  @Override public void write(int c) throws IOException {
    out.write((char) c);
  }

  @Override public void write(char[] cbuf) throws IOException {
    write(cbuf, 0, cbuf.length);
  }

  @Override public Writer append(char c) throws IOException {
    out.write(c);
    return this;
  }

  @Override public Writer append(CharSequence csq) throws IOException {
    if (csq == null) {
      throw new NullPointerException("CharSequence");
    }
    write(csq.toString());
    return this;
  }

  @Override public Writer append(CharSequence csq, int start, int end) throws IOException {
    if (csq == null) {
      throw new NullPointerException("CharSequence");
    }
    append(csq.subSequence(start, end));
    return this;
  }

  @Override public void flush() throws IOException {
    out.flush();
  }

  @Override public void close() throws IOException {
    out.close();
  }
}
