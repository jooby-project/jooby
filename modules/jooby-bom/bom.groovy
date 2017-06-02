#!/usr/bin/env groovy
// usage: groovy bom.groovy ../pom.xml > pom.xml
import groovy.xml.*

def xml = new XmlSlurper().parse(new File(args[0]));
def sw = new StringWriter()
def b = new MarkupBuilder(sw)
def template = new java.io.File("pom.template.xml").getText("UTF-8")

b.dependencies {
  for (m in xml.modules.module) {
          if (m.text().endsWith('-bom') || m.text().contains('coverage-report')) continue;
          dependency {
                  groupId('${project.groupId}');
                  artifactId(m.text());
                  version('${jooby.version}');
          }
  }
}

println template.replace("@version", xml.version.text()).replace("@dependencies", sw.toString())
