package jooby;

import com.google.common.collect.FluentIterable;

public interface HttpMutableField extends HttpField {

  default HttpMutableField setBoolean(final boolean value) {
    return setString("" + value);
  }

  default HttpMutableField setBoolean(final Iterable<Boolean> values) {
    return setString(FluentIterable.from(values).transform(v -> v.toString()).toList());
  }

  default HttpMutableField setByte(final byte value) {
    return setString("" + value);
  }

  default HttpMutableField setByte(final Iterable<Byte> values) {
    return setString(FluentIterable.from(values).transform(v -> v.toString()).toList());
  }

  default HttpMutableField setShort(final short value) {
    return setString("" + value);
  }

  default HttpMutableField setShort(final Iterable<Short> values) {
    return setString(FluentIterable.from(values).transform(v -> v.toString()).toList());
  }

  default HttpMutableField setInt(final int value) {
    return setString("" + value);
  }

  default HttpMutableField setInt(final Iterable<Integer> values) {
    return setString(FluentIterable.from(values).transform(v -> v.toString()).toList());
  }

  default HttpMutableField setFloat(final float value) {
    return setString("" + value);
  }

  default HttpMutableField setFloat(final Iterable<Float> values) {
    return setString(FluentIterable.from(values).transform(v -> v.toString()).toList());
  }

  default HttpMutableField setDouble(final double value) {
    return setString("" + value);
  }

  default HttpMutableField setDouble(final Iterable<Double> values) {
    return setString(FluentIterable.from(values).transform(v -> v.toString()).toList());
  }

  HttpMutableField setString(String value);

  HttpMutableField setString(Iterable<String> values);

  HttpMutableField setLong(long value);

  HttpMutableField setLong(Iterable<Long> values);
}
