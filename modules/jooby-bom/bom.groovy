import java.nio.file.Files
import java.nio.file.Paths

def depsw = new StringWriter()
def deps = new MarkupBuilder(depsw)

def propsw = new StringWriter()
def props = new MarkupBuilder(propsw)

def propertyName(String groupId, String artifactId) {
  def names = [artifactId]
  names.addAll(artifactId.split('-'))
  names.addAll(groupId.split("\\.").reverse())

  for (name in names) {
    def version = project.properties.get(name + '.version')
    if (version != null) {
      return '${' + name + '.version}'
    }
  }
  throw new IllegalArgumentException("Unable to find version for <" + groupId + ":" + artifactId + ">")
}

deps.dependencyManagement {
  deps.dependencies {
    for (d in project.dependencyManagement.dependencies) {
      dependency {
        groupId(d.groupId)
        artifactId(d.artifactId)
        version(propertyName(d.groupId, d.artifactId))
      }
    }
  }
}

props.properties {
    "jooby.version"(project.version)

    for (p in project.properties.sort()) {
      if (p.key.endsWith("version")) {
        "${p.key}"("${p.value}")
      }
    }
}

def template = new File("modules/jooby-bom/pom.template.xml").getText("UTF-8")


def content = template.replace("@version", project.version)
    .replace("@dependencies", depsw.toString())
    .replace("@properties", propsw.toString())

def path = Paths.get("modules", "jooby-bom", "pom.xml")
Files.createDirectories(path.getParent())
Files.write(path, content.getBytes())
