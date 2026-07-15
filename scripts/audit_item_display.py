import json
import re
from pathlib import Path

def load_json(p: str):
    raw = Path(p).read_bytes()
    text = raw.decode("utf-8")
    return json.loads(text), text, raw

zh, zh_text, zh_raw = load_json("src/main/resources/assets/seeking_immortals/lang/zh_cn.json")
en, en_text, en_raw = load_json("src/main/resources/assets/seeking_immortals/lang/en_us.json")
bulk = json.loads(Path("src/main/resources/assets/seeking_immortals/catalog_bulk_items.json").read_text(encoding="utf-8"))

report = []

def add(section, rows):
    report.append(f"## {section}")
    if not rows:
        report.append("- none")
    else:
        for r in rows:
            report.append(f"- {r}")
    report.append("")

def is_cjk(s: str) -> bool:
    return bool(re.search(r"[一-鿿]", s or ""))

def is_snake_id(s: str) -> bool:
    return bool(re.fullmatch(r"[a-z][a-z0-9_]{2,}", s or ""))

def looks_mojibake(s: str) -> bool:
    if not s:
        return False
    if "�" in s:
        return True
    # common mojibake markers for Chinese mis-decoded as latin1/cp1252
    if re.search(r"(Ã.|Â.|å.|æ.|ç.|é.|è.|ä.|ö.|ü.|ï¿½|锟斤拷)", s):
        return True
    return False

def has_code_keyword(s: str) -> bool:
    return bool(re.search(r"\b(null|true|false|return|class|public|void|TODO|FIXME)\b", s or "")) or ("Component." in (s or ""))

# Encoding
add("Encoding", [
    f"zh_cn.json: UTF-8 valid, BOM={zh_raw.startswith(b'\\xef\\xbb\\xbf')}, keys={len(zh)}, bytes={len(zh_raw)}",
    f"en_us.json: UTF-8 valid, BOM={en_raw.startswith(b'\\xef\\xbb\\xbf')}, keys={len(en)}, bytes={len(en_raw)}",
    f"catalog_bulk_items.json items={len(bulk.get('items', []))}",
])

item_keys = [k for k in zh if k.startswith("item.seeking_immortals.")]
block_keys = [k for k in zh if k.startswith("block.seeking_immortals.")]
tooltip_keys = [k for k in zh if k.startswith("tooltip.seeking_immortals.")]

raw_id, no_cjk, garbled, empty = [], [], [], []
for k in item_keys:
    v = zh[k]
    if not isinstance(v, str) or not v.strip():
        empty.append(f"{k} => {v!r}")
        continue
    item_id = k.split(".")[-1]
    if v == item_id or is_snake_id(v):
        raw_id.append(f"{k} => {v}")
    if not is_cjk(v):
        no_cjk.append(f"{k} => {v}")
    if looks_mojibake(v):
        garbled.append(f"{k} => {v}")

add("Item names (zh_cn)", [
    f"item keys: {len(item_keys)}",
    f"raw-id / snake_case as name: {len(raw_id)}",
    f"no CJK characters: {len(no_cjk)}",
    f"garbled/mojibake/replacement: {len(garbled)}",
    f"empty: {len(empty)}",
])
if raw_id:
    add("Raw-id item names", raw_id[:80])
if no_cjk:
    add("No-CJK item names", no_cjk[:80])
if garbled:
    add("Garbled item names", garbled[:80])
if empty:
    add("Empty item names", empty[:40])

b_raw, b_no, b_gar = [], [], []
for k in block_keys:
    v = zh[k]
    if not isinstance(v, str):
        continue
    block_id = k.split(".")[-1]
    if v == block_id or is_snake_id(v):
        b_raw.append(f"{k} => {v}")
    if not is_cjk(v):
        b_no.append(f"{k} => {v}")
    if looks_mojibake(v):
        b_gar.append(f"{k} => {v}")
add("Block names (zh_cn)", [
    f"block keys: {len(block_keys)}",
    f"raw-id: {len(b_raw)}",
    f"no CJK: {len(b_no)}",
    f"garbled: {len(b_gar)}",
])
if b_raw or b_no or b_gar:
    add("Block issues", b_raw + b_no + b_gar)

t_raw, t_gar, t_code = [], [], []
for k in tooltip_keys:
    v = zh.get(k, "")
    if not isinstance(v, str):
        continue
    if is_snake_id(v):
        t_raw.append(f"{k} => {v}")
    if looks_mojibake(v):
        t_gar.append(f"{k} => {v}")
    if has_code_keyword(v):
        t_code.append(f"{k} => {v}")
add("Tooltips (zh_cn)", [
    f"tooltip keys: {len(tooltip_keys)}",
    f"raw-id: {len(t_raw)}",
    f"garbled: {len(t_gar)}",
    f"code-keyword: {len(t_code)}",
])
if t_raw or t_gar or t_code:
    add("Tooltip issues", t_raw + t_gar + t_code)

# format placeholders on item/block/tooltip
fmt_bad = []
for k, v in zh.items():
    if not (k.startswith("item.") or k.startswith("block.") or k.startswith("tooltip.")):
        continue
    if not isinstance(v, str):
        continue
    i = 0
    while i < len(v):
        if v[i] == "%":
            if i + 1 < len(v) and v[i + 1] == "%":
                i += 2
                continue
            m = re.match(r"%(\d+\$)?([a-zA-Z])", v[i:])
            if not m:
                fmt_bad.append(f"{k} => {v}")
                break
            i += len(m.group(0))
        else:
            i += 1
