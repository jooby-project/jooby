import groovy.xml.*
import java.nio.file.*


def depsw = new StringWriter()
def deps = new MarkupBuilder(depsw)

deps.dependencyManagement {
    deps.dependencies {
        for (d in project.dependencyManagement.dependencies) {
            if (d.groupId.equals("io.jooby")) {
                dependency {
                    groupId(d.groupId)
                    artifactId(d.artifactId)
                    version('${project.version}')
                    if (d.type != null && !d.type.equals("jar")) {
                        type(d.type)
                    }
                    if (d.scope != null) {
                        scope(d.scope)
                     }
            }
        }
        }
    }
}

def template = new File("modules/jooby-bom/pom.template.xml").getText("UTF-8")

def content = template.replace("@version", project.version)
    .replace("@dependencies", depsw.toString())

def path = Paths.get("modules", "jooby-bom", "pom.xml")
Files.createDirectories(path.getParent())
Files.write(path, content.getBytes())
