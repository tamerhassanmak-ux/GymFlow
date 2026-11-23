from pathlib import Path
import tempfile,os,subprocess
root=Path(__file__).resolve().parents[2];fixture=Path(tempfile.mkdtemp(prefix='gymflow-qc-fault-'));classes=fixture/'classes';classes.mkdir();env=os.environ.copy()
for key in ['GYMFLOW_DB_PASSWORD','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS']:env.pop(key,None)
for key,folder in [('XDG_DATA_HOME','data'),('LOCALAPPDATA','data'),('XDG_CONFIG_HOME','config'),('APPDATA','config')]:env[key]=str(fixture/folder)
cp=os.pathsep.join([str(root/'target/classes'),str(root/'target/libs/*')]);subprocess.run(['javac','-cp',cp,'-d',str(classes),str(root/'qa-reports/tests/OperationalFaultHarness.java')],env=env,check=True)
with (root/'qa-reports/evidence/final-qa/operational-faults.log').open('w') as log:result=subprocess.run(['java',f'-Duser.home={fixture}','-cp',str(classes)+os.pathsep+cp,'OperationalFaultHarness'],env=env,stdout=log,stderr=subprocess.STDOUT,timeout=30)
raise SystemExit(result.returncode)
