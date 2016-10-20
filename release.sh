mvn -pl '!coverage-report' clean deploy -P sonatype-oss-release

cd jooby-bom

groovy bom.groovy ../pom.xml > pom.xml

mvn clean deploy -P sonatype-oss-release
