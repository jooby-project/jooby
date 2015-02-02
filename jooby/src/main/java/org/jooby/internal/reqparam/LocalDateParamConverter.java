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
  public Object convert(final TypeLiteral<?> toType, final Object[] values, final Chain chain)
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
      return chain.convert(toType, values);
    }
  }

}
