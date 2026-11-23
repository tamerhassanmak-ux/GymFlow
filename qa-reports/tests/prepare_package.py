"""Generate a disposable Linux release-candidate app image, never deploy it."""
from pathlib import Path
import tempfile
import shutil
import subprocess
import json
import os
root=Path(__file__).resolve().parents[2]
evidence=root/Path(os.environ.get('GYMFLOW_QA_EVIDENCE','qa-reports/evidence/release-fix'))
p=Path(tempfile.mkdtemp(prefix='gymflow-qc-release-package-'))
i=p/'input';i.mkdir();shutil.copy2(root/'target/GymFlow.jar',i/'GymFlow.jar');shutil.copytree(root/'target/libs',i/'libs')
paths={'root':str(p),'input':str(i),'output':str(p/'output')}
(evidence/'package-paths.json').write_text(json.dumps(paths,indent=2))
args=['jpackage','--type','app-image','--name','GymFlowQC','--app-version','1.0.4','--input',str(i),'--main-jar','GymFlow.jar','--main-class','com.mycompany.lazar_app.Lazar_APP','--dest',paths['output'],'--java-options','--module-path=$APPDIR/libs','--java-options','--add-modules=javafx.controls,javafx.fxml','--java-options','--add-reads=org.xerial.sqlitejdbc=slf4j.api']
(evidence/'jpackage-command.json').write_text(json.dumps(args,indent=2))
with (evidence/'jpackage.log').open('w') as log:result=subprocess.run(args,stdout=log,stderr=subprocess.STDOUT)
raise SystemExit(result.returncode)
