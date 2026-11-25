"""Rerun the existing business/UI harness into new Phase 12 evidence, preserving older reports."""
from pathlib import Path
import os, subprocess, tempfile
root=Path(__file__).resolve().parents[2]
evidence=root/'qa-reports/v1.0.6-ui/business-regression'
evidence.mkdir(parents=True,exist_ok=True)
fixture=Path(tempfile.mkdtemp(prefix='gymflow-qc-phase12-'))
classes=fixture/'classes';classes.mkdir()
environment=os.environ.copy()
for key in ['GYMFLOW_DB_PASSWORD','GYMFLOW_DB_TRUSTSTORE_PASSWORD','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS']:
    environment.pop(key,None)
for key,folder in [('XDG_DATA_HOME','data'),('LOCALAPPDATA','data'),('XDG_CONFIG_HOME','config'),('APPDATA','config')]:
    environment[key]=str(fixture/folder)
classpath=os.pathsep.join([str(root/'target/classes'),str(root/'target/libs/*')])
subprocess.run(['javac','-cp',classpath,'-d',str(classes),str(root/'qa-reports/tests/FinalUiHarness.java')],check=True,env=environment)
with (evidence/'execution.txt').open('w') as log:
    result=subprocess.run(['java',f'-Duser.home={fixture}','-Dprism.order=sw','-cp',str(classes)+os.pathsep+classpath,'FinalUiHarness',str(evidence/'screenshots')],cwd=root,env=environment,stdout=log,stderr=subprocess.STDOUT,timeout=300)
raise SystemExit(result.returncode)