add("Item/Block/Tooltip format placeholders", [f"invalid: {len(fmt_bad)}"] + fmt_bad[:30])

# bulk
b_bad = []
b_ok = 0
for it in bulk.get("items", []):
    item_id = it.get("id", "")
    desc = it.get("description", "")
    if not isinstance(desc, str) or not desc.strip():
        b_bad.append(f"{item_id}: empty/non-string")
        continue
    if is_snake_id(desc) or desc == item_id:
        b_bad.append(f"{item_id}: desc is id => {desc}")
    elif looks_mojibake(desc):
        b_bad.append(f"{item_id}: garbled => {desc[:60]}")
    elif re.search(r"[A-Za-z]", desc) and not is_cjk(desc):
        b_bad.append(f"{item_id}: english-only => {desc[:80]}")
    elif has_code_keyword(desc):
        b_bad.append(f"{item_id}: code-like => {desc[:80]}")
    else:
        b_ok += 1
add("Bulk catalog descriptions", [
    f"total: {len(bulk.get('items', []))}",
    f"ok Chinese-ish: {b_ok}",
    f"issues: {len(b_bad)}",
] + b_bad[:40])

# Java item literals
java_en = []
root = Path("src/main/java/com/xunxian/seekingimmortals/item")
for p in root.rglob("*.java"):
    t = p.read_text(encoding="utf-8")
    if "�" in t:
        java_en.append(f"{p}: contains U+FFFD replacement")
    for i, line in enumerate(t.splitlines(), 1):
        if "Component.literal" not in line:
            continue
        for m in re.finditer(r'Component\.literal\(\s*"((?:\\.|[^"\\])*)"', line):
            try:
                s = json.loads('"' + m.group(1) + '"')
            except Exception:
                s = m.group(1)
            if re.search(r"[A-Za-z]{4,}", s) and not is_cjk(s):
                java_en.append(f'{p.relative_to("src/main/java")}:{i}: "{s}"')
            if is_snake_id(s):
                java_en.append(f'{p.relative_to("src/main/java")}:{i}: id-literal "{s}"')
add("Java item Component.literal English/id", java_en)

# artifacts catalog
arts = json.loads(Path("src/main/resources/data/seeking_immortals/artifacts/artifacts_catalog.json").read_text(encoding="utf-8")).get("artifacts", [])
a_bad = []
for a in arts:
    item_id = a.get("id", "")
    disp = a.get("display", "")
    if not isinstance(disp, str) or not disp.strip():
        a_bad.append(f"{item_id}: empty")
    elif disp == item_id or is_snake_id(disp):
        a_bad.append(f"{item_id}: display is id")
    elif looks_mojibake(disp):
        a_bad.append(f"{item_id}: garbled {disp}")
    elif re.search(r"[A-Za-z]", disp) and not is_cjk(disp):
        a_bad.append(f"{item_id}: english-only {disp}")
add("artifacts_catalog.json display", [
    f"total artifacts: {len(arts)}",
    f"issues: {len(a_bad)}",
] + a_bad[:40])

# sample names
samples = []
for k in [
    "item.seeking_immortals.spirit_stone",
    "item.seeking_immortals.beast_contract",
    "item.seeking_immortals.recipe_qingxin_pill",
    "item.seeking_immortals.flying_sword_low",
    "item.seeking_immortals.immortal_jade",
    "item.seeking_immortals.diyuan_access_token",
    "item.seeking_immortals.recipe_binding_talisman",
    "block.seeking_immortals.leyline_surface_marker",
    "tooltip.seeking_immortals.artifact.header",
    "tooltip.seeking_immortals.catalog_carrier",
]:
    samples.append(f"{k} => {zh.get(k)}")
add("Sample zh values (file truth)", samples)

# missing keys for registered ids
ids = set()
for p in Path("src/main/java").rglob("*.java"):
    t = p.read_text(encoding="utf-8", errors="ignore")
    for m in re.finditer(r'(?:ITEMS|BLOCKS)\.register\(\s*"([a-z0-9_]+)"', t):
        ids.add(m.group(1))
for it in bulk.get("items", []):
    if it.get("id"):
        ids.add(it["id"])
missing = []
for i in sorted(ids):
    if f"item.seeking_immortals.{i}" not in zh and f"block.seeking_immortals.{i}" not in zh:
        missing.append(i)
add("Registered ids missing zh item/block key", [f"count={len(missing)}"] + missing[:50])

# catalog index remaining english displays (not item hover, but relevant)
idx_eng = 0
idx_samples = []
for p in Path("src/main/resources/data/seeking_immortals/catalog").glob("*_index.json"):
    data = json.loads(p.read_text(encoding="utf-8"))
    def walk(o):
        global idx_eng
        if isinstance(o, dict):
            if "id" in o and "display" in o and isinstance(o["display"], str):
                disp = o["display"]
                if re.search(r"[A-Za-z]", disp) and not is_cjk(disp):
                    idx_eng += 1
                    if len(idx_samples) < 15:
                        idx_samples.append(f"{p.name}: {o.get('id')} => {disp}")
            for v in o.values():
                walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)
    walk(data)
add("Catalog index English display leftovers (not item hover names)", [f"count={idx_eng}"] + idx_samples)

out = Path("project_docs/item_display_audit_0.1.494.md")
out.write_text("\n".join(report) + "\n", encoding="utf-8")
print("WROTE", out)
print("raw-id", len(raw_id), "no-cjk", len(no_cjk), "garbled", len(garbled))
print("bulk-bad", len(b_bad), "artifact-bad", len(a_bad), "java-en", len(java_en), "missing-keys", len(missing), "idx-eng", idx_eng)
