#!/usr/bin/env bash
set -euo pipefail
# Run from repository root after mvn verify. Contains no customer data/configuration.
version=$(python3 -c 'import xml.etree.ElementTree as E; print(E.parse("pom.xml").findtext("{http://maven.apache.org/POM/4.0.0}version"))')
input=target/package-input
destination=${GYMFLOW_PACKAGE_DEST:-target/installers}
mkdir -p "$input/libs" "$destination"
cp target/GymFlow.jar "$input/"
cp target/libs/*.jar "$input/libs/"
modules=java.scripting,java.sql,java.sql.rowset,java.naming,java.management,java.desktop,java.logging,java.security.jgss,java.security.sasl,java.xml,java.net.http,java.rmi,java.transaction.xa,java.instrument,jdk.crypto.ec,jdk.unsupported,jdk.charsets,jdk.localedata,jdk.zipfs
jpackage --type app-image --name GymFlow --app-version "$version" --vendor MAKSOFT --input "$input" --main-jar GymFlow.jar --main-class com.mycompany.lazar_app.Launcher --add-modules "$modules" --java-options '-Dfile.encoding=UTF-8' --icon packaging/linux/gymflow.png --dest "$destination"
# Smoke test the bundled runtime without connecting to any configured customer database.
"$destination/GymFlow/bin/GymFlow" --self-test
jpackage --type deb --name GymFlow --app-version "$version" --vendor MAKSOFT --app-image "$destination/GymFlow" --linux-shortcut --linux-package-deps "libgtk-3-0 | libgtk-3-0t64" --linux-package-name gymflow --dest "$destination"
(cd "$destination" && sha256sum ./*.deb > SHA256SUMS)
