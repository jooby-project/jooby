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
package org.jooby.internal.reqparam;

import static java.util.Objects.requireNonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jooby.Parser;

import com.google.inject.TypeLiteral;

public class DateParser implements Parser {

  private String dateFormat;

  public DateParser(final String dateFormat) {
    this.dateFormat = requireNonNull(dateFormat, "A dateFormat is required.");
  }

  @Override
  public Object parse(final TypeLiteral<?> type, final Parser.Context ctx) throws Exception {
    if (type.getRawType() == Date.class) {
      return ctx
          .param(values -> parse(dateFormat, values.get(0)))
          .body(body -> parse(dateFormat, body.text()));
    } else {
      return ctx.next();
    }
  }

  @Override
  public String toString() {
    return "Date";
  }

  private static Date parse(final String dateFormat, final String value) throws Exception {
    try {
      return new Date(Long.parseLong(value));
    } catch (NumberFormatException ex) {
      return new SimpleDateFormat(dateFormat).parse(value);
    }
  }
}
