"""Generate web and Android assets from branding/logo.svg (rsvg-convert + ImageMagick)."""
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'android/src/main/res'

def run(*args):
    subprocess.run([str(x) for x in args], check=True)

for name in ('logo.svg', 'favicon.svg'):
    shutil.copyfile(ROOT / 'branding/logo.svg', ROOT / 'web/public' / name)
mail_logo = ROOT / 'backend/src/main/resources/mail/logo.png'
mail_logo.parent.mkdir(parents=True, exist_ok=True)
run('rsvg-convert', '-w', '96', '-h', '96', ROOT / 'branding/logo.svg', '-o', mail_logo)
with tempfile.TemporaryDirectory() as tmp:
    logo = Path(tmp) / 'logo.png'
    run('rsvg-convert', '-w', '1024', '-h', '1024', ROOT / 'branding/logo.svg', '-o', logo)
    (RES / 'drawable-nodpi').mkdir(exist_ok=True)
    shutil.copyfile(logo, RES / 'drawable-nodpi/brand_logo.png')
    run('convert', logo, '-resize', '144x144', '-gravity', 'center', '-background', '#101116', '-extent', '180x180', ROOT / 'web/public/apple-touch-icon.png')
    # Adaptive icons reserve their outer third for launcher masking and motion.
    run('convert', logo, '-resize', '660x660', '-gravity', 'center', '-background', 'none', '-extent', '1080x1080', RES / 'drawable-nodpi/app_icon_foreground.png')
    for density, size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
        for name in ('app_icon.webp', 'app_icon_round.webp'):
            target = RES / f'mipmap-{density}' / name
            run('convert', logo, '-resize', f'{int(size*.78)}x{int(size*.78)}', '-gravity', 'center', '-background', '#101116', '-extent', f'{size}x{size}', target)
            if 'round' in name:
                mask = Path(tmp) / 'round-mask.png'
                run('convert', '-size', f'{size}x{size}', 'xc:none', '-fill', 'white', '-draw', f'circle {size/2},{size/2} {size/2},0', mask)
                run('convert', target, mask, '-alpha', 'on', '-compose', 'DstIn', '-composite', target)
    run('convert', logo, '-resize', '400x400', '-gravity', 'center', '-background', '#101116', '-extent', '512x512', ROOT / 'android/src/main/app_icon-playstore.png')
