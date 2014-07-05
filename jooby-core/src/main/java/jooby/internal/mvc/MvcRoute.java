package jooby.internal.mvc;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import jooby.Request;
import jooby.Response;
import jooby.Route;
import jooby.TemplateProcessor;
import jooby.internal.ParameterDefinition;
import jooby.mvc.Template;
import net.sf.cglib.reflect.FastMethod;

class MvcRoute implements Route {

  private FastMethod route;

  private ParamResolver resolver;

  public MvcRoute(final FastMethod route, final ParamResolver resolver) {
    this.route = requireNonNull(route, "The route is required.");
    this.resolver = requireNonNull(resolver, "The resolver is required.");
  }

  @Override
  public void handle(final Request request, final Response response) throws Exception {
    Method method = route.getJavaMethod();
    List<ParameterDefinition> parameters = resolver.resolve(method);
    Object[] args = parameters.stream().map(p -> p.get(request)).toArray();
    Object handler = request.get(method.getDeclaringClass());
    Object result = route.invoke(handler, args);

    // default view name
    String defaultViewName = Optional.ofNullable(method.getAnnotation(Template.class))
        .map(template -> template.name().isEmpty() ? method.getName() : template.name())
        .orElse(method.getName());
    response.header(TemplateProcessor.VIEW_NAME, defaultViewName);

    // send it!
    response.send(result);
  }
}
