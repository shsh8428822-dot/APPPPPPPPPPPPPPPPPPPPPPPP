#!/usr/bin/env python3
"""Assemble two MIT HTML5 game collections into one offline GAME BOX library."""
from __future__ import annotations
import json
import os
import re
import shutil
import sys
import urllib.request
from pathlib import Path

sausi_root = Path(sys.argv[1]).resolve()
mash_root = Path(sys.argv[2]).resolve()
dest = Path(sys.argv[3]).resolve()
dest.mkdir(parents=True, exist_ok=True)

def copy_file(src: Path, target: Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, target)

licenses = dest / "licenses"
licenses.mkdir(exist_ok=True)
copy_file(sausi_root / "LICENSE", licenses / "SAUSI-MIT.txt")
copy_file(mash_root / "LICENSE", licenses / "MASHUKUI-MIT.txt")
copy_file(mash_root / "i18n.js", dest / "indie" / "i18n.js")

category_map = {
    "puzzle": "חשיבה", "board": "אסטרטגיה", "arcade": "ארקייד",
    "shooter": "ארקייד", "platformer": "הרפתקאות", "sports": "ספורט",
    "casual": "ארקייד", "racing": "מירוצים", "3d": "הרפתקאות",
    "word-quiz": "מילים",
}
indie_category = {
    "air-hockey": "ספורט", "bowling": "ספורט", "hoop-shot": "ספורט",
    "mini-golf": "ספורט", "paddle-duel": "ספורט", "penalty-kick": "ספורט",
    "battleship": "אסטרטגיה", "checkers": "אסטרטגיה", "connect-four": "אסטרטגיה",
    "gomoku": "אסטרטגיה", "mancala": "אסטרטגיה", "reversi": "אסטרטגיה",
    "tic-tac-toe": "אסטרטגיה", "snakes-ladders": "אסטרטגיה",
    "memory-match": "זיכרון", "simon-says": "זיכרון",
    "word-search": "מילים", "wordle": "מילים",
    "2048": "חשיבה", "hanoi": "חשיבה", "lights-out": "חשיבה",
    "mahjong-link": "חשיבה", "minesweeper": "חשיבה", "nonogram": "חשיבה",
    "pipe-connect": "חשיבה", "rush-hour": "חשיבה", "slide-puzzle": "חשיבה",
    "sokoban": "חשיבה", "sudoku": "חשיבה", "water-sort": "חשיבה",
    "tetris": "חשיבה", "block-drop": "חשיבה",
    "cross-road": "מירוצים", "dino-run": "מירוצים",
}
title_he = {
    "2048": "2048", "sudoku": "סודוקו", "minesweeper": "שולה מוקשים",
    "snake": "נחש", "tic-tac-toe": "איקס עיגול", "connect-four": "ארבע בשורה",
    "checkers": "דמקה", "chess": "שחמט", "memory": "זיכרון",
    "memory-match": "התאמת זוגות", "lights-out": "כיבוי אורות",
    "nonogram": "נונוגרם", "wordle": "אתגר מילים", "tetris": "טטריס",
    "bowling": "באולינג", "air-hockey": "הוקי אוויר",
    "paddle-duel": "דו־קרב מטקות", "penalty-kick": "בעיטות עונשין",
    "hanoi": "מגדלי האנוי",
}
games = []
cdn_cache: dict[str, Path] = {}
cdn_pattern = re.compile(
    r"https://cdn\.jsdelivr\.net/npm/(phaser|three)@([^/]+)/([^\"'\s]+)"
    r"|https://cdnjs\.cloudflare\.com/ajax/libs/three\.js/([^/]+)/three\.min\.js"
    r"|https://unpkg\.com/three@([^/]+)/build/three\.min\.js"
)
font_link_pattern = re.compile(
    r"<link\b[^>]*href=[\"']https?://fonts\.googleapis\.com[^\"']*[\"'][^>]*>",
    flags=re.IGNORECASE,
)
font_import_pattern = re.compile(
    r"@import\s+(?:url\()?['\"]?https?://fonts\.googleapis\.com[^;]*;",
    flags=re.IGNORECASE,
)

def vendor_cdn(match: re.Match[str], file_path: Path) -> str:
    if match.group(1):
        library, version, asset_path = match.group(1), match.group(2), match.group(3)
    elif match.group(4):
        library, version, asset_path = "three", match.group(4), "three.min.js"
    else:
        library, version, asset_path = "three", match.group(5), "three.min.js"
    asset_path = asset_path.split("?", 1)[0]
    suffix = "module" if "module" in asset_path else "min"
    key = f"{library}-{version}-{suffix}"
    if key not in cdn_cache:
        lib_path = dest / "libs" / f"{key}.js"
        lib_path.parent.mkdir(parents=True, exist_ok=True)
        url = match.group(0).rstrip(");,")
        request = urllib.request.Request(url, headers={"User-Agent": "GAME-BOX-offline-build"})
        with urllib.request.urlopen(request, timeout=120) as response:
            lib_path.write_bytes(response.read())
        cdn_cache[key] = lib_path
    return os.path.relpath(cdn_cache[key], start=file_path.parent).replace(os.sep, "/")

