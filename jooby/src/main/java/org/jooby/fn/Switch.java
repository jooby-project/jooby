package org.jooby.fn;

import java.util.function.Predicate;

public interface Switch<In, Out> {

  Switch<In, Out> when(In value, Out result);

  Switch<In, Out> when(Predicate<In> predicate, Out result);

  Switch<In, Out> when(In value, ExSupplier<Out> fn);

  Switch<In, Out> when(Predicate<In> predicate, ExSupplier<Out> fn);

  Out get() throws Exception;

  Out otherwise(Out otherwise) throws Exception;

}
