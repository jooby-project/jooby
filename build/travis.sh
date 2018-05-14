#!/bin/bash
# ALPN_VERSION="`groovy -e 'String v = (System.getProperty("java.vendor").contains("IBM") ? {def m = (["java", "-version"].execute().err.text =~ /(?s)java version "([0-9]*.[0-9]*.[0-9]*)".*Oracle jdk[0-9]u([0-9]*)-.*/); "${m[0][1]}_${m[0][2]}"}.call() : System.getProperty("java.version")); ((new URL("http://www.eclipse.org/jetty/documentation/9.4.x/alpn-chapter.html")).text =~ /ALPN vs. OpenJDK versions(.*)<\/table>/).each { m, s -> ( s =~ /([0-9].[0-9].[0-9]*u[0-9]*)[^0-9]*([0-9].[0-9]*.[0-9]*.v2[0-9]*)/).each { _, jdkv, alpnv -> if (v.equals(jdkv.replace("u","_"))){ println alpnv }}}'`"
# echo "ALPN $ALPN_VERSION"
# mvn -q -Dlogback.configurationFile=logback-travis.xml -Dalpn-boot-version=$ALPN_VERSION -Dcoverage.port=random -Dcoverage.securePort=random clean package coveralls:report -P coverage
mvn -q -Dlogback.configurationFile=logback-travis.xml -Dcoverage.port=random -Dcoverage.securePort=random clean package coveralls:report -P coverage