def patch_tree(folder: Path) -> list[str]:
    external: list[str] = []
    for file_path in folder.rglob("*"):
        if not file_path.is_file() or file_path.suffix.lower() not in {
            ".html", ".js", ".mjs", ".css", ".json"
        }:
            continue
        try:
            content = file_path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        content = cdn_pattern.sub(lambda m: vendor_cdn(m, file_path), content)
        content = font_link_pattern.sub("", content)
        content = font_import_pattern.sub("", content)
        for url in re.findall(r"https?://[^\s\"'<>]+", content):
            if "fonts.googleapis.com" not in url and "fonts.gstatic.com" not in url:
                external.append(url.split("?", 1)[0])
        file_path.write_text(content, encoding="utf-8")
    return sorted(set(external))

# Collection 1: all 142 indexed games from sausi-7/games, MIT.
registry = json.loads((sausi_root / "games" / "registry.json").read_text(encoding="utf-8"))
source_games = registry.get("games", [])
if len(source_games) != 142:
    raise SystemExit(f"Expected 142 entries in the pinned MIT registry; found {len(source_games)}")
external_report = {}
sausi_count = 0
for game in source_games:
    source_html = sausi_root / game["path"]
    if not source_html.is_file():
        # One upstream entry spells its HTML file "inex.html" instead of "index.html".
        alternate_html = source_html.with_name("inex.html")
        if alternate_html.is_file():
            source_html = alternate_html
        else:
            raise SystemExit(f"Game entry is missing: {game['path']}")
    source_dir = source_html.parent
    relative_game_dir = source_dir.relative_to(sausi_root / "games")
    target_dir = dest / "sausi" / relative_game_dir
    target_dir.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(source_dir, target_dir, dirs_exist_ok=True)
    external = patch_tree(target_dir)
    if external:
        external_report[f"sausi/{relative_game_dir.as_posix()}"] = external
    rel_index = (Path("sausi") / relative_game_dir / source_html.name).as_posix()
    slug = game.get("slug") or source_dir.name
    games.append({
        "id": f"sausi-{slug}",
        "title": title_he.get(slug, game.get("name", slug)),
        "originalTitle": game.get("name", slug),
        "description": game.get("description", "משחק אופליין"),
        "category": category_map.get(game.get("category", ""), "ארקייד"),
        "path": rel_index,
        "source": "sausi-7/games",
    })
    sausi_count += 1

# Collection 2: 48 compact game folders from mashukui/web-games, MIT.
indie_count = 0
for source_dir in sorted(mash_root.iterdir()):
    if not source_dir.is_dir() or source_dir.name.startswith("."):
        continue
    source_html = source_dir / "index.html"
    if not source_html.is_file():
        continue
    target_dir = dest / "indie" / source_dir.name
    shutil.copytree(source_dir, target_dir, dirs_exist_ok=True)
    external = patch_tree(target_dir)
    if external:
        external_report[f"indie/{source_dir.name}"] = external
    slug = source_dir.name
    games.append({
        "id": f"indie-{slug}",
        "title": title_he.get(slug, slug.replace("-", " ").title()),
        "originalTitle": title_he.get(slug, slug.replace("-", " ").title()),
        "description": "משחק HTML5 קליל שעובד ללא חיבור לרשת.",
        "category": indie_category.get(slug, "ארקייד"),
        "path": f"indie/{slug}/index.html",
        "source": "mashukui/web-games",
    })
    indie_count += 1

if indie_count != 48:
    raise SystemExit(f"Expected 48 single-file indie games; found {indie_count}")

