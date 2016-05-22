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
package org.jooby.internal.parser;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.inject.Inject;

import org.jooby.Parser;

import com.google.inject.TypeLiteral;

public class LocalDateParser implements Parser {

  private DateTimeFormatter formatter;

  @Inject
  public LocalDateParser(final DateTimeFormatter formatter) {
    this.formatter = requireNonNull(formatter, "A date time formatter is required.");
  }

  @Override
  public Object parse(final TypeLiteral<?> type, final Parser.Context ctx) throws Throwable {
    if (type.getRawType() == LocalDate.class) {
      return ctx
          .param(values -> parse(formatter, values.get(0)))
          .body(body -> parse(formatter, body.text()));
    } else {
      return ctx.next();
    }
  }

  @Override
  public String toString() {
    return "LocalDate";
  }

  private static LocalDate parse(final DateTimeFormatter formatter, final String value) {
    try {
      Instant epoch = Instant.ofEpochMilli(Long.parseLong(value));
      ZonedDateTime zonedDate = epoch.atZone(
          Optional.ofNullable(formatter.getZone())
              .orElse(ZoneId.systemDefault())
          );
      return zonedDate.toLocalDate();
    } catch (NumberFormatException ex) {
      return LocalDate.parse(value, formatter);
    }
  }

}
