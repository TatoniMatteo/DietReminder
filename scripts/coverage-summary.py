#!/usr/bin/env python3
"""Print coverage honestly by test type; do not add percentages from different suites."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
reports={
    'Backend (unit + PostgreSQL)': 'backend/build/reports/jacoco/test/jacocoTestReport.xml',
    'Shared contracts': 'common/build/reports/jacoco/test/jacocoTestReport.xml',
    'HTTP client': 'client/build/reports/jacoco/test/jacocoTestReport.xml',
    'Android unit': 'android/build/reports/coverage/local/report.xml',
    'Android device': 'android/build/reports/coverage/androidTest/debug/connected/report.xml',
}
for label,relative in reports.items():
    path=root/relative
    if not path.exists(): print(f'{label}: not generated');continue
    counters={node.attrib['type']:node.attrib for node in ET.parse(path).getroot().findall('counter')}
    def ratio(kind):
        counter=counters.get(kind,{})
        covered=int(counter.get('covered',0));total=covered+int(counter.get('missed',0))
        return f'{100*covered/total:.1f}%' if total else 'n/a'
    print(f'{label}: lines {ratio("LINE")}, branches {ratio("BRANCH")}')
web=root/'web/coverage/coverage-summary.json'
if web.exists():
    total=json.loads(web.read_text())['total']
    print(f'Web unit/component: lines {total["lines"]["pct"]}%, branches {total["branches"]["pct"]}%')
print('Browser end-to-end results are separate from unit/component coverage.')
