"""Prepare an OSV query from Maven's resolved runtime graph; summarize a saved reply."""
from pathlib import Path
import json
import subprocess
import sys
root=Path(__file__).resolve().parents[2]
import os
p=root/Path(os.environ.get('GYMFLOW_QA_EVIDENCE','qa-reports/evidence/release-fix'))
if len(sys.argv)>1 and sys.argv[1]=='prepare':
    output=p/'runtime-dependencies.txt'
    subprocess.run(['mvn','-o','dependency:list','-DincludeScope=runtime',f'-DoutputFile={output}'],check=True,cwd=root,stdout=subprocess.DEVNULL)
    queries=[]
    for line in output.read_text().splitlines():
        parts=line.strip().split(':')
        if len(parts)>=5 and parts[2]=='jar':
            queries.append({'package':{'ecosystem':'Maven','name':parts[0]+':'+parts[1]},'version':parts[-2]})
    queries=list({json.dumps(q,sort_keys=True):q for q in queries}.values())
    (p/'osv-query.json').write_text(json.dumps({'queries':queries},indent=2))
    print(len(queries),'runtime package/version queries')
else:
    queries=json.loads((p/'osv-query.json').read_text())['queries']
    results=json.loads((p/'osv-results.json').read_text())['results']
    assert len(queries)==len(results),'Incomplete vulnerability reply'
    findings=[{'package':q,'vulnerabilities':v['vulns']} for q,v in zip(queries,results) if v.get('vulns')]
    (p/'osv-summary.json').write_text(json.dumps(findings,indent=2))
    print(f'OSV: {len(queries)} packages queried; {len(findings)} affected package versions')
    raise SystemExit(bool(findings))
