/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jooby.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * {@link InputStream} implementation that reads a character stream from a {@link Reader} and
 * transforms it to a byte stream using a specified charset encoding. The stream
 * is transformed using a {@link CharsetEncoder} object, guaranteeing that all charset
 * encodings supported by the JRE are handled correctly. In particular for charsets such as
 * UTF-16, the implementation ensures that one and only one byte order marker
 * is produced.
 * <p>
 * Since in general it is not possible to predict the number of characters to be read from the
 * {@link Reader} to satisfy a read request on the {@link ReaderInputStream}, all reads from the
 * {@link Reader} are buffered. There is therefore no well defined correlation between the current
 * position of the {@link Reader} and that of the {@link ReaderInputStream}. This also implies that
 * in general there is no need to wrap the underlying {@link Reader} in a
 * {@link java.io.BufferedReader}.
 * <p>
 * {@link ReaderInputStream} implements the inverse transformation of
 * {@link java.io.InputStreamReader}; in the following example, reading from <tt>in2</tt> would
 * return the same byte sequence as reading from <tt>in</tt> (provided that the initial byte
 * sequence is legal with respect to the charset encoding):
 *
 * <pre>
 * InputStream in = ...
 * Charset cs = ...
 * InputStreamReader reader = new InputStreamReader(in, cs);
 * ReaderInputStream in2 = new ReaderInputStream(reader, cs);</pre>
 * {@link ReaderInputStream} implements the same transformation as
 * {@link java.io.OutputStreamWriter}, except that the control flow is reversed: both classes
 * transform a character stream into a byte stream, but {@link java.io.OutputStreamWriter} pushes
 * data to the underlying stream, while {@link ReaderInputStream} pulls it from the underlying
 * stream.
 * <p>
 * Note that while there are use cases where there is no alternative to using this class, very often
 * the need to use this class is an indication of a flaw in the design of the code. This class is
 * typically used in situations where an existing API only accepts an {@link InputStream}, but where
 * the most natural way to produce the data is as a character stream, i.e. by providing a
 * {@link Reader} instance. An example of a situation where this problem may appear is when
 * implementing the {@link javax.activation.DataSource} interface from the Java Activation
 * Framework.
 * <p>
 * Given the fact that the {@link Reader} class doesn't provide any way to predict whether the next
 * read operation will block or not, it is not possible to provide a meaningful implementation of
 * the {@link InputStream#available()} method. A call to this method will always return 0. Also,
 * this class doesn't support {@link InputStream#mark(int)}.
 * <p>
 * Instances of {@link ReaderInputStream} are not thread safe.
 *
 * @author <a href="mailto:veithen@apache.org">Andreas Veithen</a>
 * @since Commons IO 2.0
 */
public class ReaderInputStream extends InputStream {
  private static final int DEFAULT_BUFFER_SIZE = 1024;

  private final Reader reader;
  private final CharsetEncoder encoder;

  /**
   * CharBuffer used as input for the decoder. It should be reasonably
   * large as we read data from the underlying Reader into this buffer.
   */
  private final CharBuffer encoderIn;

  /**
   * ByteBuffer used as output for the decoder. This buffer can be small
   * as it is only used to transfer data from the decoder to the
   * buffer provided by the caller.
   */
  private final ByteBuffer encoderOut = ByteBuffer.allocate(128);

  private CoderResult lastCoderResult;

  private boolean endOfInput;

  /**
   * Construct a new {@link ReaderInputStream}.
   *
   * @param reader the target {@link Reader}
   * @param charset the charset encoding
   * @param bufferSize the size of the input buffer in number of characters
   */
  private ReaderInputStream(final Reader reader, final Charset charset, final int bufferSize) {
    this.reader = reader;
    encoder = charset.newEncoder();
    encoderIn = CharBuffer.allocate(bufferSize);
    encoderIn.flip();
  }

  /**
   * Construct a new {@link ReaderInputStream} with a default input buffer size of
   * 1024 characters.
   *
   * @param reader the target {@link Reader}
   * @param charset the charset encoding
   */
  public ReaderInputStream(final Reader reader, final Charset charset) {
    this(reader, charset, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Read the specified number of bytes into an array.
   *
   * @param b the byte array to read into
   * @param off the offset to start reading bytes into
   * @param len the number of bytes to read
   * @return the number of bytes read or <code>-1</code> if the end of the stream has been reached
   * @throws IOException if an I/O error occurs
   */
  @Override
  public int read(final byte[] b, int off, int len) throws IOException {
    int read = 0;
    while (len > 0) {
      if (encoderOut.position() > 0) {
        encoderOut.flip();
        int c = Math.min(encoderOut.remaining(), len);
        encoderOut.get(b, off, c);
        off += c;
        len -= c;
        read += c;
        encoderOut.compact();
      } else {
        if (!endOfInput && (lastCoderResult == null || lastCoderResult.isUnderflow())) {
          encoderIn.compact();
          int position = encoderIn.position();
          // We don't use Reader#read(CharBuffer) here because it is more efficient
          // to write directly to the underlying char array (the default implementation
          // copies data to a temporary char array).
          int c = reader.read(encoderIn.array(), position, encoderIn.remaining());
          if (c == -1) {
            endOfInput = true;
          } else {
            encoderIn.position(position + c);
          }
          encoderIn.flip();
        }
        lastCoderResult = encoder.encode(encoderIn, encoderOut, endOfInput);
        if (endOfInput && encoderOut.position() == 0) {
          break;
        }
      }
    }
    return read == 0 && endOfInput ? -1 : read;
  }

  /**
   * Read the specified number of bytes into an array.
   *
   * @param b the byte array to read into
   * @return the number of bytes read or <code>-1</code> if the end of the stream has been reached
   * @throws IOException if an I/O error occurs
   */
  @Override
  public int read(final byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  /**
   * Read a single byte.
   *
   * @return either the byte read or <code>-1</code> if the end of the stream
   *         has been reached
   * @throws IOException if an I/O error occurs
   */
  @Override
  public int read() throws IOException {
    byte[] b = new byte[1];
    return read(b) == -1 ? -1 : b[0] & 0xFF;
  }

  /**
   * Close the stream. This method will cause the underlying {@link Reader} to be closed.
   *
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void close() throws IOException {
    reader.close();
  }
}
