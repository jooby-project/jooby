package org.jooby.crash

import org.crsh.cli.Usage
import org.crsh.cli.Command
import org.crsh.cli.completers.SystemPropertyNameCompleter
import org.crsh.command.InvocationContext
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import org.crsh.cli.Man
import org.crsh.cli.Argument
import org.crsh.cli.Option

import org.crsh.cli.completers.EnumCompleter
import java.util.regex.Pattern
import org.crsh.cli.Required

import com.google.common.base.Strings;

@Usage("application properties commands")
class conf
{

  @Usage("print configuration tree")
  @Command
  Object tree() {
    return configTree(context.attributes.conf.origin().description())
  }
  
  @Usage("print properties")
  @Command
  void props(InvocationContext context,
    @Usage("Apply a path filter, either a property name or config sub-tree")
    @Argument
    String path) {

    def conf = context.attributes.conf
    try {
      def local = path == null ? conf : conf.getConfig(path)
      def prefix = path == null ? "" : path + "."
  
      local.entrySet().each {
        context.provide([name: prefix + it.key, value: it.value.unwrapped()])
      }
    } catch (Exception x) {
      context.provide([name: path, value: conf.getAnyRef(path)])
    }
  }
  
  private String configTree(final String description) {
    return configTree(description.split(":\\s+\\d+,|,"), 0);
  }

  private String configTree(final String[] sources, final int i) {
    if (i < sources.length) {
      return new StringBuilder()
          .append(Strings.padStart("", i, (char)' '))
          .append("└── ")
          .append(sources[i])
          .append("\n")
          .append(configTree(sources, i + 1))
          .toString();
    }
    return "";
  }

}

@Retention(RetentionPolicy.RUNTIME)
@Usage("the property name")
@Man("The name of the property")
@Argument(name = "name", completer = SystemPropertyNameCompleter.class)
@interface PropertyName { }

@Retention(RetentionPolicy.RUNTIME)
@Usage("the property value")
@Man("The value of the property")
@Argument(name = "value")
@interface PropertyValue { }
