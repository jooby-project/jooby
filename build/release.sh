#!/bin/bash
ALPN_VERSION="`groovy -e 'String v = (System.getProperty("java.vendor").contains("IBM") ? {def m = (["java", "-version"].execute().err.text =~ /(?s)java version "([0-9]*.[0-9]*.[0-9]*)".*Oracle jdk[0-9]u([0-9]*)-.*/); "${m[0][1]}_${m[0][2]}"}.call() : System.getProperty("java.version")); ((new URL("http://www.eclipse.org/jetty/documentation/9.4.x/alpn-chapter.html")).text =~ /ALPN vs. OpenJDK versions(.*)<\/table>/).each { m, s -> ( s =~ /([0-9].[0-9].[0-9]*u[0-9]*)[^0-9]*([0-9].[0-9]*.[0-9]*.v2[0-9]*)/).each { _, jdkv, alpnv -> if (v.equals(jdkv.replace("u","_"))){ println alpnv }}}'`"
echo "ALPN $ALPN_VERSION"

mvn -Dalpn-boot-version=$ALPN_VERSION -pl '!modules/coverage-report' clean deploy -P sonatype-oss-release

cd modules/jooby-bom

groovy bom.groovy > pom.xml

mvn -Dalpn-boot-version=$ALPN_VERSION clean deploy -P sonatype-oss-release
