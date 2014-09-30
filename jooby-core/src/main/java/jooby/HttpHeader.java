package jooby;

import com.google.common.annotations.Beta;
import com.google.common.collect.FluentIterable;

/**
 * Mutable version of {@link Variant} useful for setting response headers.
 *
 * @author edgar
 * @since 0.1.0
 * @see Response#header(String)
 */
@Beta
public interface HttpHeader extends Variant {

  /**
   * Set a boolean header. It overrides any previous value.
   *
   * @param value A header value.
   * @return This instance.
   */
  default HttpHeader setBoolean(final boolean value) {
    return setString("" + value);
  }

  /**
   * Set a boolean header. It overrides any previous value.
   *
   * @param values A header value.
   * @return This instance.
   */
  default HttpHeader setBoolean(final Iterable<Boolean> values) {
    return setString(FluentIterable.from(values).transform(v -> v.toString()).toList());
  }

  /**
   * Set a byte header. It overrides any previous value.
   *
   * @param value A header value.
   * @return This instance.
   */
  default HttpHeader setByte(final byte value) {
    return setString("" + value);
  }

  /**
   * Set a bye header. It overrides any previous value.
   *
   * @param values A header value.
   * @return This instance.
   */
  default HttpHeader setByte(final Iterable<Byte> values) {
    return setString(FluentIterable.from(values).transform(v -> v.toString()).toList());
  }

  /**
   * Set a short header. It overrides any previous value.
   *
   * @param value A header value.
   * @return This instance.
   */
  default HttpHeader setShort(final short value) {
    return setString("" + value);
  }

  /**
   * Set a short header. It overrides any previous value.
   *
   * @param values A header value.
   * @return This instance.
   */
  default HttpHeader setShort(final Iterable<Short> values) {
    return setString(FluentIterable.from(values).transform(v -> v.toString()).toList());
  }

  /**
   * Set an int header. It overrides any previous value.
   *
   * @param value A header value.
   * @return This instance.
   */
  default HttpHeader setInt(final int value) {
    return setString("" + value);
  }

  /**
   * Set an int header. It overrides any previous value.
   *
   * @param values A header value.
   * @return This instance.
   */
  default HttpHeader setInt(final Iterable<Integer> values) {
    return setString(FluentIterable.from(values).transform(v -> v.toString()).toList());
  }

  /**
   * Set a float header. It overrides any previous value.
   *
   * @param value A header value.
   * @return This instance.
   */
  default HttpHeader setFloat(final float value) {
    return setString("" + value);
  }

  /**
   * Set a float header. It overrides any previous value.
   *
   * @param values A header value.
   * @return This instance.
   */
  default HttpHeader setFloat(final Iterable<Float> values) {
    return setString(FluentIterable.from(values).transform(v -> v.toString()).toList());
  }

  /**
   * Set a double header. It overrides any previous value.
   *
   * @param value A header value.
   * @return This instance.
   */
  default HttpHeader setDouble(final double value) {
    return setString("" + value);
  }

  /**
   * Set a double header. It overrides any previous value.
   *
   * @param values A header value.
   * @return This instance.
   */
  default HttpHeader setDouble(final Iterable<Double> values) {
    return setString(FluentIterable.from(values).transform(v -> v.toString()).toList());
  }

  /**
   * Set a string header. It overrides any previous value.
   *
   * @param value A header value.
   * @return This instance.
   */
  HttpHeader setString(String value);

  /**
   * Set a string header. It overrides any previous value.
   *
   * @param values A header value.
   * @return This instance.
   */
  HttpHeader setString(Iterable<String> values);

  /**
   * Set a long header. It overrides any previous value.
   *
   * @param value A header value.
   * @return This instance.
   */
  HttpHeader setLong(long value);

  /**
   * Set a values header. It overrides any previous value.
   *
   * @param values A header value.
   * @return This instance.
   */
  HttpHeader setLong(Iterable<Long> values);
}
