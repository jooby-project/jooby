package jooby;

import com.google.common.annotations.Beta;
import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * Jooby doesn't use a custom scope annotation for request scoped object. Request scoped object
 * are binded using a child injector per each request.
 *
 * <h1>Providing request scoped objects</h1>
 * <p>
 * Jooby give you an extension point in order to register scope requested objects, here is how do
 * you usually do it.
 * </p>
 *
 * <pre>
 * class MyModule implements JoobyModule {
 *   void configure(Mode mode, Config config, Binder binder) {
 *     requestModule = Multibinder.newSetBinder(binder, RequestModule.class);
 *     requestModule.addBinding().toInstance(requestBinder -> {
 *       requestBinder.bind(MyService.class).to(...);
 *     })
 *   }
 * }
 * </pre>
 *
 * <h1>Do I have to provide request objects?</h1>
 * <p>
 * You don't. Request scoped object are useful if you need/want to have a single instance of an
 * object per request. A good example of such object is a db session, bc you want to reuse the
 * session during the request execution.
 * </p>
 * <p>
 * If you don't need/have that requirement. You shouldn't use request scoped object and just work
 * with prototype objects, as Guice suggest.
 * </p>
 *
 * @author edgar
 * @since 0.1.0
 */
@Beta
public interface RequestModule extends Module {

  @Override
  void configure(Binder binder);

}
