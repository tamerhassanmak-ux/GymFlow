"""Portable CI entry point for the existing 35-scenario Java harness."""
import os
from pathlib import Path
import subprocess
import tempfile
import time

root = Path(__file__).resolve().parents[2]
fixture = Path(tempfile.mkdtemp(prefix="gymflow-qc-"))
classes = fixture / "classes"
classes.mkdir()
environment = os.environ.copy()
for key, folder in (("XDG_DATA_HOME", "data"), ("LOCALAPPDATA", "data"),
                    ("XDG_CONFIG_HOME", "config"), ("APPDATA", "config")):
    environment[key] = str(fixture / folder)
# Never inherit remote database credentials into disposable tests.
environment.pop("GYMFLOW_DB_PASSWORD", None)
for option in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"):
    environment.pop(option, None)
classpath = os.pathsep.join((str(root / "target/classes"), str(root / "target/libs/*")))
subprocess.run(["javac", "-cp", classpath, "-d", str(classes),
                str(root / "qa-reports/tests/QcHarness.java")], check=True, env=environment)
started = time.perf_counter()
result = subprocess.run(["java", f"-Duser.home={fixture}", "-cp",
                         str(classes) + os.pathsep + classpath, "QcHarness"], env=environment)
print(f"Legacy harness wall time: {time.perf_counter() - started:.3f} s", flush=True)
raise SystemExit(result.returncode)
