import groovy.xml.*
import java.nio.file.*

def modules = project.collectedProjects.find {it.name == 'modules'}.modules.sort()
modules.add(0, 'jooby');

def skip = ["jooby-bom", "jooby-gradle-setup"]

def depsw = new StringWriter()
def deps = new MarkupBuilder(depsw)
deps.dependencyManagement {
    deps.dependencies {
        for (module in modules) {
            if (!skip.contains(module)) {
                dependency {
                    groupId('io.jooby')
                    artifactId(module)
                    version('${project.version}')
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
