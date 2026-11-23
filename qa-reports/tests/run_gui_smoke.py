"""Explicit local-display acceptance probes; uses only disposable user directories."""
from pathlib import Path
import tempfile
import os
import subprocess
import json
import hashlib
root = Path(__file__).resolve().parents[2]
evidence = root / Path(os.environ.get("GYMFLOW_QA_EVIDENCE", "qa-reports/evidence/release-fix"))

def isolated_environment(prefix):
    fixture = Path(tempfile.mkdtemp(prefix=prefix))
    env = os.environ.copy()
    env.pop("GYMFLOW_DB_PASSWORD", None)
    for option in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"):
        env.pop(option, None)
    for key, folder in (("XDG_DATA_HOME", "data"), ("LOCALAPPDATA", "data"),
                        ("XDG_CONFIG_HOME", "config"), ("APPDATA", "config")):
        env[key] = str(fixture / folder)
    return fixture, env

fixture, env = isolated_environment("gymflow-qc-release-fxml-")
classes = fixture / "classes"
classes.mkdir()
cp = os.pathsep.join((str(root / "target/GymFlow.jar"), str(root / "target/libs/*")))
subprocess.run(["javac", "-cp", cp, "-d", str(classes),
                str(root / "qa-reports/tests/FxmlProbe.java")], check=True, env=env)
args = ["java", f"-Duser.home={fixture}", "-Dprism.order=sw", "-cp", str(classes)+os.pathsep+cp,
        "FxmlProbe"] + [p.name for p in sorted((root / "src/main/resources/fxml").glob("*.fxml"))]
with (evidence / "fxml-runtime.log").open("w") as log:
    result = subprocess.run(args, env=env, stdout=log, stderr=subprocess.STDOUT, timeout=55)
if result.returncode:
    raise SystemExit(result.returncode)
paths = json.loads((evidence / "package-paths.json").read_text())
image = Path(paths["output"]) / "GymFlowQC"
packaged_jar = image / "lib/app/GymFlow.jar"
digest = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
assert digest(root / "target/GymFlow.jar") == digest(packaged_jar), "Packaged JAR differs"
(evidence / "artifact-sha256.txt").write_text(digest(packaged_jar)+"  target/GymFlow.jar and packaged GymFlow.jar\n")
fixture, env = isolated_environment("gymflow-qc-release-startup-")
env["JAVA_TOOL_OPTIONS"] = f"-Duser.home={fixture}"
with (evidence / "artifact-launch.log").open("w") as log:
    try:
        result = subprocess.run([str(image / "bin/GymFlowQC")], env=env, stdout=log, stderr=subprocess.STDOUT, timeout=12)
        exit_status = result.returncode
    except subprocess.TimeoutExpired:
        exit_status = "stopped at 12-second first-run smoke boundary"
content = (evidence / "artifact-launch.log").read_text()
assert "Database connection initialized successfully using: SQLite" in content
assert "Database migrations completed successfully." in content
assert "[ OK ] 014" in content
assert "Exception" not in content
(evidence / "package-result.json").write_text(json.dumps({"packaged_jar_matches": True,
    "sqlite_initialized": True, "fourteen_migrations_completed": True,
    "startup_exit": exit_status, "full_login_or_shutdown_verified": False}, indent=2))
print("FXML loads and packaged SQLite/migration smoke passed; first-run account/license flow remains manual.")
