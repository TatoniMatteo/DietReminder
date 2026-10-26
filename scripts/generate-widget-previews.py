#!/usr/bin/env python3
"""Generate launcher previews matching the course panel and shopping list widgets."""
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
res = root / 'android/src/main/res'
for name in ('meals', 'shopping'):
    if name == 'meals':
        heading = 'Pranzo'
        subtitle = 'Prossimo pasto · Il tuo piano alimentare'
        body = '''
<text x="48" y="164" font-size="32" font-weight="bold" fill="#E2EBE2">Pasta al pomodoro</text>
<text x="48" y="208" font-size="32" font-weight="bold" fill="#E2EBE2">Verdure di stagione</text>

<text x="280" y="317" text-anchor="middle" font-size="18" font-weight="bold">Prossimo</text>
<path d="m58 300-10 10 10 10 M502 300l10 10-10 10" fill="none" stroke="#D0E8D3" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>'''
    else:
        heading = 'Lista della spesa'
        subtitle = '2 da acquistare'
        body = '''
<g fill="none" stroke="#D0E8D3" stroke-width="2"><rect x="46" y="120" width="24" height="24" rx="6"/><rect x="46" y="200" width="24" height="24" rx="6"/></g>
<text x="92" y="140" font-size="24" font-weight="bold">Mele</text><text x="92" y="167" fill="#BCCBBC" font-size="17">2 pz · Frutta e verdura</text>
<text x="92" y="220" font-size="24" font-weight="bold">Pane</text><text x="92" y="247" fill="#BCCBBC" font-size="17">250 g · Pane e cereali</text>'''
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="560" height="360" viewBox="0 0 560 360">
<rect width="560" height="360" rx="38" fill="#1B251E"/>
<g font-family="sans-serif" fill="#E2EBE2"><text x="24" y="44" font-size="24" font-weight="bold">{heading}</text>
<text x="24" y="70" font-size="16" fill="#BCCBBC">{subtitle}</text>{body}</g>
<path d="M508 45l16-16 M508 29h16v16" fill="none" stroke="#BCCBBC" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg>'''
    subprocess.run(['rsvg-convert', '-o', str(res / f'drawable-nodpi/widget_preview_{name}.png')], input=svg.encode(), check=True)
