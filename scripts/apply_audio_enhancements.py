#!/usr/bin/env python3
from __future__ import annotations

import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
SRC = APP / "src" / "main" / "java"
PLAYER_DIR = SRC / "com" / "playtorrio" / "tv" / "ui" / "screens" / "player"
PAYLOAD = ROOT / "enhancements" / "src"
REPORT = ROOT / "enhanced-patch-report.txt"
NEW_APP_ID = "com.playtorrio.tv.enhanced"

log_lines: list[str] = []


def log(message: str) -> None:
    print(message)
    log_lines.append(message)


def fail(message: str) -> None:
    log(f"ERROR: {message}")
    REPORT.write_text("\n".join(log_lines) + "\n", encoding="utf-8")
    raise SystemExit(2)


def write_if_changed(path: Path, text: str) -> bool:
    old = path.read_text(encoding="utf-8")
    if old == text:
        return False
    path.write_text(text, encoding="utf-8")
    return True


def copy_payload() -> None:
    if not PAYLOAD.exists():
        fail(f"Enhancement payload not found: {PAYLOAD}")
    copied = 0
    for source in PAYLOAD.rglob("*.kt"):
        rel = source.relative_to(PAYLOAD)
        target = SRC / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)
        copied += 1
        log(f"ADD  {target.relative_to(ROOT)}")
    if copied < 7:
        fail(f"Expected at least 7 Kotlin enhancement files, copied only {copied}")


def patch_application_id() -> None:
    gradle = APP / "build.gradle.kts"
    if not gradle.exists():
        fail("app/build.gradle.kts not found")
    text = gradle.read_text(encoding="utf-8")
    original = text

    # This is the Android package identity. Namespace/package declarations intentionally stay original.
    text, count = re.subn(
        r'(\bapplicationId\s*=\s*")[^"]+(\")',
        rf'\g<1>{NEW_APP_ID}\g<2>',
        text,
        count=1,
    )
    if count != 1:
        fail("Could not find exactly one applicationId in app/build.gradle.kts")

    # Make the installed build easy to identify in Android settings/logs.
    text = re.sub(
        r'(\bversionName\s*=\s*")([^"]+)(\")',
        lambda m: m.group(1) + (m.group(2) if m.group(2).endswith("-enhanced") else m.group(2) + "-enhanced") + m.group(3),
        text,
        count=1,
    )

    if text != original:
        gradle.write_text(text, encoding="utf-8")
        log(f"EDIT {gradle.relative_to(ROOT)}: applicationId -> {NEW_APP_ID}")

    strings = APP / "src" / "main" / "res" / "values" / "strings.xml"
    if strings.exists():
        s = strings.read_text(encoding="utf-8")
        s2, n = re.subn(
            r'(<string\s+name=["\']app_name["\'][^>]*>).*?(</string>)',
            r'\1PlayTorrio Enhanced\2',
            s,
            count=1,
            flags=re.S,
        )
        if n:
            strings.write_text(s2, encoding="utf-8")
            log(f"EDIT {strings.relative_to(ROOT)}: app_name -> PlayTorrio Enhanced")

    manifest = APP / "src" / "main" / "AndroidManifest.xml"
    if manifest.exists():
        m = manifest.read_text(encoding="utf-8")
        before = m
        # Provider authorities must be unique across simultaneously installed apps.
        m = re.sub(
            r'(android:authorities\s*=\s*")com\.playtorrio\.tv([^"}]*)"',
            rf'\1{NEW_APP_ID}\2"',
            m,
        )
        # A custom app-level permission with the old package prefix can also collide.
        m = re.sub(
            r'(<(?:permission|uses-permission)\b[^>]*android:name\s*=\s*")com\.playtorrio\.tv(\.[^"]+")',
            rf'\1{NEW_APP_ID}\2',
            m,
        )
        if m != before:
            manifest.write_text(m, encoding="utf-8")
            log(f"EDIT {manifest.relative_to(ROOT)}: package-scoped authorities/permissions -> enhanced id")


def player_kotlin_files() -> list[Path]:
    if not PLAYER_DIR.exists():
        fail(f"Player source directory not found: {PLAYER_DIR.relative_to(ROOT)}")
    return [
        p for p in PLAYER_DIR.rglob("*.kt")
        if p.name not in {"EnhancedRenderersFactory.kt", "AudioEnhancementControls.kt"}
        and "audio" not in p.parts[len(PLAYER_DIR.parts):]
    ]


