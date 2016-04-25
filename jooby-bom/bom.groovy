#!/usr/bin/env groovy
// usage: groovy bom.groovy ../pom.xml > pom.xml
import groovy.xml.*

def xml = new XmlSlurper().parse(new File(args[0]));
def sw = new StringWriter()
def b = new MarkupBuilder(sw)

def i = 0;

b.print('<?xml version="1.0" encoding="UTF-8"?>\n')

b.project("xmlns": "http://maven.apache.org/POM/4.0.0", 'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance", 'xsi:schemaLocation': "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd") {

  b.modelVersion('4.0.0')
  b.groupId(xml.groupId)
  b.artifactId('jooby-bom')
  b.version(xml.version)
  b.packaging(xml.packaging)
  b.name('jooby-bom')
  b.description('Jooby (Bill of Materials)')
  b.url(xml.url)

  b.properties {
    'jooby.version'(xml.version)
  }

  b.licenses {
    for (l in xml.licenses.license) {
      license {
        name(l.name)
        url(l.url)
      }
    }
  }

  b.developers {
    for (d in xml.developers.developer) {
      developer {
        id(d.id)
        name(d.name)
        url(d.url)
      }
    }
  }

  b.scm {
    connection(xml.scm.connection)
    developerConnection(xml.scm.developerConnection)
    url(xml.scm.url)
  }

  b.distributionManagement {
    repository {
      id(xml.distributionManagement.repository.id)
      name(xml.distributionManagement.repository.name)
      url(xml.distributionManagement.repository.url)
    }
  }

  b.dependencyManagement {
          dependencies {
                  for (m in xml.modules.module) {
                          if (m.text().endsWith('-bom') || m.text().contains('coverage-report')) continue;
                          dependency {
                                  groupId('${project.groupId}');
                                  artifactId(m.text());
                                  version('${jooby.version}');
                          }
                  }
          }
  }
}

println sw;
