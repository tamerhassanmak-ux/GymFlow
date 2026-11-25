#!/usr/bin/env bash
set -euo pipefail
# Requires: mvn test-compile/package and a previously built app-image, display/Xvfb, JDK21 for preparation.
application=${1:-target/installers/GymFlow}
runtime="$application/lib/runtime"
input=target/packaged-ui-input
output=${GYMFLOW_UI_RUNNER_DEST:-target/packaged-ui-runner}
fixture=target/packaged-ui-profile
console=${GYMFLOW_JUNIT_CONSOLE:-$HOME/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.12.1/junit-platform-console-standalone-1.12.1.jar}
mkdir -p "$input/libs" target/ui-qa-launcher "$fixture/qa-home" "$fixture/config" "$fixture/data"
javac --release 21 -d target/ui-qa-launcher qa-reports/tests/UiQaLauncher.java
jar --create --file "$input/ui-qa-launcher.jar" -C target/ui-qa-launcher .
jar --create --file "$input/ui-tests.jar" -C target/test-classes .
cp "$application/lib/app/GymFlow.jar" "$input/GymFlow.jar"
cp "$application"/lib/app/libs/*.jar "$input/libs/"
cp "$console" "$input/"
# The runtime-image is reused, not rebuilt from the development JDK.
jpackage --type app-image --name GymFlowUiQA --input "$input" --main-jar ui-qa-launcher.jar --main-class UiQaLauncher --runtime-image "$runtime" --dest "$output" --java-options '-Dgymflow.ui=true' --java-options "-Duser.home=$(realpath "$fixture/qa-home")" --java-options "-Dgymflow.ui.evidence=$(realpath qa-reports/v1.0.6-ui)/packaged"
if [[ ${GYMFLOW_UI_PREPARE_ONLY:-false} == true ]]; then
    echo "Packaged UI runner prepared at $output/GymFlowUiQA"
    exit 0
fi
# Do not inherit any configured remote database credentials into tests.
env -u GYMFLOW_DB_PASSWORD -u GYMFLOW_DB_TRUSTSTORE_PASSWORD XDG_DATA_HOME="$(realpath "$fixture/data")" XDG_CONFIG_HOME="$(realpath "$fixture/config")" "$output/GymFlowUiQA/bin/GymFlowUiQA" execute --select-class=com.mycompany.lazar_app.ResponsiveUiTest --fail-if-no-tests --disable-ansi-colors --reports-dir=qa-reports/v1.0.6-ui/packaged/junit
env -u GYMFLOW_DB_PASSWORD -u GYMFLOW_DB_TRUSTSTORE_PASSWORD XDG_DATA_HOME="$(realpath "$fixture/data")" XDG_CONFIG_HOME="$(realpath "$fixture/config")" "$output/GymFlowUiQA/bin/GymFlowUiQA" execute --select-class=com.mycompany.lazar_app.WindowMaximizeTest --fail-if-no-tests --disable-ansi-colors --reports-dir=qa-reports/v1.0.6-ui/packaged/maximize
