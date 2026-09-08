#!/usr/bin/env python3
import base64,html,json,zipfile
from pathlib import Path
root=Path(__file__).resolve().parents[1]
inv=json.loads((root/'docs/asset-inventory.json').read_text())
cards=[]
with zipfile.ZipFile(root/'app/libs/original-game.jar') as z:
    for e in inv['entries']:
        if 'dimensions' not in e:continue
        src=base64.b64encode(z.read(e['path'])).decode();w,h=e['dimensions']
        cards.append(f'<article><div class="art"><img alt="{html.escape(e["path"])}" src="data:image/png;base64,{src}"></div><h3>{html.escape(e["path"])}</h3><p>{w} × {h} pixels · {e["bytes"]:,} bytes</p><code>{e["sha256"][:20]}…</code></article>')
count=len(cards)
page='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Original game · asset inspection</title><style>
*{box-sizing:border-box}body{margin:0;background:#0c1017;color:#e9eef6;font:15px/1.65 system-ui,sans-serif}main{max-width:1280px;margin:auto;padding:60px 28px}.eyebrow{color:#bea1ff;letter-spacing:.16em;font-size:12px;text-transform:uppercase}h1{font-size:clamp(32px,5vw,58px);line-height:1.08;letter-spacing:-.04em;margin:22px 0}header p{max-width:800px;color:#a7b4c9}.stats{display:flex;gap:12px;flex-wrap:wrap;margin:30px 0 40px}.stat{background:#192231;padding:16px 24px;border-radius:12px}.stat b{font-size:26px;display:block}.stat span{color:#9daec6;font-size:12px}.note{border-left:3px solid #ac86ef;padding:12px 20px;background:#151a25;margin:26px 0 40px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:18px}article{background:#161e2a;border:1px solid #293448;border-radius:14px;overflow:hidden}article h3,article p,article code{margin:0 18px}article h3{margin-top:16px;font-size:16px}article p{color:#a7b4c9;font-size:13px}article code{display:block;margin-bottom:18px;font-size:11px;color:#7687a1}.art{height:240px;background:repeating-conic-gradient(#27303b 0% 25%,#1e2631 0% 50%) 50%/16px 16px;display:flex;align-items:center;justify-content:center;padding:12px;overflow:auto}.art img{max-width:100%;max-height:100%;object-fit:contain;image-rendering:pixelated}footer{color:#7889a3;margin-top:40px}a{color:#c8adff}code{overflow-wrap:anywhere}
</style><main><header><div class="eyebrow">SAN / Original archive inspection</div><h1>The original pixels.<br>The original game.</h1><p>Assets extracted directly from the user-supplied GTA 5 MOD archive. No AI-generated replacements or redrawn sprites. This gallery is an inventory, not a claim that all gameplay has been tested.</p></header>'''
page+=f'<div class="stats"><div class="stat"><b>{count}</b><span>PNG image files</span></div><div class="stat"><b>6</b><span>level files, including Creep</span></div><div class="stat"><b>5</b><span>languages</span></div><div class="stat"><b>20</b><span>packed palettes</span></div><div class="stat"><b>10</b><span>original game classes</span></div></div>'
page+='<div class="note"><strong>Identity of the supplied game</strong><br>The manifest calls the game “GTA5 Mobile,” but the entry point is <code>SaintsRow2</code>. The dialogue, missions and credits are from the 2008 Saints Row 2 mobile game. The port preserves this supplied mod; it is not desktop GTA V.</div><section class="grid">'+''.join(cards)+'</section>'
page+='<footer>Source SHA-256: <code>'+inv['source_sha256']+'</code><p>All original asset rights remain with their respective owners. Publishing permission was confirmed by the repository owner; that permission has not been independently verified.</p></footer></main></html>'
(root/'docs/asset-gallery.html').write_text(page)
print(f'Gallery contains {count} original PNG files')
