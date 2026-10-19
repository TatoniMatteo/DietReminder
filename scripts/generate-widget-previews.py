#!/usr/bin/env python3
"""Generate launcher preview bitmaps from the same brand and widget layout."""
from pathlib import Path
import base64,subprocess
root=Path(__file__).resolve().parents[1]
res=root/'android/src/main/res'
logo=base64.b64encode((res/'drawable-nodpi/brand_logo.png').read_bytes()).decode()
for name,title,subtitle,content in [
 ('meals','Prossimo pasto','Il tuo piano alimentare','<text x="28" y="142" font-size="32" font-weight="bold">Pranzo</text><text x="28" y="178" fill="#91e7a6" font-size="25">12:30</text><text x="28" y="222" fill="#a1a3b4" font-size="18">Pasta al pomodoro</text><text x="28" y="252" fill="#a1a3b4" font-size="18">Verdure di stagione</text>'),
 ('shopping','Lista della spesa','2 da acquistare','<g fill="none" stroke="#91e7a6" stroke-width="2"><rect x="28" y="118" width="24" height="24" rx="6"/><rect x="28" y="190" width="24" height="24" rx="6"/></g><text x="72" y="138" font-size="24">Mele</text><text x="72" y="166" fill="#a1a3b4" font-size="17">2 pz · Frutta e verdura</text><text x="72" y="210" font-size="24">Pane</text><text x="72" y="238" fill="#a1a3b4" font-size="17">250 g · Pane e cereali</text>')]:
 center='Prossimo' if name=='meals' else 'Apri lista'
 svg=f'''<svg xmlns="http://www.w3.org/2000/svg" width="560" height="360" viewBox="0 0 560 360"><rect width="560" height="360" rx="38" fill="#181a21"/><g font-family="sans-serif" fill="#e9e9f2"><image href="data:image/png;base64,{logo}" x="28" y="26" width="48" height="48"/><text x="92" y="46" font-size="22" font-weight="bold">{title}</text><text x="92" y="72" font-size="16" fill="#a1a3b4">{subtitle}</text>{content}<text x="280" y="324" text-anchor="middle" fill="#91e7a6" font-size="18" font-weight="bold">{center}</text></g><path d="m48 306-10 10 10 10 M512 306l10 10-10 10 M505 55l16-16 M505 39h16v16" fill="none" stroke="#91e7a6" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg>'''
 subprocess.run(['rsvg-convert','-o',str(res/f'drawable-nodpi/widget_preview_{name}.png')],input=svg.encode(),check=True)
 old=res/f'drawable/widget_preview_{name}.xml'
 if old.exists():old.unlink()
