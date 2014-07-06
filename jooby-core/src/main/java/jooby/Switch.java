package jooby;

import java.util.function.Predicate;

public interface Switch<In, Out> {

  interface Fn<T> {
    T apply() throws Exception;
  }

  Switch<In, Out> when(In value, Out result);

  Switch<In, Out> when(Predicate<In> predicate, Out result);

  Switch<In, Out> when(In value, Fn<Out> fn);

  Switch<In, Out> when(Predicate<In> predicate, Fn<Out> fn);

  Out get() throws Exception;

  Out otherwise(Out otherwise) throws Exception;

}
