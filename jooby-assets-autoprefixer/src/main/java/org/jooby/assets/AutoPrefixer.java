package org.jooby.assets;

import org.jooby.MediaType;

import com.typesafe.config.Config;

/**
 * <h1>auto-prefixer</h1>
 * <p>
 * <a href="https://github.com/postcss/postcss">PostCSS</a> plugin to parse CSS and add vendor
 * prefixes to CSS rules using values from <a href="http://caniuse.com">Can I Use</a>. It
 * is recommended by Google and used in Twitter, and Taobao.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * assets {
 *   pipeline {
 *     dev: [auto-prefixer]
 *     dist: [auto-prefixer]
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Once configured, write your CSS rules without vendor prefixes (in fact, forget about them
 * entirely):
 * </p>
 *
 * <pre>{@code
 * :fullscreen a {
 *   display: flex
 * }
 * }</pre>
 *
 * <p>Output:</p>
 *
 * <pre>{@code
 * :-webkit-full-screen a {
 *    display: -webkit-box;
 *    display: flex
 * }
 * :-moz-full-screen a {
 *    display: flex
 * }
 * :-ms-fullscreen a {
 *    display: -ms-flexbox;
 *    display: flex
 * }
 * :fullscreen a {
 *    display: -webkit-box;
 *    display: -ms-flexbox;
 *    display: flex
 * }
 * }</pre>
 *
 * <h2>options</h2>
 *
 * <pre>{@code
 * {
 *   auto-prefixer {
 *     browsers: ["> 1%", "IE 7"]
 *     cascade: true
 *     add: true
 *     remove: true
 *     supports: true
 *     flexbox: true
 *     grid: true
 *     stats: {}
 *   }
 * }
 * }</pre>
 *
 * <p>
 * For complete documentation about available options, please refer to the <a href="https://github.com/postcss/autoprefixer">autoprefixer</a> site.
 * </p>
 * @author edgar
 * @since 1.0.0
 */
public class AutoPrefixer extends AssetProcessor {

  @Override
  public boolean matches(final MediaType type) {
    return MediaType.css.matches(type);
  }

  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    return V8Context.run(v8 -> {
      return v8.invoke("auto-prefixer.js", source, options(), filename);
    });
  }

}