(dest / "catalog.json").write_text(
    json.dumps(games, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
)

launcher = r'''<!doctype html>
<html lang="he" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<meta name="theme-color" content="#07170f">
<title>GAME BOX – משחקים נוספים</title>
<style>
*{box-sizing:border-box}body{margin:0;background:#07170f;color:#e8f5ec;font-family:Arial,sans-serif}
header{padding:18px 16px 10px;background:#0e281a;position:sticky;top:0;z-index:2;border-bottom:1px solid #244432}
h1{font-size:23px;margin:0 0 6px;color:#fff}p{margin:0 0 12px;color:#b9d5c2;font-size:13px}
input{width:100%;padding:13px 15px;border-radius:14px;border:1px solid #315840;background:#07170f;color:#fff;font-size:16px;outline:none}
#filters{display:flex;gap:7px;overflow-x:auto;padding:11px 0 2px;scrollbar-width:none}
.filter{white-space:nowrap;border:1px solid #315840;background:#102a1c;color:#c9e4d1;border-radius:999px;padding:8px 13px;font-size:13px}
.filter.active{background:#1f8a55;border-color:#1f8a55;color:white}
#count{font-size:12px;color:#a6c5b0;margin-top:9px}
#grid{padding:14px;display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}
.card{min-width:0;text-align:right;background:#102a1c;border:1px solid #244432;border-radius:16px;padding:14px;color:#fff;min-height:105px}
.card b{display:block;font-size:15px;margin-bottom:8px;overflow-wrap:anywhere}
.card small{display:block;color:#b9d5c2;line-height:1.35;font-size:12px}
.card em{display:inline-block;font-style:normal;color:#8ed0a6;font-size:11px;margin-top:8px}
#empty{padding:30px;text-align:center;color:#b9d5c2}
#player{display:none;position:fixed;inset:0;background:#07170f;z-index:5}
#playerbar{height:48px;display:flex;align-items:center;justify-content:space-between;padding:5px 10px;background:#0e281a}
#playerbar button{border:0;border-radius:10px;background:#1f8a55;color:#fff;padding:9px 14px;font-size:14px}
#game{width:100%;height:calc(100% - 48px);border:0;background:#fff;display:block}
@media(min-width:700px){#grid{grid-template-columns:repeat(4,minmax(0,1fr))}}
</style>
</head>
<body>
<header>
  <h1>🎲 משחקים נוספים</h1>
  <p>אוסף משחקי HTML5 שאפשר לשחק בהם גם בלי חיבור לאינטרנט</p>
  <input id="search" type="search" placeholder="חיפוש משחק..." autocomplete="off">
  <div id="filters"></div>
  <div id="count"></div>
</header>
<main id="grid"></main>
<div id="empty" hidden>לא נמצאו משחקים מתאימים.</div>
<div id="player">
  <div id="playerbar"><button id="back">חזרה לרשימת המשחקים</button><strong id="gameTitle"></strong></div>
  <iframe id="game" allow="autoplay; fullscreen; gamepad; clipboard-read; clipboard-write"></iframe>
</div>
<script>
const categories=["הכול","חשיבה","אסטרטגיה","ארקייד","מירוצים","קלפים","ספורט","הרפתקאות","זיכרון","מילים"];
let data=[],selected="הכול";
const grid=document.getElementById("grid"),filters=document.getElementById("filters");
const search=document.getElementById("search"),count=document.getElementById("count");
const player=document.getElementById("player"),frame=document.getElementById("game");
function renderFilters(){
 filters.replaceChildren();
 categories.forEach(c=>{
  const b=document.createElement("button");b.className="filter"+(selected===c?" active":"");b.textContent=c;
  b.onclick=()=>{selected=c;renderFilters();render();};filters.appendChild(b);
 });
}
function render(){
 const q=search.value.trim().toLocaleLowerCase();
 const filtered=data.filter(g=>(selected==="הכול"||g.category===selected)&&
  (g.title+" "+g.originalTitle+" "+g.description).toLocaleLowerCase().includes(q));
 grid.replaceChildren();
 filtered.forEach(g=>{
  const card=document.createElement("button");card.className="card";
  const title=document.createElement("b");title.textContent=g.title;
  const desc=document.createElement("small");desc.textContent=g.description||"משחק אופליין";
  const tag=document.createElement("em");tag.textContent=g.category;
  card.append(title,desc,tag);card.onclick=()=>openGame(g);grid.appendChild(card);
 });
 document.getElementById("empty").hidden=filtered.length>0;
 count.textContent=filtered.length+" משחקים מוצגים מתוך "+data.length;
}
function openGame(g){
 document.getElementById("gameTitle").textContent=g.title;
 frame.src=new URL(g.path,location.href).href;
 player.style.display="block";
}
document.getElementById("back").onclick=()=>{
 frame.src="about:blank";player.style.display="none";
};
search.addEventListener("input",render);
fetch("./catalog.json").then(r=>r.json()).then(g=>{data=g;renderFilters();render();})
.catch(()=>{count.textContent="לא ניתן לטעון את רשימת המשחקים המקומית.";});
</script>
</body></html>
'''
(dest / "index.html").write_text(launcher, encoding="utf-8")
(dest / "indie" / "index.html").write_text(
    '<!doctype html><meta charset="utf-8"><meta http-equiv="refresh" content="0;url=../index.html"><title>GAME BOX</title>',
    encoding="utf-8",
)
(dest / "external-resource-audit.json").write_text(
    json.dumps(external_report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
)
print(json.dumps({
    "sausi_games": sausi_count,
    "indie_games": indie_count,
    "total_html5_games": len(games),
    "vendored_libraries": len(cdn_cache),
    "audited_games_with_external_references": len(external_report),
}, ensure_ascii=False))
