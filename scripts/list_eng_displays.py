import json
import re
from pathlib import Path
from collections import Counter

rows = []
for p in Path("src/main/resources/data/seeking_immortals/catalog").glob("*_index.json"):
    data = json.loads(p.read_text(encoding="utf-8"))

    def walk(o):
        if isinstance(o, dict):
            if "id" in o and "display" in o and isinstance(o["display"], str):
                disp = o["display"]
                if re.search(r"[A-Za-z]", disp) and not re.search(r"[一-鿿]", disp):
                    rows.append({"file": p.name, "id": str(o.get("id", "")), "display": disp})
            for v in o.values():
                walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)

    walk(data)

print("total", len(rows))
by = Counter(r["file"] for r in rows)
for f, c in by.most_common():
    print(f"{c:4d} {f}")
print("display==id", sum(1 for r in rows if r["id"] == r["display"]), "/", len(rows))
print("unique ids", len({r["id"] for r in rows}))
for i in sorted({r["id"] for r in rows}):
    print(i)
Path("_tmp_eng_displays.json").write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")
print("wrote _tmp_eng_displays.json")
