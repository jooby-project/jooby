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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.inject.Inject;

import org.jooby.ParamConverter;

import com.google.inject.TypeLiteral;

public class LocalDateParamConverter implements ParamConverter {

  private DateTimeFormatter formatter;

  @Inject
  public LocalDateParamConverter(final DateTimeFormatter formatter) {
    this.formatter = requireNonNull(formatter, "A date time formatter is required.");
  }

  @Override
  public Object convert(final TypeLiteral<?> toType, final Object[] values, final Context ctx)
      throws Exception {
    if (toType.getRawType() == LocalDate.class) {
      try {
        Instant epoch = Instant.ofEpochMilli(Long.parseLong((String) values[0]));
        ZonedDateTime zonedDate = epoch.atZone(
            Optional.ofNullable(formatter.getZone())
                .orElse(ZoneId.systemDefault())
            );
        return zonedDate.toLocalDate();
      } catch (NumberFormatException ex) {
        return LocalDate.parse((CharSequence) values[0], formatter);
      }
    } else {
      return ctx.convert(toType, values);
    }
  }

}
