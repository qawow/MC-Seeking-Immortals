#!/usr/bin/env python3
"""Generate the bilingual Patchouli quest handbook from authoritative quest data."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DATA_ROOT = ROOT / "src/main/resources/data/seeking_immortals"
TEXT_ROOT = DATA_ROOT / "text_material"
BOOK_ROOT = DATA_ROOT / "patchouli_books/seeking_immortals_guide"

CHAINS_PATH = TEXT_ROOT / "quest_chains.json"
HOOKS_PATH = TEXT_ROOT / "quest_hooks.json"
STORY_PATH = TEXT_ROOT / "main_story_chapters.json"
LINES_PATH = TEXT_ROOT / "quest_lines_full_descriptions_v147.json"
OVERLAY_PATH = TEXT_ROOT / "quest_handbook_i18n_v1.json"

EXPECTED_CHAIN_COUNT = 62
EXPECTED_STAGE_COUNT = 241
EXPECTED_LINE_COUNT = 35
EXPECTED_STORY_COUNT = 7
EXPECTED_FTB_COUNT = 9

LANGUAGES = ("zh_cn", "en_us")
HAN_RE = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")
PROPER_TOKENS = {
    "dajin": "Great Jin",
    "diyuan": "Diyuan",
    "fashi": "Spell-Warrior",
    "fengyuan": "Fengyuan",
    "ftb": "FTB",
    "guiling": "Guiling",
    "hehuan": "Hehuan",
    "huangfeng": "Yellow Maple",
    "kunwu": "Kunwu",
    "mulan": "Mulan",
    "qianzhu": "Thousand Bamboo",
    "qi": "Qi",
    "qixuan": "Qixuan",
    "tianfu": "Heavenly Talisman",
    "tianhu": "Tianhu",
    "tianlan": "Tianlan",
    "tianyuan": "Tianyuan",
    "wanbao": "Wanbao",
    "wutu": "Wutu",
    "xutian": "Void Heaven",
    "yanyue": "Veiled Moon",
    "yinming": "Yinming",
    "yuling": "Spirit Beast",
}


def load_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        value = json.load(handle)
    if not isinstance(value, dict):
        raise ValueError(f"expected a JSON object: {path}")
    return value


def encoded(value: dict[str, Any]) -> bytes:
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def require_text(value: Any, context: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"nonblank text required: {context}")
    return value.strip()


def require_unique_ids(values: list[dict[str, Any]], context: str) -> dict[str, dict[str, Any]]:
    indexed: dict[str, dict[str, Any]] = {}
    for value in values:
        identifier = require_text(value.get("id"), f"{context} id")
        if identifier in indexed:
            raise ValueError(f"duplicate {context} id: {identifier}")
        indexed[identifier] = value
    return indexed


def humanize_hook(hook_id: str) -> str:
    tokens = [token for token in hook_id.split("_") if token and token != "hook"]
    words = [PROPER_TOKENS.get(token, token.capitalize()) for token in tokens]
    value = " ".join(words).strip()
    if not value or "_" in value or HAN_RE.search(value):
        raise ValueError(f"could not create an English stage label for {hook_id}")
    return value


def patchouli_link(target: str, label: str) -> str:
    return f"$(l:{target}){label}$()"


def text_page(text: str, title: str | None = None, anchor: str | None = None) -> dict[str, str]:
    page = {"type": "patchouli:text"}
    if title:
        page["title"] = title
    if anchor:
        page["anchor"] = anchor
    page["text"] = text
    return page


def indexed_pages(
        items: list[tuple[str, str]],
        page_size: int,
        title: str,
        continued_title: str,
) -> list[dict[str, str]]:
    pages: list[dict[str, str]] = []
    chunks = [items[index:index + page_size] for index in range(0, len(items), page_size)]
    for index, chunk in enumerate(chunks):
        page_title = title if len(chunks) == 1 else f"{title} {index + 1}/{len(chunks)}"
        if index > 0 and continued_title:
            page_title = f"{continued_title} {index + 1}/{len(chunks)}"
        pages.append(text_page("$(br)".join(
            patchouli_link(target, label) for target, label in chunk
        ), page_title))
    return pages


def flatten_strings(value: Any) -> list[str]:
    if isinstance(value, str):
        return [value]
    if isinstance(value, list):
        return [text for item in value for text in flatten_strings(item)]
    if isinstance(value, dict):
        return [text for item in value.values() for text in flatten_strings(item)]
    return []


class HandbookData:
    def __init__(self) -> None:
        chains_root = load_json(CHAINS_PATH)
        hooks_root = load_json(HOOKS_PATH)
        story_root = load_json(STORY_PATH)
        lines_root = load_json(LINES_PATH)
        self.overlay = load_json(OVERLAY_PATH)

        self.chains = require_unique_ids(chains_root.get("chains", []), "quest chain")
        hook_records = require_unique_ids(hooks_root.get("hooks", []), "quest hook")
        self.hooks = {
            identifier: value["display"].strip()
            for identifier, value in hook_records.items()
            if isinstance(value.get("display"), str) and value["display"].strip()
        }
        self.story = require_unique_ids(story_root.get("chapters", []), "story chapter")
        self.lines = require_unique_ids(lines_root.get("lines", []), "authored quest line")
        self.ftb = require_unique_ids(self.overlay.get("ftb_chapters", []), "FTB overlay chapter")
        self.story_overlay = require_unique_ids(
            self.overlay.get("story_chapters", []), "story overlay chapter")

        if self.overlay.get("schema_version") != 1:
            raise ValueError("quest handbook overlay schema_version must be 1")
        self._validate_counts()
        self._validate_overlay_coverage()
        self._validate_english_overlay()

    def _validate_counts(self) -> None:
        if len(self.chains) != EXPECTED_CHAIN_COUNT:
            raise ValueError(f"expected {EXPECTED_CHAIN_COUNT} chains, got {len(self.chains)}")
        if len(self.lines) != EXPECTED_LINE_COUNT:
            raise ValueError(f"expected {EXPECTED_LINE_COUNT} authored lines, got {len(self.lines)}")
        if len(self.story) != EXPECTED_STORY_COUNT:
            raise ValueError(f"expected {EXPECTED_STORY_COUNT} story chapters, got {len(self.story)}")
        if len(self.ftb) != EXPECTED_FTB_COUNT:
            raise ValueError(f"expected {EXPECTED_FTB_COUNT} FTB chapters, got {len(self.ftb)}")

        stage_count = sum(self.stage_count(chain) for chain in self.chains.values())
        if stage_count != EXPECTED_STAGE_COUNT:
            raise ValueError(f"expected {EXPECTED_STAGE_COUNT} native stages, got {stage_count}")

    @staticmethod
    def stage_count(chain: dict[str, Any]) -> int:
        stages = chain.get("steps")
        if isinstance(stages, list):
            return len(stages)
        if isinstance(stages, int) and stages > 0:
            return stages
        raise ValueError(f"invalid steps for chain {chain.get('id')}: {stages!r}")

    def _validate_overlay_coverage(self) -> None:
        mapped_chains = [chain_id for chapter in self.ftb.values() for chain_id in chapter.get("chains", [])]
        if len(mapped_chains) != len(set(mapped_chains)):
            raise ValueError("a native chain occurs in more than one FTB overlay chapter")
        if set(mapped_chains) != set(self.chains):
            missing = sorted(set(self.chains) - set(mapped_chains))
            extra = sorted(set(mapped_chains) - set(self.chains))
            raise ValueError(f"FTB chain coverage mismatch; missing={missing}, extra={extra}")

        mapped_lines = [line_id for chapter in self.story_overlay.values() for line_id in chapter.get("lines", [])]
        if len(mapped_lines) != len(set(mapped_lines)):
            raise ValueError("an authored line occurs in more than one story overlay chapter")
        if set(mapped_lines) != set(self.lines):
            missing = sorted(set(self.lines) - set(mapped_lines))
            extra = sorted(set(mapped_lines) - set(self.lines))
            raise ValueError(f"story line coverage mismatch; missing={missing}, extra={extra}")
        if set(self.story_overlay) != set(self.story):
            raise ValueError("story overlay chapter ids do not match main_story_chapters.json")

        chain_titles = self.overlay.get("chain_titles_en", {})
        line_titles = self.overlay.get("line_titles_en", {})
        line_summaries = self.overlay.get("line_summaries_en", {})
        if set(chain_titles) != set(self.chains):
            raise ValueError("chain_titles_en must cover exactly the 62 native chains")
        if set(line_titles) != set(self.lines) or set(line_summaries) != set(self.lines):
            raise ValueError("English line titles and summaries must cover exactly the 35 authored lines")

        missing_hooks: set[str] = set()
        numeric_chains: set[str] = set()
        for chain_id, chain in self.chains.items():
            stages = chain.get("steps")
            if isinstance(stages, int):
                numeric_chains.add(chain_id)
                continue
            for stage in stages:
                hook_id, summary = self._array_stage_parts(chain_id, stage)
                if not summary and hook_id not in self.hooks:
                    missing_hooks.add(hook_id)
        if set(self.overlay.get("hook_labels_zh", {})) != missing_hooks:
            missing = sorted(missing_hooks - set(self.overlay.get("hook_labels_zh", {})))
            extra = sorted(set(self.overlay.get("hook_labels_zh", {})) - missing_hooks)
            raise ValueError(f"Chinese hook overlay mismatch; missing={missing}, extra={extra}")
        if set(self.overlay.get("numeric_stage_labels", {})) != numeric_chains:
            raise ValueError("numeric_stage_labels must cover exactly the numeric-only chains")
        for chain_id in numeric_chains:
            labels = self.overlay["numeric_stage_labels"][chain_id]
            if len(labels) != self.stage_count(self.chains[chain_id]):
                raise ValueError(f"numeric label count mismatch: {chain_id}")

    def _validate_english_overlay(self) -> None:
        english_values: list[str] = []
        english_values.extend(self.overlay["chain_titles_en"].values())
        english_values.extend(self.overlay["line_titles_en"].values())
        english_values.extend(self.overlay["line_summaries_en"].values())
        for chapter in self.ftb.values():
            english_values.append(require_text(chapter.get("title_en"), f"FTB title {chapter['id']}"))
        for chapter in self.story_overlay.values():
            english_values.append(require_text(chapter.get("title_en"), f"story title {chapter['id']}"))
            english_values.append(require_text(chapter.get("summary_en"), f"story summary {chapter['id']}"))
        for labels in self.overlay["numeric_stage_labels"].values():
            english_values.extend(require_text(label.get("en"), "numeric English stage") for label in labels)
        for value in english_values:
            require_text(value, "English overlay value")
            if HAN_RE.search(value):
                raise ValueError(f"English overlay text contains Han characters: {value}")

    @staticmethod
    def _array_stage_parts(chain_id: str, stage: Any) -> tuple[str, str | None]:
        if isinstance(stage, str):
            return require_text(stage, f"stage hook in {chain_id}"), None
        if not isinstance(stage, dict):
            raise ValueError(f"invalid stage in {chain_id}: {stage!r}")
        hook_id = require_text(stage.get("hook"), f"stage hook in {chain_id}")
        summary = stage.get("summary")
        if summary is not None:
            summary = require_text(summary, f"stage summary {chain_id}/{hook_id}")
        return hook_id, summary

    def stage_labels(self, chain_id: str) -> list[dict[str, str]]:
        chain = self.chains[chain_id]
        stages = chain["steps"]
        if isinstance(stages, int):
            return [
                {
                    "zh": require_text(label.get("zh"), f"numeric Chinese stage {chain_id}"),
                    "en": require_text(label.get("en"), f"numeric English stage {chain_id}"),
                }
                for label in self.overlay["numeric_stage_labels"][chain_id]
            ]

        labels: list[dict[str, str]] = []
        for stage in stages:
            hook_id, summary = self._array_stage_parts(chain_id, stage)
            zh_label = summary or self.hooks.get(hook_id) or self.overlay["hook_labels_zh"].get(hook_id)
            labels.append({
                "zh": require_text(zh_label, f"Chinese stage label {chain_id}/{hook_id}"),
                "en": humanize_hook(hook_id),
            })
        return labels


def category(language: str) -> dict[str, Any]:
    if language == "zh_cn":
        name = "任务与剧情"
        description = "主线章节、原生任务线阶段与 FTB 投影索引。"
    else:
        name = "Quests and Story"
        description = "Story chapters, native quest stages, and FTB projection indexes."
    return {
        "name": name,
        "description": description,
        "icon": "seeking_immortals:mortal_quest_token",
        "sortnum": 5,
    }


def guide_entry(data: HandbookData, language: str) -> dict[str, Any]:
    is_zh = language == "zh_cn"
    story_links = [
        (chapter["entry"], (
            f"第{index}章 · {data.story[chapter_id]['display']}"
            if is_zh else f"Chapter {index}: {chapter['title_en']}"
        ))
        for index, (chapter_id, chapter) in enumerate(data.story_overlay.items())
    ]
    native_links = [
        (chapter["entry"], chapter["title_zh"] if is_zh else chapter["title_en"])
        for chapter in data.ftb.values()
    ]

    if is_zh:
        pages = [text_page(
            "模组原生任务进度是唯一权威账本。服务器校验接取、当前阶段、上交物、分支与奖励；"
            "Patchouli、追踪界面、命令和 FTB Quests 只提供索引、展示或受校验的行动入口。",
            "任务权威与导航",
        )]
        pages.extend(indexed_pages(story_links, 4, "七章剧情索引", "剧情索引"))
        pages.extend(indexed_pages(native_links, 5, "九卷原生任务", "原生任务"))
        pages.append(text_page(
            "每条原生任务线均按服务器阶段顺序列出。安装 FTB Quests 后，同名章节投影这六十二条任务线；"
            "镜像节点不会另存进度或重复发奖，允许回写的节点也只能请求推进当前下一阶段。",
            "FTB 投影边界",
        ))
        pages.append(text_page(
            "若进度未变化，先检查当前阶段、人物、区域、境界、分支与上交代价。重新登录会从服务器账本同步；"
            "未安装 FTB Quests 时，原生任务、NPC、追踪与奖励仍照常运行。",
            "排查进度",
        ))
        name = "任务系统指南"
    else:
        pages = [text_page(
            "Native quest progress is the sole authoritative ledger. The server validates acceptance, the current stage, "
            "turn-ins, branches, and rewards; Patchouli, the tracker, commands, and FTB Quests provide indexes, displays, "
            "or validated action requests.",
            "Authority and Navigation",
        )]
        pages.extend(indexed_pages(story_links, 4, "Seven-Chapter Story", "Story Index"))
        pages.extend(indexed_pages(native_links, 5, "Nine Native Volumes", "Native Volumes"))
        pages.append(text_page(
            "Every native quest line is listed in server stage order. With FTB Quests installed, matching chapters project "
            "these sixty-two lines. Mirror nodes neither store separate progress nor duplicate rewards, and write-back nodes "
            "may request only the currently valid next stage.",
            "FTB Projection Boundary",
        ))
        pages.append(text_page(
            "When progress does not change, check the current stage, character, region, realm, branch, and turn-in cost. "
            "Logging in again synchronizes from the server ledger. Native quests, NPCs, tracking, and rewards still work "
            "when FTB Quests is absent.",
            "Troubleshooting",
        ))
        name = "Quest System Guide"

    return {
        "name": name,
        "category": "seeking_immortals:quests",
        "icon": "seeking_immortals:mortal_quest_token",
        "sortnum": 0,
        "pages": pages,
    }


def story_entry(data: HandbookData, chapter_id: str, index: int, language: str) -> dict[str, Any]:
    is_zh = language == "zh_cn"
    chapter = data.story[chapter_id]
    overlay = data.story_overlay[chapter_id]
    line_ids = overlay["lines"]
    name = f"第{index}章 · {chapter['display']}" if is_zh else f"Chapter {index}: {overlay['title_en']}"
    overview = (
        require_text(chapter.get("setting", {}).get("lore"), f"story lore {chapter_id}")
        + f"$(br2)本章收录 {len(line_ids)} 条已撰写剧情线；索引只说明路线，不替代服务器进度。"
        if is_zh else
        overlay["summary_en"]
        + f"$(br2)This chapter contains {len(line_ids)} authored quest lines. The index describes routes and does not replace server progress."
    )
    items = [
        (
            f"{overlay['entry']}#line_{line_id}",
            data.lines[line_id]["title"] if is_zh else data.overlay["line_titles_en"][line_id],
        )
        for line_id in line_ids
    ]
    pages = [text_page(overview, "章节概览" if is_zh else "Chapter Overview")]
    pages.extend(indexed_pages(
        items,
        5,
        "剧情线索引" if is_zh else "Quest Line Index",
        "剧情线索引" if is_zh else "Quest Line Index",
    ))
    for line_id in line_ids:
        line = data.lines[line_id]
        pages.append(text_page(
            require_text(line.get("tagline"), f"line tagline {line_id}")
            if is_zh else data.overlay["line_summaries_en"][line_id],
            require_text(line.get("title"), f"line title {line_id}")
            if is_zh else data.overlay["line_titles_en"][line_id],
            f"line_{line_id}",
        ))
    return {
        "name": name,
        "category": "seeking_immortals:quests",
        "icon": "minecraft:writable_book",
        "sortnum": 10 + index,
        "pages": pages,
    }


def numbered_stage_text(labels: list[str], start: int) -> str:
    return "$(br)".join(f"{index}. {label}" for index, label in enumerate(labels, start=start))


def native_entry(data: HandbookData, chapter_id: str, index: int, language: str) -> dict[str, Any]:
    is_zh = language == "zh_cn"
    chapter = data.ftb[chapter_id]
    chain_ids = chapter["chains"]
    title_key = "title_zh" if is_zh else "title_en"
    title = chapter[title_key]
    pages = [text_page(
        (
            f"本卷对应 FTB Quests 章节“{title}”，收录 {len(chain_ids)} 条原生任务线。"
            "阶段顺序来自原生任务资料，完成与奖励仍以服务器账本为准。"
            if is_zh else
            f"This volume matches the FTB Quests chapter '{title}' and contains {len(chain_ids)} native quest lines. "
            "Stage order comes from native quest data; completion and rewards remain server-authoritative."
        ),
        "投影说明" if is_zh else "Projection Notes",
    )]
    chain_links = [
        (
            f"{chapter['entry']}#chain_{chain_id}",
            data.chains[chain_id]["display"] if is_zh else data.overlay["chain_titles_en"][chain_id],
        )
        for chain_id in chain_ids
    ]
    pages.extend(indexed_pages(
        chain_links,
        4,
        "任务线索引" if is_zh else "Quest Line Index",
        "任务线索引" if is_zh else "Quest Line Index",
    ))

    for chain_id in chain_ids:
        chain_title = (
            data.chains[chain_id]["display"] if is_zh else data.overlay["chain_titles_en"][chain_id]
        )
        labels = [stage["zh" if is_zh else "en"] for stage in data.stage_labels(chain_id)]
        anchor = f"chain_{chain_id}"
        if len(labels) <= 4:
            pages.append(text_page(numbered_stage_text(labels, 1), chain_title, anchor))
        else:
            pages.append(text_page(numbered_stage_text(labels[:3], 1), f"{chain_title} 1/2", anchor))
            pages.append(text_page(numbered_stage_text(labels[3:], 4), f"{chain_title} 2/2"))

    return {
        "name": title,
        "category": "seeking_immortals:quests",
        "icon": "seeking_immortals:mortal_quest_token",
        "sortnum": 30 + index,
        "pages": pages,
    }


def expected_outputs(data: HandbookData) -> tuple[dict[Path, bytes], set[str]]:
    outputs: dict[Path, bytes] = {}
    generated_stems = {"quest_system_guide"}
    for language in LANGUAGES:
        language_root = BOOK_ROOT / language
        outputs[language_root / "categories/quests.json"] = encoded(category(language))
        outputs[language_root / "entries/quest_system_guide.json"] = encoded(guide_entry(data, language))

        for index, chapter_id in enumerate(data.story_overlay):
            entry = data.story_overlay[chapter_id]["entry"]
            generated_stems.add(entry)
            outputs[language_root / f"entries/{entry}.json"] = encoded(
                story_entry(data, chapter_id, index, language))

        for index, chapter_id in enumerate(data.ftb):
            entry = data.ftb[chapter_id]["entry"]
            generated_stems.add(entry)
            outputs[language_root / f"entries/{entry}.json"] = encoded(
                native_entry(data, chapter_id, index, language))

    return outputs, generated_stems


def unexpected_generated_files(expected_stems: set[str]) -> list[Path]:
    unexpected: list[Path] = []
    for language in LANGUAGES:
        entries_root = BOOK_ROOT / language / "entries"
        for path in entries_root.glob("quest_*.json"):
            if path.stem not in expected_stems:
                unexpected.append(path)
    return sorted(unexpected)


def check_outputs(outputs: dict[Path, bytes], generated_stems: set[str]) -> int:
    failures: list[str] = []
    for path, expected in outputs.items():
        if not path.is_file():
            failures.append(f"missing: {path.relative_to(ROOT)}")
        elif path.read_bytes() != expected:
            failures.append(f"stale: {path.relative_to(ROOT)}")
    for path in unexpected_generated_files(generated_stems):
        failures.append(f"unexpected generated entry: {path.relative_to(ROOT)}")
    if failures:
        print("Quest handbook generation check failed:", file=sys.stderr)
        for failure in failures:
            print(f"  - {failure}", file=sys.stderr)
        return 1
    print(f"Quest handbook is current: {len(generated_stems)} bilingual entries.")
    return 0


def write_outputs(outputs: dict[Path, bytes], generated_stems: set[str]) -> int:
    unexpected = unexpected_generated_files(generated_stems)
    if unexpected:
        print("Refusing to overwrite with unexpected generated quest entries present:", file=sys.stderr)
        for path in unexpected:
            print(f"  - {path.relative_to(ROOT)}", file=sys.stderr)
        return 1

    changed: list[Path] = []
    for path, content in outputs.items():
        if path.is_file() and path.read_bytes() == content:
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)
        changed.append(path)
    print(f"Generated {len(generated_stems)} bilingual quest entries; updated {len(changed)} files.")
    for path in changed:
        print(f"  {path.relative_to(ROOT)}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if generated resources are missing or stale")
    args = parser.parse_args()
    try:
        data = HandbookData()
        outputs, generated_stems = expected_outputs(data)
        if args.check:
            return check_outputs(outputs, generated_stems)
        return write_outputs(outputs, generated_stems)
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"Quest handbook generation failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