def patch_player_audio_sink() -> None:
    files = player_kotlin_files()

    # Preferred path: keep all of PlayTorrio's existing renderer configuration and simply swap
    # the factory class for our subclass. Chained settings remain intact.
    for path in files:
        text = path.read_text(encoding="utf-8")
        if "DefaultRenderersFactory" not in text:
            continue
        if not re.search(r'\bDefaultRenderersFactory\s*\(', text):
            continue
        new = re.sub(r'\bDefaultRenderersFactory\s*\(', 'EnhancedRenderersFactory(', text)
        # If the old class was imported, remove the import to avoid an unused import warning.
        new = re.sub(r'^\s*import\s+androidx\.media3\.exoplayer\.DefaultRenderersFactory\s*\n', '', new, flags=re.M)
        path.write_text(new, encoding="utf-8")
        log(f"EDIT {path.relative_to(ROOT)}: DefaultRenderersFactory -> EnhancedRenderersFactory")
        return

    # Common simpler path: ExoPlayer.Builder(context) with the default renderer factory.
    simple_context = r'(?:[A-Za-z_][A-Za-z0-9_]*)(?:\.[A-Za-z_][A-Za-z0-9_]*)*'
    one_arg = re.compile(rf'ExoPlayer\.Builder\(\s*({simple_context})\s*\)')
    for path in files:
        text = path.read_text(encoding="utf-8")
        match = one_arg.search(text)
        if not match:
            continue
        ctx = match.group(1)
        new = text[:match.start()] + f"ExoPlayer.Builder({ctx}, EnhancedRenderersFactory({ctx}))" + text[match.end():]
        path.write_text(new, encoding="utf-8")
        log(f"EDIT {path.relative_to(ROOT)}: installed EnhancedRenderersFactory in ExoPlayer.Builder")
        return

    # Last controlled fallback: replace an explicitly supplied renderer factory only in the player.
    two_arg = re.compile(
        rf'ExoPlayer\.Builder\(\s*({simple_context})\s*,\s*([A-Za-z_][A-Za-z0-9_.]*)\s*\)'
    )
    for path in files:
        text = path.read_text(encoding="utf-8")
        match = two_arg.search(text)
        if not match:
            continue
        ctx = match.group(1)
        old_factory = match.group(2)
        new = text[:match.start()] + f"ExoPlayer.Builder({ctx}, EnhancedRenderersFactory({ctx}))" + text[match.end():]
        path.write_text(new, encoding="utf-8")
        log(f"EDIT {path.relative_to(ROOT)}: replaced renderer factory '{old_factory}' with EnhancedRenderersFactory")
        return

    fail("Could not locate the movie/TV ExoPlayer renderer factory. No unsafe global patch was attempted.")


def build_pairs(text: str) -> tuple[dict[int, int], dict[int, int]]:
    paren_stack: list[int] = []
    brace_stack: list[int] = []
    parens: dict[int, int] = {}
    braces: dict[int, int] = {}
    i = 0
    state = "code"
    while i < len(text):
        c = text[i]
        n = text[i + 1] if i + 1 < len(text) else ""
        if state == "code":
            if text.startswith('"""', i):
                state = "triple"
                i += 3
                continue
            if c == '"':
                state = "string"
            elif c == "'":
                state = "char"
            elif c == '/' and n == '/':
                state = "line_comment"
                i += 1
            elif c == '/' and n == '*':
                state = "block_comment"
                i += 1
            elif c == '(':
                paren_stack.append(i)
            elif c == ')' and paren_stack:
                op = paren_stack.pop()
                parens[op] = i
            elif c == '{':
                brace_stack.append(i)
            elif c == '}' and brace_stack:
                op = brace_stack.pop()
                braces[op] = i
        elif state == "string":
            if c == '\\':
                i += 1
            elif c == '"':
                state = "code"
        elif state == "char":
            if c == '\\':
                i += 1
            elif c == "'":
                state = "code"
        elif state == "triple":
            if text.startswith('"""', i):
                state = "code"
                i += 3
                continue
        elif state == "line_comment":
            if c == '\n':
                state = "code"
        elif state == "block_comment":
            if c == '*' and n == '/':
                state = "code"
                i += 1
        i += 1
    return parens, braces


def identifier_before(text: str, pos: int) -> tuple[str, int] | None:
    chunk = text[max(0, pos - 180):pos]
    m = re.search(r'([A-Za-z_][A-Za-z0-9_.]*)\s*$', chunk)
    if not m:
        return None
    name = m.group(1)
    start = max(0, pos - 180) + m.start(1)
    return name, start


def line_indent(text: str, pos: int) -> str:
    line_start = text.rfind('\n', 0, pos) + 1
    return re.match(r'[ \t]*', text[line_start:pos]).group(0)


