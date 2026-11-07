#!/usr/bin/env python3
"""Apply GAME BOX branding and Hebrew localization to the pinned Puzzle Suite source."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

root = Path(sys.argv[1]).resolve()

def replace_once(path: str, old: str, new: str) -> None:
    file = root / path
    source = file.read_text(encoding="utf-8")
    if old not in source:
        raise SystemExit(f"Expected source text was not found in {path}: {old!r}")
    file.write_text(source.replace(old, new, 1), encoding="utf-8")

# Use the GAME BOX name and open with a dark green theme.
replace_once(
    "lib/main.dart",
    "title: 'Puzzle Games',",
    "title: 'GAME BOX',",
)
replace_once(
    "lib/providers/theme_provider.dart",
    "    return AppThemeMode.system;",
    "    return AppThemeMode.dark;",
)
design = root / "lib/utils/design_system.dart"
design_text = design.read_text(encoding="utf-8")
colors = {
    "static const Color primary = Color(0xFF6366F1);":
        "static const Color primary = Color(0xFF1F8A55);",
    "static const Color primaryShadow = Color(0xFF4F46E5);":
        "static const Color primaryShadow = Color(0xFF14532D);",
    "static const Color darkBackground = Color(0xFF121212);":
        "static const Color darkBackground = Color(0xFF07170F);",
    "static const Color darkSurface = Color(0xFF1E1E1E);":
        "static const Color darkSurface = Color(0xFF102A1C);",
    "static const Color darkOutline = Color(0xFF2D2D2D);":
        "static const Color darkOutline = Color(0xFF244432);",
    "static const Color darkOutlineVariant = Color(0xFF3A3A3A);":
        "static const Color darkOutlineVariant = Color(0xFF315840);",
}
for old, new in colors.items():
    if old not in design_text:
        raise SystemExit(f"Expected theme color not found: {old}")
    design_text = design_text.replace(old, new, 1)
design.write_text(design_text, encoding="utf-8")

shell = root / "lib/ui/features/main_shell/main_shell.dart"
shell_text = shell.read_text(encoding="utf-8")
for old, new in {
    "label: 'STATS'": "label: 'נתונים'",
    "label: 'GAMES'": "label: 'משחקים'",
    "label: 'SETTINGS'": "label: 'הגדרות'",
}.items():
    shell_text = shell_text.replace(old, new)
shell.write_text(shell_text, encoding="utf-8")

web_index = root / "web/index.html"
web_text = web_index.read_text(encoding="utf-8")
web_text = re.sub(r"<title>.*?</title>", "<title>GAME BOX</title>", web_text, count=1, flags=re.S)
web_index.write_text(web_text, encoding="utf-8")

# Keep all original localization keys/placeholders so generated Dart code stays complete.
en_path = root / "lib/l10n/app_en.arb"
he_path = root / "lib/l10n/app_he.arb"
arb = json.loads(en_path.read_text(encoding="utf-8"))
arb["@@locale"] = "he"
hebrew = {
    "appTitle": "GAME BOX",
    "home": "בית",
    "settings": "הגדרות",
    "playAgain": "שחק שוב",
    "playNext": "המשחק הבא",
    "seeCompleted": "הצג משחקים שהושלמו",
    "finish": "סיום",
    "congrats": "כל הכבוד",
    "wellDone": "יפה מאוד",
    "timeUp": "הזמן נגמר",
    "victory": "ניצחון",
    "completed": "הושלם",
    "score": "ניקוד",
    "timeLeft": "זמן",
    "trials": "ניסיונות",
    "moves": "מהלכים",
    "keepScreenAwake": "השאר מסך דולק",
    "keepScreenAwakeDescription": "מנע כיבוי המסך בזמן המשחק",
    "appearance": "מראה",
    "language": "שפה",
    "gameplay": "הגדרות משחק",
    "supportUs": "תמיכה",
    "systemLegal": "מידע ורישיונות",
    "games": "משחקים",
    "solvedToday": "נפתרו היום",
    "searchGames": "חיפוש משחקים...",
    "readyToStart": "מוכנים לאתגר?",
    "greatStart": "התחלה מצוינת!",
    "incredible": "מדהים! אתה אלוף הפאזלים.",
    "noGamesMatch": "לא נמצאו משחקים מתאימים",
    "categoryAll": "הכול",
    "categoryAttention": "ריכוז",
    "categoryLogic": "היגיון",
    "categoryMath": "חשבון",
    "categoryWord": "מילים",
    "categoryMemory": "זיכרון",
    "categorySpatial": "מרחב",
    "themeLight": "בהיר",
    "themeDark": "כהה",
    "themeSystem": "לפי המכשיר",
    "starOnGithub": "דרג ב־GitHub",
    "privacyPolicy": "מדיניות פרטיות",
    "termsOfService": "תנאי שימוש",
    "licenses": "רישיונות",
    "howToPlay": "איך משחקים",
    "gotIt": "הבנתי",
    "sudokuTitle": "סודוקו",
    "sudokuSubtitle": "השלם את הלוח כך שבכל שורה, עמודה וריבוע יופיעו הספרות 1 עד 9 ללא חזרות.",
    "minesweeperTitle": "שולה מוקשים",
    "minesweeperSubtitle": "גלה את כל המשבצות הבטוחות בלי להפעיל מוקש.",
    "game2048Title": "2048",
    "game2048Subtitle": "החלק אריחים לכל כיוון, אחד מספרים שווים והגע ל־2048.",
    "nonogramTitle": "נונוגרם",
    "nonogramSubtitle": "גלה תמונה נסתרת בעזרת רמזים לוגיים.",
    "akariTitle": "אקארי – אורות",
    "akariSubtitle": "האר את כל המשבצות הלבנות בלוח.",
    "crosswordTitle": "תשבץ",
    "wordSearchTitle": "תפזורת",
    "lightsOutTitle": "כיבוי אורות",
    "memoryMatrixTitle": "מטריצת זיכרון",
    "findWordTitle": "מצא את המילה",
    "wordSearchSubtitle": "מצא את כל המילים המוסתרות בלוח.",
    "crosswordSubtitle": "השלם את הלוח בעזרת הרמזים.",
    "blockEscapeTitle": "הזזת קוביות",
    "blockEscapeSubtitle": "הזז את הקוביות כדי לפנות דרך ליציאה.",
    "bridgesTitle": "גשרים",
    "bridgesSubtitle": "חבר את האיים במספר הגשרים הנכון.",
    "calculationSprintTitle": "מרוץ חשבון",
    "calculationSprintSubtitle": "פתור כמה שיותר תרגילים בזמן מוגבל.",
    "colorFloodTitle": "הצפת צבע",
    "colorMatchTitle": "התאמת צבע",
    "chimpTestTitle": "מבחן השימפנזה",
    "simonSequenceTitle": "רצף סיימון",
    "schulteTableTitle": "טבלת שולטה",
    "nonogramWin": "התמונה נחשפה!",
    "minesweeperWin": "ניצחון!",
    "minesweeperMines": "מוקשים",
    "minesweeperStatus": "מצב",
    "minesweeperWon": "ניצחת",
    "minesweeperBoom": "בום",
    "minesweeperPlaying": "משחק",
    "minesweeperReveal": "חשוף",
    "minesweeperFlag": "סמן",
}
arb.update(hebrew)
he_path.write_text(json.dumps(arb, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# Supply the upstream GPL notice in the app's own licenses/about files.
