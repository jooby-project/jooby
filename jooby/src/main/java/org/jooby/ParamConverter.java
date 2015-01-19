package org.jooby;

import com.google.inject.TypeLiteral;

/**
 * Convert a request param (path, query, form) to something else.
 *
 * <h1>Registering a param converter</h1>
 * There are two ways of registering a param converter:
 *
 * <ol>
 * <li>Using the {@link Jooby#param(ParamConverter)} method</li>
 * <li>From a Guice module:
 * <pre>
 *   Multibinder&lt;ParamConverter&gt; pcb = Multibinder
        .newSetBinder(binder, ParamConverter.class);
     pcb.addBinding().to(MyParamConverter.class);
 * </pre>
 * </li>
 * </ol>
 * Param converters are executed in the order they were registered. The first converter that
 * resolved the type: wins!.
 *
 * <h1>Built-in param converters</h1>
 * These are the built-in param converters:
 * <ol>
 * <li>Primitives and String: convert to int, double, char, string, etc...</li>
 * <li>Enums</li>
 * <li>{@link java.util.Date}: It parses a date using the <code>application.dateFormat</code>
 * property.</li>
 * <li>{@link java.time.LocalDate}: It parses a date using the <code>application.dateFormat</code>
 * property.</li>
 * <li>{@link java.util.Locale}</li>
 * <li>Classes with a static method: <code>valueOf</code></li>
 * <li>Classes with a static method: <code>fromName</code></li>
 * <li>Classes with a static method: <code>fromString</code></li>
 * <li>Classes with a public constructor with one <code>String</code> argument</li>
 * <li>It is an Optional&lt;T&gt;, List&lt;T&gt;, Set&lt;T&gt; or SortedSet&lt;T&gt; where T
 * satisfies one of previous rules</li>
 * </ol>
 *
 * @author edgar
 * @see Jooby#param(ParamConverter)
 */
public interface ParamConverter {

  /**
   * Contains a reference to the next param converter that is going to be executed.
   *
   * @author edgar
   */
  interface Chain {
    /**
     * Ask to the next convert to resolve a type and make the conversion.
     *
     * @param type A type to resolve.
     * @param values Raw values. Types is one of two: <code>String</code> or <code>Upload</code>.
     * @return A converted value.
     * @throws Exception If conversion fails.
     */
    Object convert(TypeLiteral<?> type, Object[] values) throws Exception;
  }

  /**
   * <p>
   *  Convert one or more values to the required type. If the converter doesn't support the
   *  required type a call to {@link Chain#convert(TypeLiteral, Object[])} must be done.
   * </p>
   *
   * Example:
   * <pre>
   *  ParamConverter converter = (type, values, next) {@literal ->} {
   *    if (type.getRawType() == MyType.class) {
   *      // convert to MyType
   *      return ...;
   *    }
   *    // no luck! move next
   *    return next.convert(type, values);
   *  }
   * </pre>
   *
   * It's also possible to create generic/parameterized types too:
   *
   * <pre>
   *  public class MyType&lt;T&gt; {}
   *
   *  ParamConverter converter = (type, values, next) {@literal ->} {
   *    if (type.getRawType() == MyType.class) {
   *      // Creates a new type from current generic type
   *      TypeLiterale&lt;?&gt; paramType = TypeLiteral
   *        .get(((ParameterizedType) toType.getType()).getActualTypeArguments()[0]);
   *
   *      // Ask param converter to resolve the new type.
   *      Object result = next.convert(paramType, values);
   *      return new MyType(result);
   *    }
   *    // no luck! move next
   *    return next.convert(type, values);
   *  }
   * </pre>
   *
   *
   * @param type Requested type.
   * @param values Raw values. Types is one of two: <code>String</code> or <code>Upload</code>.
   * @param next The next converter in the chain.
   * @return A converted value.
   * @throws Exception If conversion fails.
   */
  Object convert(TypeLiteral<?> type, Object[] values, Chain next) throws Exception;

}
