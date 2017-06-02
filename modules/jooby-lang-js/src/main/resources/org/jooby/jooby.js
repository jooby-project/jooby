var Results = org.jooby.Results;

/**
 * Creates a new Jooby app.
 *
 * @param {Function} fn Startup function. Optional.
 */
var jooby = function (fn) {
  var METHODS = ['get', 'post', 'put', 'delete', 'patch', 'head', 'trace', 'options', 'connect'];
  var Jooby = org.jooby.Jooby,
      Request = Java.extend(org.jooby.internal.js.JsRequest),
      Response = Java.extend(org.jooby.internal.js.JsResponse),
      Handler = org.jooby.Route.Filter,
      Module = org.jooby.Jooby.Module,
      Config = com.typesafe.config.Config,
      Consumer = Java.type('java.util.function.Consumer'),
      Predicate = Java.type('java.util.function.Predicate'),
      Parser = org.jooby.Parser,
      Class = java.lang.Class,
      Key = com.google.inject.Key,
      TypeLiteral = com.google.inject.TypeLiteral,
      Renderer = org.jooby.Renderer,
      delegate = new Jooby();

  if (!this.__jooby_) {
    this.__jooby_ = function () {
      return delegate;
    };
  }
  var resolveType = function (type) {
    var clazz = type.class;
    if (clazz === Class.class || clazz === Key.class || clazz === TypeLiteral.class) {
      return type;
    }
    return clazz;
  };

  var typeOf = function (args) {
    var names = [];
    for (var i = 0; i < args.length; i++) {
      var arg = args[i];
      if (arg.___jooby__) {
        names.push('Jooby');
      } else if (Module.class.isInstance(arg)) {
        names.push('Module');
      } else if (Config.class.isInstance(arg)) {
        names.push('Config');
      } else {
        var str = new java.lang.String(arg);
        if (str.startsWith('[JavaClass')) {
          names.push('JavaClass');
        } else if (arg.class) {
          names.push(arg.class.simpleName);
        } else {
          names.push(typeof arg);
        }
      }
    }
    return names.length == 0 ? '.' : names.join('.');
  };

  /**
   * wrap a function into a Route.Filter
   */
  var filter = function (fn) {
    return new Handler()
    {
      handle: function (req, rsp, chain) {

        var jsreq = new Request(req)
        {
          require: function () {
            if (arguments.length === 1) {
              return req.require(resolveType(arguments[0]));
            } else {
              return req.require(arguments[0], resolveType(arguments[1]));
            }
          }
        }
        ;
        var jsrsp = new Response(rsp)
        {
          send: function (val) {
            if (val) {
              if (val.class) {
                // java object
                rsp.send(val);
              } else {
                // js object/literal
                jsrsp.sendjs(val);
              }
            }
          }
        }
        ;

        jsrsp.send(fn(jsreq, jsrsp, chain));
      }
    }
    ;
  };

  /**
   * handler route variants: .get(..), .post(...);
   */
  var routes = {
    '.': function (method, args) {
      return delegate[method]();
    },
    'String.function': function (method, args) {
      return delegate[method](args[0], filter(args[1]));
    },
    'String.String.function': function (method, args) {
      return delegate[method](args[0], args[1], filter(args[2]));
    },
    'String.String.String.function': function (method, args) {
      return delegate[method](args[0], args[1], args[2], filter(args[3]));
    }
  };

  var route = function (method, args) {
    return routes[typeOf(args)](method, args);
  };

  /**
   * handle .use('/pattern')
   *          .get('/:id', function () {});
   */
  var ns = function (_ns) {
    var h = {
      'String.function': function (m, args) {
        return _ns[m](args[0], filter(args[1]));
      },
      'function': function (m, args) {
        return _ns[m](filter(args[0]));
      }
    };
    var ns = {};

    ns.name = function (name) {
      _ns.name(name);
      return ns;
    };

    ns.produces = function (produces) {
      _ns.produces(produces);
      return ns;
    };

    ns.consumes = function (consumes) {
      _ns.consumes(consumes);
      return ns;
    };

    var methods = METHODS.concat(['all']);
    for each(m in methods) {
      ns[m] = function (it) {
        return function () {
          h[typeOf(arguments)](it, arguments);
          return ns;
        };
      }(m);
    }
    return ns;
  };

  /**
   * handle .use(..) variants
   */
  var use = {
    '*': function (args) {
      delegate.use(args[0]);
      return app;
    },
    'JavaClass': function (args) {
      delegate.use(resolveType(args[0]));
      return app;
    },
    'Jooby': function (args) {
      delegate.use(args[0].___jooby__);
      return app;
    },
    'function': function (args) {
      var fn = args[0];
      delegate.use(new Module()
      {
        configure: function (env, conf, binder) {
          fn(env, conf, binder);
        }
      ,

        config: function () {
          return ConfigFactory.empty();
        }
      }
      )
      ;
      return app;
    },
    'String': function (args) {
      return ns(delegate.use(args[0]));
    },
    'String.function': function (args) {
      return delegate.use(args[0], filter(args[1]));
    },
    'String.String.function': function (args) {
      return delegate.use(args[0], args[1], filter(args[2]));
    }
  };

  /**
   * .on handlers
   */
  var on = {
    'String.function': function (args) {
      delegate.on(args[0], new Consumer()
      {
        accept: function (conf) {
          args[1](conf);
        }
      }
      )
    },
    'function.function': function (args) {
      delegate.on(args[0], new Consumer()
      {
        accept: function (conf) {
          args[1](conf);
        }
      }
      )
    }
  };

  var app = {
    ___jooby__: delegate
  };

  app.use = function () {
    var fn = use[typeOf(arguments)] || use['*'];
    return fn(arguments);
  };

  app.on = function () {
    on[typeOf(arguments)](arguments);
    return app;
  };

  app.require = function (type) {
    return delegate.require(resolveType(type));
  };

  app.session = function (session) {
    return delegate.session(session);
  };

  app.parser = function (parser) {
    delegate.parser(parser);
    return app;
  };

  app.renderer = function (renderer) {
    delegate.renderer(renderer);
    return app;
  };

  app.err = function (handler) {
    delegate.err(handler);
    return app;
  };

  app.ws = function (pattern, handler) {
    return delegate.ws(pattern, handler);
  };

  app.assets = function (path, location) {
    if (location) {
      return delegate.assets(path, location);
    }
    return delegate.assets(path);
  }

  // handlers on jooby
  for each(m in METHODS) {
    app[m] = function (it) {
      return function () {
        return route(it, arguments);
      };
    }(m);
  }

  /**
   * Start callback (fancy!)
   */
  var start = function (fn) {
    var loaded = false,
        loadmozilla = function () {
          if (!loaded) {
            load('nashorn:mozilla_compat.js');
            loaded = true;
          }
        };

    return function () {
      for (var i in arguments) {
        var str = new java.lang.String(arguments[i]);
        if (str.startsWith('[JavaPackage')) {
          loadmozilla();
          importPackage(arguments[i]);
        }
      }
      fn.apply(app, arguments);
      return app;
    };
  };

  app.stop = function () {
    delegate.stop();
  };

  app.toString = function () {
    return delegate.toString();
  }

  if (typeof fn === 'function') {
    return start(fn);
  } else {
    return app;
  }
};
