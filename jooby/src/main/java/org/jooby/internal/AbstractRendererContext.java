/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.MediaType.Matcher;
import org.jooby.Renderer;
import org.jooby.Status;
import org.jooby.View;

import com.google.common.base.Joiner;

public abstract class AbstractRendererContext implements Renderer.Context {

  private Set<Renderer> renderers;

  private Matcher matcher;

  protected final Charset charset;

  private Map<String, Object> locals;

  private List<MediaType> produces;

  private boolean committed;

  public AbstractRendererContext(final Set<Renderer> renderers, final List<MediaType> produces,
      final Charset charset, final Map<String, Object> locals) {
    this.renderers = renderers;
    this.produces = produces;
    this.matcher = MediaType.matcher(produces);
    this.charset = charset;
    this.locals = locals;
  }

  public void render(final Object value) throws Exception {
    Iterator<Renderer> it = renderers.iterator();
    List<String> notFound = new LinkedList<>();
    while (!committed && it.hasNext()) {
      Renderer next = it.next();
      try {
        next.render(value, this);
      } catch (FileNotFoundException ex) {
        // view engine should recover from a template not found
        if (next instanceof View.Engine) {
          notFound.add(next.toString());
        } else {
          throw ex;
        }
      }
    }
    if (!committed) {
      if (notFound.size() > 0) {
        throw new FileNotFoundException("Template not found: " + ((View) value).name() + " in "
            + notFound);
      }
      throw new Err(Status.NOT_ACCEPTABLE, Joiner.on(", ").join(produces));
    }
  }

  @Override
  public Map<String, Object> locals() {
    return locals;
  }

  @Override
  public boolean accepts(final MediaType type) {
    return matcher.matches(type);
  }

  @Override
  public Renderer.Context type(final MediaType type) {
    // NOOP
    return this;
  }

  @Override
  public Renderer.Context length(final long length) {
    // NOOP
    return this;
  }

  @Override
  public Charset charset() {
    return charset;
  }

  @Override
  public void send(final CharBuffer buffer) throws Exception {
    type(MediaType.html);
    send(charset.encode(buffer));
  }

  @Override
  public void send(final Reader reader) throws Exception {
    type(MediaType.html);
    send(new ReaderInputStream(reader, charset));
  }

  @Override
  public void send(final String text) throws Exception {
    type(MediaType.html);
    _send(text);
    committed = true;
  }

  @Override
  public void send(final byte[] bytes) throws Exception {
    type(MediaType.octetstream);
    length(bytes.length);
    _send(bytes);
    committed = true;
  }

  @Override
  public void send(final ByteBuffer buffer) throws Exception {
    type(MediaType.octetstream);
    length(buffer.remaining());
    _send(buffer);
    committed = true;
  }

  @Override
  public void send(final FileChannel file) throws Exception {
    type(MediaType.octetstream);
    length(file.size());
    _send(file);
    committed = true;
  }

  @Override
  public void send(final InputStream stream) throws Exception {
    type(MediaType.octetstream);
    if (stream instanceof FileInputStream) {
      send(((FileInputStream) stream).getChannel());
    } else {
      _send(stream);
    }
    committed = true;
  }

  @Override
  public String toString() {
    return renderers.toString();
  }

  protected void _send(final String text) throws Exception {
    byte[] bytes = text.getBytes(charset);
    send(bytes);
  }

  protected abstract void _send(final byte[] bytes) throws Exception;

  protected abstract void _send(final ByteBuffer buffer) throws Exception;

  protected abstract void _send(final FileChannel file) throws Exception;

  protected abstract void _send(final InputStream stream) throws Exception;

}
