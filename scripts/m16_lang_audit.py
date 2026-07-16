#!/usr/bin/env python3
"""M16: zh_cn / en_us key parity audit (no gameplay changes)."""
import json
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1]
zh = json.loads((root / 'src/main/resources/assets/seeking_immortals/lang/zh_cn.json').read_text(encoding='utf-8-sig'))
en = json.loads((root / 'src/main/resources/assets/seeking_immortals/lang/en_us.json').read_text(encoding='utf-8-sig'))
zh_only = sorted(set(zh) - set(en))
en_only = sorted(set(en) - set(zh))
print(f'zh={len(zh)} en={len(en)} zh_only={len(zh_only)} en_only={len(en_only)}')
if zh_only:
    print('zh-only sample:', zh_only[:20])
if en_only:
    print('en-only sample:', en_only[:20])
sys.exit(0 if not zh_only and not en_only else 1)
