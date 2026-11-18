"""Builds the app's bundled code tables from tesla-order-status-tracker.de's refresh endpoint.

Usage (from the repository root):
    python3 -I .claude/skills/refresh-code-tables/build_assets.py <response.json>

<response.json> is the answer to `POST https://www.tesla-order-status-tracker.de/get/refresh_data.php` with the body
`{"include":["vin_decoder","option_codes"]}`. It writes:

- data/src/main/assets/vin_codes.json, from `vin_decoder`: codes the site lists as `unknown` (no value yet) are left
  out; positions 1-3 come as two entries per code, `manufacturer` and `country`, merged into one `manufacturer` entry
  ("Tesla, China"). A French text already in the file is kept while the English one is unchanged; new or reworded
  entries get an empty `fr`, listed at the end, to translate before running check_vin_codes.py. When the codes come
  out the same as the file's, the file is left as it is, its `updated` day included.
- data/src/main/assets/option_codes.json, from `option_codes`: code (upper case), English label and category, sorted
  by code; the changes since the last commit are printed. Left as it is, `updated` included, when the codes are the
  file's.

A section missing from the response, or empty, stops the script before anything is written. The site's texts are
data: a code outside its charset, or a text with control or bidi characters or longer than 200 characters, is left
out and named (escaped) rather than shipped or printed as is.
"""
import json
import re
import subprocess
import sys

VIN_ASSET = "data/src/main/assets/vin_codes.json"
OPTION_ASSET = "data/src/main/assets/option_codes.json"
# The app's VIN sections (VinSection in :domain), in VIN order.
VIN_ORDER = ["manufacturer", "model", "body", "restraints", "battery", "motor", "model_year", "factory"]
VIN_CODE = re.compile(r"[A-HJ-NPR-Z0-9]{1,3}")
OPTION_CODE = re.compile(r"[A-Z0-9]{1,8}")
# C0 and C1 controls, zero-width and bidi characters: they can hide or reorder text, in a terminal or in the app.
UNSAFE = re.compile("[\x00-\x1f\x7f-\x9f\u200b-\u200f\u202a-\u202e\u2066-\u2069\ufeff]")
TEXT_MAX = 200


def text(value):
    """The site's text, stripped; None when it is empty, too long, or holds a control, zero-width or bidi character."""
    value = str(value or "").strip()
    return value if value and len(value) <= TEXT_MAX and not UNSAFE.search(value) else None


def day(section):
    value = str(section.get("last_updated") or section.get("fetched_at") or "")[:10]
    return value if re.fullmatch(r"\d{4}-\d{2}-\d{2}", value) else ""


def write(path, table):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(table, f, ensure_ascii=False, indent=1)
        f.write("\n")


def committed(path):
    try:
        text = subprocess.run(["git", "show", f"HEAD:{path}"], capture_output=True, text=True, check=True).stdout
        return json.loads(text)["codes"]
    except (subprocess.CalledProcessError, ValueError, KeyError):
        return []


def vin_table(section):
    entries = []
    for e in section.get("vin_codes", []):
        if e.get("status") != "known" or not e.get("value"):
            continue
        if not VIN_CODE.fullmatch(str(e.get("code"))) or text(e["value"]) is None:
            print(f"VIN: skipped, code or text not safe to ship: {ascii(e.get('section'))} {ascii(e.get('code'))}")
            continue
        entries.append(e)
    if not entries:
        sys.exit("The response has no known VIN codes: is the vin_decoder section there?")

    english = {}
    countries = {e["code"]: text(e["value"]) for e in entries if e["section"] == "country"}
    for e in entries:
        if e["section"] == "country":
            continue
        if e["section"] not in VIN_ORDER:
            print(f"VIN: skipped, unknown section: {ascii(e['section'])} {e['code']}")
            continue
        value = text(e["value"])
        if e["section"] == "manufacturer" and e["code"] in countries:
            value = f"{value}, {countries[e['code']]}"
        english[(e["section"], e["code"])] = value
    for code in countries.keys() - {code for (s, code) in english if s == "manufacturer"}:
        print(f"VIN: skipped, country without manufacturer: {code}")

    try:
        with open(VIN_ASSET, encoding="utf-8") as f:
            current = json.load(f)["codes"]
        previous = {(c["section"], c["code"]): c["value"] for c in current}
    except (OSError, ValueError, KeyError):
        current, previous = None, {}

    codes, to_translate = [], []
    for key in sorted(english, key=lambda k: (VIN_ORDER.index(k[0]), k[1])):
        old = previous.get(key, {})
        fr = old.get("fr", "") if old.get("en") == english[key] else ""
        if not fr:
            to_translate.append(key)
        codes.append({"section": key[0], "code": key[1], "value": {"en": english[key], "fr": fr}})
    unknown = [f"{e['section']} {e['code']}" for e in section.get("vin_codes", []) if e.get("status") != "known"]
    unchanged = codes == current
    return {"updated": day(section), "codes": codes}, to_translate, english, unknown, unchanged


