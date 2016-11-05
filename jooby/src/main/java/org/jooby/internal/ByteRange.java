package org.jooby.internal;

import java.util.Iterator;
import java.util.function.BiFunction;

import org.jooby.Err;
import org.jooby.Status;

import com.google.common.base.Splitter;

import javaslang.Tuple;
import javaslang.Tuple2;

public class ByteRange {

  private static final String BYTES_EQ = "bytes=";

  public static Tuple2<Long, Long> parse(final String value) {
    if (!value.startsWith(BYTES_EQ)) {
      throw new Err(Status.REQUESTED_RANGE_NOT_SATISFIABLE, value);
    }
    BiFunction<String, Integer, Long> number = (it, offset) -> {
      try {
        return Long.parseLong(it.substring(offset));
      } catch (NumberFormatException | IndexOutOfBoundsException x) {
        throw new Err(Status.REQUESTED_RANGE_NOT_SATISFIABLE, value);
      }
    };

    Iterator<String> ranges = Splitter.on(',')
        .trimResults()
        .omitEmptyStrings()
        .split(value.substring(BYTES_EQ.length()))
        .iterator();
    if (ranges.hasNext()) {
      String range = ranges.next();
      int dash = range.indexOf('-');
      if (dash == 0) {
        return Tuple.of(-1L, number.apply(range, 1));
      } else if (dash > 0) {
        Long start = number.apply(range.substring(0, dash), 0);
        int endidx = dash + 1;
        Long end = endidx < range.length() ? number.apply(range, endidx) : -1L;
        return Tuple.of(start, end);
      }
    }
    throw new Err(Status.REQUESTED_RANGE_NOT_SATISFIABLE, value);
  }
}