def find_subtitle_button_insertion(text: str) -> tuple[int, str, str] | None:
    parens, braces = build_pairs(text)
    markers = [
        r'ClosedCaption',
        r'ClosedCaptions',
        r'\bSubtitles?\b',
        r'\bSubtitle\b',
        r'contentDescription\s*=\s*"(?:CC|Subtitles?)"',
        r'text\s*=\s*"CC"',
    ]
    marker_positions: list[int] = []
    for pat in markers:
        marker_positions.extend(m.start() for m in re.finditer(pat, text, flags=re.I))
    marker_positions = sorted(set(marker_positions))

    candidates: list[tuple[int, int, int, str, str]] = []
    # tuple score, insertion_pos, start, call_name, indent
    for marker in marker_positions:
        for op, cl in parens.items():
            if not (op < marker < cl):
                continue
            ident = identifier_before(text, op)
            if ident is None:
                continue
            name, start = ident
            lname = name.lower()
            score = 0
            if "button" in lname:
                score += 100
            if "control" in lname:
                score += 40
            if "action" in lname:
                score += 20
            if "icon" in lname:
                score += 10
            if score == 0:
                continue
            # Prefer the tightest enclosing call and occurrences near icon/content descriptions.
            score += max(0, 30 - min(30, (cl - op) // 100))
            around = text[max(0, marker - 180):min(len(text), marker + 180)].lower()
            if "subtitle" in around or "caption" in around:
                score += 25
            end = cl + 1
            j = end
            while j < len(text) and text[j].isspace():
                j += 1
            if j < len(text) and text[j] == '{' and j in braces:
                end = braces[j] + 1
            candidates.append((score, end, start, name, line_indent(text, start)))

    if candidates:
        candidates.sort(key=lambda x: (x[0], -abs(x[1] - x[2])), reverse=True)
        score, end, start, name, indent = candidates[0]
        return end, indent, f"after {name} containing subtitle/CC marker (score {score})"

    # Fallback: insert before the closing brace of the tightest Row/LazyRow containing a subtitle marker.
    for marker in marker_positions:
        row_candidates: list[tuple[int, int, int, str]] = []
        for op, cl in parens.items():
            if not (op < marker < cl):
                continue
            ident = identifier_before(text, op)
            if ident is None:
                continue
            name, start = ident
            if name.split('.')[-1] not in {"Row", "LazyRow"}:
                continue
            j = cl + 1
            while j < len(text) and text[j].isspace():
                j += 1
            if j < len(text) and text[j] == '{' and j in braces and j < marker < braces[j]:
                row_candidates.append((braces[j], start, j, name))
        if row_candidates:
            close, start, brace_open, name = min(row_candidates, key=lambda x: x[0] - x[2])
            indent = line_indent(text, start) + "    "
            return close, indent, f"inside enclosing {name} controls row"
    return None


def choose_player_ui_file(files: list[Path]) -> list[Path]:
    preferred = PLAYER_DIR / "PlayerScreen.kt"
    ordered: list[Path] = []
    if preferred.exists():
        ordered.append(preferred)
    scored: list[tuple[int, Path]] = []
    for p in files:
        if p == preferred:
            continue
        t = p.read_text(encoding="utf-8", errors="ignore")
        score = 0
        if "@Composable" in t:
            score += 10
        low = t.lower()
        if "subtitle" in low:
            score += 8
        if "caption" in low:
            score += 8
        if "icon" in low:
            score += 4
        if "button" in low:
            score += 4
        if score:
            scored.append((score, p))
    ordered.extend(p for _, p in sorted(scored, key=lambda x: x[0], reverse=True))
    return ordered


def patch_player_ui() -> None:
    files = player_kotlin_files()
    for path in choose_player_ui_file(files):
        text = path.read_text(encoding="utf-8")
        if "AudioEnhancementControls()" in text:
            log(f"OK   {path.relative_to(ROOT)} already contains AudioEnhancementControls()")
            return
        insertion = find_subtitle_button_insertion(text)
        if insertion is None:
            continue
        pos, indent, reason = insertion
        injected = f"\n{indent}AudioEnhancementControls()"
        new = text[:pos] + injected + text[pos:]
        path.write_text(new, encoding="utf-8")
        log(f"EDIT {path.relative_to(ROOT)}: inserted AudioEnhancementControls() {reason}")
        return
    fail("Could not safely locate the CC/subtitle control in the current player UI.")


def sanity_check() -> None:
    # Make failures obvious in CI rather than producing an APK with a cosmetic or missing feature.
    gradle = (APP / "build.gradle.kts").read_text(encoding="utf-8")
    if f'applicationId = "{NEW_APP_ID}"' not in gradle:
        fail("Side-by-side applicationId sanity check failed")

    player_text = "\n".join(p.read_text(encoding="utf-8", errors="ignore") for p in PLAYER_DIR.glob("*.kt"))
    if "AudioEnhancementControls()" not in player_text:
        fail("UI injection sanity check failed")
    if "EnhancedRenderersFactory" not in player_text:
        fail("Audio renderer injection sanity check failed")

    required = [
        PLAYER_DIR / "AudioEnhancementControls.kt",
        PLAYER_DIR / "EnhancedRenderersFactory.kt",
        PLAYER_DIR / "audio" / "AudioEnhancementDsp.kt",
        PLAYER_DIR / "audio" / "PlayTorrioAudioEnhancementProcessor.kt",
    ]
    missing = [str(p.relative_to(ROOT)) for p in required if not p.exists()]
    if missing:
        fail("Missing generated source files: " + ", ".join(missing))


def main() -> None:
    log("PlayTorrio Enhanced patcher")
    log(f"Repo root: {ROOT}")
    copy_payload()
    patch_application_id()
    patch_player_audio_sink()
    patch_player_ui()
    sanity_check()
    log("SUCCESS: audio enhancements and side-by-side app identity applied")
    REPORT.write_text("\n".join(log_lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
