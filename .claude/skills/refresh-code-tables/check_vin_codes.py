"""Checks data/src/main/assets/vin_codes.json and prints what changed since the committed version.

Usage (from the repository root): python3 -I .claude/skills/refresh-code-tables/check_vin_codes.py [asset]
Exits non-zero, saying why, when the asset doesn't follow the format the app reads (VinCodeCatalog).
"""
import json
import re
import subprocess
import sys

ASSET = "data/src/main/assets/vin_codes.json"
# Section id -> number of VIN characters it covers (VinSection in :domain).
SECTIONS = {
    "manufacturer": 3, "model": 1, "body": 1, "restraints": 1,
    "battery": 1, "motor": 1, "model_year": 1, "factory": 1,
}
VIN_CHARS = re.compile(r"^[A-HJ-NPR-Z0-9]+$")
# C0 and C1 controls, zero-width and bidi characters: they can hide or reorder text, in a terminal or in the app.
UNSAFE = re.compile("[\x00-\x1f\x7f-\x9f\u200b-\u200f\u202a-\u202e\u2066-\u2069\ufeff]")
TEXT_MAX = 200
LANGUAGES = {"en", "fr"}


def key(entry):
    return entry["section"], entry["code"]


def check(table):
    errors = []
    if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", str(table.get("updated", ""))):
        errors.append("`updated` must be a yyyy-MM-dd date")
    codes = table.get("codes")
    if not isinstance(codes, list) or not codes:
        return errors + ["`codes` must be a non-empty list"]
    seen = set()
    for i, entry in enumerate(codes):
        where = f"codes[{i}] ({ascii(entry.get('section'))} {ascii(entry.get('code'))})"
        section, code = entry.get("section"), entry.get("code", "")
        if section not in SECTIONS:
            errors.append(f"{where}: unknown section, expected one of {sorted(SECTIONS)}")
            continue
        if len(code) != SECTIONS[section] or not VIN_CHARS.match(code):
            errors.append(f"{where}: a {section} code is {SECTIONS[section]} VIN character(s), never I, O or Q")
        value = entry.get("value", {})
        if set(value) != LANGUAGES or any(not str(v).strip() for v in value.values()):
            errors.append(f"{where}: `value` needs a non-empty text in each of {sorted(LANGUAGES)}, and only those")
        elif any(len(str(v)) > TEXT_MAX or UNSAFE.search(str(v)) for v in value.values()):
            errors.append(f"{where}: a text is longer than {TEXT_MAX} characters, or holds a control, zero-width or bidi character")
        if key(entry) in seen:
            errors.append(f"{where}: duplicate of an earlier entry with the same section and code")
        seen.add(key(entry))
    return errors


def committed(path):
    try:
        text = subprocess.run(["git", "show", f"HEAD:{path}"], capture_output=True, text=True, check=True).stdout
        return json.loads(text)["codes"]
    except (subprocess.CalledProcessError, ValueError, KeyError):
        return []


def describe(entry):
    return f"{entry['section']:<12} {entry['code']:<4} {entry['value']['en']}"


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else ASSET
    with open(path, encoding="utf-8") as f:
        table = json.load(f)
    errors = check(table)
    if errors:
        print("\n".join(errors))
        sys.exit(1)
    codes = table["codes"]
    print(f"{len(codes)} codes, updated {table['updated']}")
    for section in SECTIONS:
        print(f"  {section:<12} {sum(1 for c in codes if c['section'] == section)}")
    before = {key(c): c for c in committed(ASSET)}
    after = {key(c): c for c in codes}
    for k in after.keys() - before.keys():
        print("+ " + describe(after[k]))
    for k in before.keys() - after.keys():
        print("- " + describe(before[k]))
    for k in after.keys() & before.keys():
        if after[k]["value"] != before[k]["value"]:
            print("~ " + describe(after[k]) + f"   (was: {before[k]['value']['en']})")


if __name__ == "__main__":
    main()
