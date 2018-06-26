#!/usr/bin/env groovy
// usage: groovy bom.groovy ../pom.xml > pom.xml
import groovy.xml.*

def jooby = new XmlSlurper().parse(new File("../../pom.xml"))
def modules = new XmlSlurper().parse(new File("../pom.xml"))
def sw = new StringWriter()
def props = new StringWriter()
MarkupBuilder deps = new MarkupBuilder(sw)
MarkupBuilder bprops = new MarkupBuilder(props)
def template = new File("pom.template.xml").getText("UTF-8")

deps.dependencies {
  for (m in modules.modules.module) {
          if (m.text().endsWith('-bom') || m.text().contains('coverage-report')) continue
      dependency {
                  groupId('${project.groupId}')
          artifactId(m.text())
          version('${jooby.version}')
      }
  }
}

bprops.properties {
  "jooby.version"(jooby.version.text())
  jooby.properties.each {properties ->
      properties.children().each {property ->
          "${property.name()}"("${property.text()}")
      }
  }
}

println template.replace("@version", jooby.version.text()).replace("@dependencies", sw.toString()).replace("@properties", props.toString())
