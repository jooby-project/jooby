welcome = { ->
  def registry = crash.context.attributes.registry
  def conf = crash.context.attributes.conf
  def appname = conf.getString("application.name")
  def version = conf.getString("application.version")

  try {
    return registry.require("application.banner", String.class) + " v" + version + "\n"
  } catch (Exception x) {
    return """
Welcome to $appname v$version
"""
  }
}

prompt = { ->
  def env = crash.context.attributes.conf.getString("application.env")
  return env + "> ";
}