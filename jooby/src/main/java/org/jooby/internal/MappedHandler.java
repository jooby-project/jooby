package org.jooby.internal;

import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Chain;
import org.jooby.Route.Filter;
import org.jooby.Route.Mapper;

import javaslang.CheckedFunction2;
import javaslang.control.Try;

@SuppressWarnings({"unchecked", "rawtypes" })
public class MappedHandler implements Filter {

  private CheckedFunction2<Request, Response, Object> supplier;
  private Mapper mapper;

  public MappedHandler(final CheckedFunction2<Request, Response, Object> supplier, final Route.Mapper mapper) {
    this.supplier = supplier;
    this.mapper = mapper;
  }

  @Override
  public void handle(final Request req, final Response rsp, final Chain chain) throws Throwable {
    Object input = supplier.apply(req, rsp);
    Object output = Try
        .of(() -> mapper.map(input))
        .recover(x -> Match(x).of(Case(instanceOf(ClassCastException.class), input)))
        .get();
    rsp.send(output);
    chain.next(req, rsp);
  }

}
