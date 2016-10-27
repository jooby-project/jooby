/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.assets;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Router;
import org.jooby.handlers.AssetHandler;
import org.jooby.internal.assets.AssetHandlerWithCompiler;
import org.jooby.internal.assets.AssetVars;
import org.jooby.internal.assets.LiveCompiler;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javaslang.control.Try;

/**
 * <h1>assets</h1>
 * <p>
 * The asset module is library to concatenate, minify or compress JavaScript and CSS
 * assets. It also adds the ability to write these assets in other languages and process/compile
 * them to another language. Finally, it help you to write high quality code by validate JavaScript
 * and CSS too.
 * </p>
 * <p>
 * A variety of processors are available (jshint, csslint, jscs, uglify, closure-compiler, etc..),
 * but
 * also you might want to write your owns.
 * </p>
 *
 * <h2>getting started</h2>
 * <p>
 * The first thing you need to do is to define your assets. Definition is done in your
 * <code>.conf</code> file or in a special file: <code>assets.conf</code>.
 * </p>
 *
 * <strong>assets.conf</strong>
 * <pre>
 * assets {
 *   fileset {
 *     home: [assets/home.js, assets/home.css]
 *   }
 * }
 * </pre>
 *
 * <strong>App.java</strong>
 * <pre>
 *  {
 *    use(new Assets());
 *  }
 * </pre>
 *
 * <p>
 * The assets module will publish 4 request local variables for <code>home</code> fileset:
 * <code>_css</code> and <code>_js</code> each of these variables is a list of string with the
 * corresponding files. There are two more variables: <code>_styles</code> and <code>_scripts</code>
 * :
 * </p>
 *
 * <pre>
 *   &lt;html&gt;
 *   &lt;head&gt;
 *     {{{home_styles}}}
 *   &lt;body&gt;
 *     ...
 *     {{{home_scripts}}
 *   &lt;/body&gt;
 *   &lt;/head&gt;
 *   &lt;/html&gt;
 * </pre>
 *
 * <p>
 * The variables: <code>_styles</code> and <code>_scripts</code> produces one ore more
 * <code>link</code> and <code>script</code> tags. The example above, shows you how to
 * render these variables in the template engine of your choice (handlebars, here).
 * </p>
 *
 * <p>
 * Now, let's see how to configure the Maven plugin to process our assets at build-time:
 * </p>
 *
 * <strong>pom.xml</strong>
 *
 * <pre>
 *   &lt;plugin&gt;
 *     &lt;groupId&gt;org.jooby&lt;/groupId&gt;
 *     &lt;artifactId&gt;jooby-maven-plugin&lt;/artifactId&gt;
 *     &lt;executions&gt;
 *       &lt;execution&gt;
 *         &lt;goals&gt;
 *           &lt;goal&gt;assets&lt;/goal&gt;
 *         &lt;/goals&gt;
 *       &lt;/execution&gt;
 *     &lt;/executions&gt;
 *   &lt;/plugin&gt;
 * </pre>
 *
 * <p>
 * The plugin will process all your assets and include them to the final <code>.jar</code>,
 * <code>.zip</code> or <code>.war</code>.
 * </p>
 *
 * <p>
 * Cool, isn't?
 * </p>
 *
 * <h2>how it works?</h2>
 * <p>
 * The <code>assets.fileset</code> defines all your assets. In <code>dev</code> assets are
 * rendered/processed at runtime. In <code>prod</code> at built-time.
 * </p>
 * <p>
 * Assets are rendered at runtime using <code>*_styles</code> or <code>*_scripts
 * </code> variables. So you define your assets in one single place: <code>assets.conf</code>.
 * </p>
 * <p>
 * Also, at build-time, the asset compiler concatenates all the files from a fileset and
 * generate a fingerprint. The fingerprint is a SHA-1 hash of the content of the fileset. Thanks to
 * the fingerprint an asset can be cached it for ever! Defaults cache max age is:
 * <code>365 days</code>.
 * </p>
 * <p>
 * That isn't all! the <code>*_styles</code> and <code>*_scripts</code> are updated with the
 * fingerprint version of assets, so you don't have to do or change anything in your views! It just
 * works!!!
 * </p>
 *
 * <h2>fileset</h2>
 * <p>
 * A fileset is a group of assets within a name. The fileset name is expanded into 4 request local
 * variables, for example:
 * </p>
 * <pre>
 * assets {
 *   fileset {
 *     home: [assets/home.js, assets/home.css]
 *     pageA: [assets/pageA.js, assets/pageA.css]
 *   }
 * }
 * </pre>
 *
 * <p>
 * Produces 4 variables for <code>home</code>:
 * </p>
 *
 * <ul>
 * <li>home_css: a list of all the <code>css</code> files</li>
 * <li>home_styles: a string, with all the <code>css</code> files rendered as <code>link</code> tags
 * </li>
 * <li>home_js: a list of all the <code>js</code> files</li>
 * <li>home_scripts: a string, with all the <code>js</code> files rendered as <code>script</code>
 * tags</li>
 * </ul>
 *
 * <p>
 * Another 4 variables will be available for the <code>pageA</code> fileset!
 * </p>
 *
 * <h3>extending filesets</h3>
 * <p>
 * Extension or re-use of filesets is possible via the: <code>&lt;</code> operator:
 * </p>
 * <pre>
 * assets {
 *   fileset {
 *     base: [js/lib/jquery.js, css/normalize.css]
 *     home &lt; base: [js/home.js]
 *     pageA &lt; base: [js/pageA.js]
 *   }
 * }
 * </pre>
 *
 * <h2>processors</h2>
 * <p>
 * An {@link AssetProcessor} usually checks or modify an asset content in one way or another. They
 * are defined in the <code>assets.conf</code> files using the <code>pipeline</code> construction:
 * </p>
 *
 * <pre>
 * assets {
 *   fileset {
 *     home: [js/home.js, css/home.css]
 *   }
 *
 *   pipeline {
 *     dev: [jshint, jscs, csslint, sass]
 *     dist: [uglify, sass, clean-css]
 *   }
 * }
 * </pre>
 *
 * <p>
 * Example above, defines a <strong>pipeline</strong> for development (dev) and one generic for prod
 * (dist).
 * </p>
 * <p>
 * In <code>dev</code> the code will be checked it against js-hint, jscs and csslint! But
 * also, we want to use sass for css!!
 * </p>
 * <p>
 * The generic <code>dist</code> will be used it for any other environment and here we just want to
 * optimize our javascript code with uglify, compile sass to css and then optimize the css using
 * clean-css!!
 * </p>
 *
 * <p>
 * For more information about processor, have a look at the {@link AssetProcessor} doc.
 * </p>
 *
 * <h2>aggregators</h2>
 * <p>
 * Contributes new or dynamically generated content to a <code>fileset</code>. Content generated by
 * an aggregator might be processed by an {@link AssetProcessor}.
 * </p>
 *
 * <h3>how to use it?</h3>
 * <p>
 * First thing to do is to add the dependency:
 * </p>
 * <pre>
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;org.jooby&lt;/groupId&gt;
 *     &lt;artifactId&gt;jooby-assets-dr-svg-sprites&lt;/artifactId&gt;
 *     &lt;scope&gt;provided&lt;/scope&gt;
 *   &lt;/dependency&gt;
 * </pre>
 *
 * <p>
 * Did you see the <strong>provided</strong> scope? We just need the aggregator for development,
 * because assets are processed at runtime. For <code>prod</code>, assets are processed at
 * built-time via Maven/Gradle plugin, so we don't need it. This also, helps to keep our
 * dependencies and the jar size small.
 * </p>
 *
 * <p>
 * Now we have the dependency all we have to do is to add the <code>svg-sprites</code> aggregator to
 * a fileset:
 * </p>
 *
 * <pre>
 * assets {
 *   fileset {
 *     home: [
 *       // 1) Add the aggregator to a fileset
 *       svg-sprites,
 *       css/style.css,
 *       js/app.js
 *     ]
 *   }
 *
 *   svg-sprites {
 *     // 2) The `css/sprite.css` file is part of the `home` fileset.
 *     spritePath: "css/sprite.css"
 *
 *     spriteElementPath: "images/svg",
 *   }
 * }
 * </pre>
 *
 * <p>
 * Here for example, the <code>svg-sprites</code> aggregator contributes the
 * <code>css/sprite.css</code> file to the <code>home</code> fileset. The fileset then looks like:
 * </p>
 *
 * <pre>
 * assets {
 *   fileset {
 *     home: [
 *       css/sprite.css,
 *       css/style.css,
 *       js/app.js
 *     ]
 *   }
 * }
 * </pre>
 * <p>
 * Replaces the aggregator name with one or more files from {@link AssetAggregator#fileset()}
 * method.
 * </p>
 *
 * @author edgar
 * @since 0.11.0
 */