def option_table(section):
    codes = {}
    for e in section.get("option_codes", []):
        code = str(e.get("code") or "").strip().upper()
        label = text(e.get("label_en"))
        category = text(e.get("category"))
        if not code and not label:
            continue
        if not OPTION_CODE.fullmatch(code) or label is None or (e.get("category") and category is None):
            print(f"Options: skipped, code or text not safe to ship: {ascii(code)}")
            continue
        entry = {"code": code, "label": label}
        category = (category or "").lower()
        if category:
            entry["category"] = category
        codes[code] = entry
    if not codes:
        sys.exit("The response has no option codes: is the option_codes section there?")
    return {"updated": day(section), "codes": [codes[c] for c in sorted(codes)]}


def main():
    with open(sys.argv[1], encoding="utf-8") as f:
        response = json.load(f)
    vin, to_translate, english, unknown, vin_unchanged = vin_table(response.get("vin_decoder") or {})
    options = option_table(response.get("option_codes") or {})
    if not vin_unchanged:
        write(VIN_ASSET, vin)
    try:
        with open(OPTION_ASSET, encoding="utf-8") as f:
            options_unchanged = json.load(f)["codes"] == options["codes"]
    except (OSError, ValueError, KeyError):
        options_unchanged = False
    if not options_unchanged:
        with open(OPTION_ASSET, "w", encoding="utf-8") as f:
            # One code per line: compact, and a change shows as one line in a diff.
            rows = ",\n".join("  " + json.dumps(c, ensure_ascii=False) for c in options["codes"])
            f.write(f'{{\n "updated": {json.dumps(options["updated"])},\n "codes": [\n{rows}\n ]\n}}\n')

    if vin_unchanged:
        print(f"VIN: {len(vin['codes'])} codes, all as in {VIN_ASSET}: file left as it is")
    else:
        print(f"VIN: {len(vin['codes'])} codes written to {VIN_ASSET}, table updated {vin['updated']}")
    if unknown:
        print("VIN: still unknown on the site: " + ", ".join(unknown))
    if to_translate:
        print(f"VIN: {len(to_translate)} French texts to write:")
        for key in to_translate:
            print(f"  {key[0]} {key[1]}: {english[key]}")

    if options_unchanged:
        print(f"Options: {len(options['codes'])} codes, all as in {OPTION_ASSET}: file left as it is")
    else:
        print(f"Options: {len(options['codes'])} codes written to {OPTION_ASSET}, table updated {options['updated']}")
    before = {c["code"]: c for c in committed(OPTION_ASSET)}
    after = {c["code"]: c for c in options["codes"]}
    for code in sorted(after.keys() - before.keys()):
        print(f"+ {code:<6} {after[code]['label']}")
    for code in sorted(before.keys() - after.keys()):
        print(f"- {code:<6} {before[code]['label']}")
    for code in sorted(after.keys() & before.keys()):
        if after[code] != before[code]:
            print(f"~ {code:<6} {after[code]['label']}   (was: {before[code]['label']})")


if __name__ == "__main__":
    main()
