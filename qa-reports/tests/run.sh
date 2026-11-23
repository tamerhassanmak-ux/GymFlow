#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
qa_root=$(mktemp -d /tmp/gymflow-qc-XXXXXX)
export XDG_DATA_HOME="$qa_root/data"
export XDG_CONFIG_HOME="$qa_root/config"
mkdir -p "$qa_root/classes"
javac -cp 'target/classes:target/libs/*' -d "$qa_root/classes" qa-reports/tests/QcHarness.java
/usr/bin/time -v java -Duser.home="$qa_root" -cp "$qa_root/classes:target/classes:target/libs/*" QcHarness