public class Assets implements Jooby.Module {

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    Try.run(() -> {
      boolean dev = "dev".equals(env.name());
      ClassLoader loader = getClass().getClassLoader();
      Config conf = conf(dev, loader, config);
      String cpath = config.getString("application.path");
      AssetCompiler compiler = new AssetCompiler(loader, conf);

      Router routes = env.router();
      routes.use("*", "*", new AssetVars(compiler, cpath)).name("/assets/vars");
      // live compiler?
      boolean watch = conf.hasPath("assets.watch") ? conf.getBoolean("assets.watch") : dev;
      if (watch) {
        LiveCompiler liveCompiler = new LiveCompiler(conf, compiler);
        env.onStart(liveCompiler::start);
        env.onStop(liveCompiler::stop);
        routes.use("*", "*", liveCompiler).name("/assets/compiler");
      }

      AssetHandler handler = dev
          ? new AssetHandlerWithCompiler("/", compiler)
              .etag(false)
              .lastModified(false)
          : new AssetHandler("/")
              .etag(conf.getBoolean("assets.etag"))
              .cdn(conf.getString("assets.cdn"))
              .lastModified(conf.getBoolean("assets.lastModified"));

        handler.maxAge(conf.getString("assets.cache.maxAge"));

      compiler.patterns().forEach(pattern -> routes.get(pattern, handler));

    }).get();
  }

  private Config conf(final boolean dev, final ClassLoader loader, final Config conf) {
    final Config[] confs;
    if (!dev) {
      confs = new Config[]{
          ConfigFactory.parseResources(loader,
              "assets." + conf.getString("application.env").toLowerCase() + ".conf"),
          ConfigFactory.parseResources(loader, "assets.dist.conf"),
          ConfigFactory.parseResources(loader, "assets.conf") };
      for (Config it : confs) {
        if (!it.isEmpty()) {
          return it.withFallback(conf).resolve();
        }
      }
    }
    return ConfigFactory.parseResources(loader, "assets.conf").withFallback(conf).resolve();
  }

}
