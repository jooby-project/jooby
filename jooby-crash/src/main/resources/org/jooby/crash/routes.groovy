package org.jooby.crash

import org.crsh.cli.Usage
import org.crsh.cli.Command

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import org.jooby.Route;

class routes
{
  @Usage("print application routes")
  @Command
  void main(InvocationContext context) {
    def routes = context.attributes.routes

    routes.each {
      def order = 0;
      def filter = it.filter()
      def pattern = it.pattern()

      if (filter instanceof Route.Before) {
        pattern = "{before}" + pattern
      } else if (filter instanceof Route.After) {
        pattern = "{after}" + pattern
      } else if (filter instanceof Route.Complete) {
        pattern = "{complete}" + pattern
      }

      context.provide([order:order, method: it.method(), pattern: pattern, consumes: it.consumes(), produces: it.produces(), name: it.name(), source: it.source()])
      order += 1
    }

    context.provide('\n\n')
    def sockets = context.attributes.websockets
    sockets.each {
      context.provide([method: 'WS', pattern: it.pattern, consumes: it.consumes(), produces: it.produces()])
    }
  }
}
