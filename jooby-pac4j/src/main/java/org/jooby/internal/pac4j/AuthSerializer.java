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
package org.jooby.internal.pac4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Primitives;

public final class AuthSerializer {

  private static final String PREFIX = "b64~";

  public static final Object strToObject(final String value) {
    if (value == null || !value.startsWith(PREFIX)) {
      return value;
    }
    byte[] bytes = BaseEncoding.base64().decode(value.substring(PREFIX.length()));
    try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return stream.readObject();
    } catch (Exception ex) {
      throw new IllegalArgumentException("Can't de-serialize value " + value, ex);
    }
  }

  public static final String objToStr(final Object value) {
    if (value instanceof CharSequence || Primitives.isWrapperType(value.getClass())) {
      return value.toString();
    }
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream stream = new ObjectOutputStream(bytes)) {
      stream.writeObject(value);
      stream.flush();
      return PREFIX + BaseEncoding.base64().encode(bytes.toByteArray());
    } catch (Exception ex) {
      throw new IllegalArgumentException("Can't serialize value " + value, ex);
    }
  }

}
