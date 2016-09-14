package org.jooby.pebble;

import java.util.List;
import java.util.Map;

import org.jooby.Env;

import com.google.common.collect.ImmutableMap;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Function;

public class XssExt extends AbstractExtension {

  private Map<String, Function> xss;

  public XssExt(final Env env) {
    this.xss = ImmutableMap.of("xss", new Function() {

      @Override
      public List<String> getArgumentNames() {
        return null;
      }

      @Override
      public Object execute(final Map<String, Object> args) {
        args.remove("_context");
        args.remove("_self");
        Object[] values = args.values().toArray(new Object[args.size()]);
        String[] xss = new String[values.length - 1];
        System.arraycopy(values, 1, xss, 0, values.length - 1);
        return env.xss(xss).apply(values[0].toString());
      }
    });
  }

  @Override
  public Map<String, Function> getFunctions() {
    return xss;
  }
}
