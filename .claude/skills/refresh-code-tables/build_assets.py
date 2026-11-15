"""Builds the app's bundled code tables from tesla-order-status-tracker.de's refresh endpoint.

Usage (from the repository root):
    python3 -I .claude/skills/refresh-code-tables/build_assets.py <response.json>

<response.json> is the answer to `POST https://www.tesla-order-status-tracker.de/get/refresh_data.php` with the body
`{"include":["vin_decoder","option_codes"]}`. It writes:

- data/src/main/assets/vin_codes.json, from `vin_decoder`: codes the site lists as `unknown` (no value yet) are left
  out; positions 1-3 come as two entries per code, `manufacturer` and `country`, merged into one `manufacturer` entry
  ("Tesla, China"). A French text already in the file is kept while the English one is unchanged; new or reworded
  entries get an empty `fr`, listed at the end, to translate before running check_vin_codes.py.
- data/src/main/assets/option_codes.json, from `option_codes`: code (upper case), English label and category, sorted
  by code; the changes since the last commit are printed.

A section missing from the response, or empty, stops the script before anything is written.
"""
import json
import subprocess
import sys

VIN_ASSET = "data/src/main/assets/vin_codes.json"
OPTION_ASSET = "data/src/main/assets/option_codes.json"
# The app's VIN sections (VinSection in :domain), in VIN order.
VIN_ORDER = ["manufacturer", "model", "body", "restraints", "battery", "motor", "model_year", "factory"]


def day(section):
    return str(section.get("last_updated") or section.get("fetched_at") or "")[:10]


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
    entries = [e for e in section.get("vin_codes", []) if e.get("status") == "known" and e.get("value")]
    if not entries:
        sys.exit("The response has no known VIN codes: is the vin_decoder section there?")

    english = {}
    countries = {e["code"]: e["value"].strip() for e in entries if e["section"] == "country"}
    for e in entries:
        if e["section"] == "country":
            continue
        if e["section"] not in VIN_ORDER:
            print(f"VIN: skipped, unknown section: {e['section']} {e['code']}")
            continue
        value = e["value"].strip()
        if e["section"] == "manufacturer" and e["code"] in countries:
            value = f"{value}, {countries[e['code']]}"
        english[(e["section"], e["code"])] = value
    for code in countries.keys() - {code for (s, code) in english if s == "manufacturer"}:
        print(f"VIN: skipped, country without manufacturer: {code}")

    try:
        with open(VIN_ASSET, encoding="utf-8") as f:
            previous = {(c["section"], c["code"]): c["value"] for c in json.load(f)["codes"]}
    except (OSError, ValueError, KeyError):
        previous = {}

    codes, to_translate = [], []
    for key in sorted(english, key=lambda k: (VIN_ORDER.index(k[0]), k[1])):
        old = previous.get(key, {})
        fr = old.get("fr", "") if old.get("en") == english[key] else ""
        if not fr:
            to_translate.append(key)
        codes.append({"section": key[0], "code": key[1], "value": {"en": english[key], "fr": fr}})
    unknown = [f"{e['section']} {e['code']}" for e in section.get("vin_codes", []) if e.get("status") != "known"]
    return {"updated": day(section), "codes": codes}, to_translate, english, unknown


def option_table(section):
    codes = {}
    for e in section.get("option_codes", []):
        code = str(e.get("code") or "").strip().upper()
        label = str(e.get("label_en") or "").strip()
        if not code or not label:
            continue
        entry = {"code": code, "label": label}
        category = str(e.get("category") or "").strip().lower()
        if category:
            entry["category"] = category
        codes[code] = entry
    if not codes:
        sys.exit("The response has no option codes: is the option_codes section there?")
    return {"updated": day(section), "codes": [codes[c] for c in sorted(codes)]}


def main():
    with open(sys.argv[1], encoding="utf-8") as f:
        response = json.load(f)
    vin, to_translate, english, unknown = vin_table(response.get("vin_decoder") or {})
    options = option_table(response.get("option_codes") or {})
    write(VIN_ASSET, vin)
    with open(OPTION_ASSET, "w", encoding="utf-8") as f:
        # One code per line: compact, and a change shows as one line in a diff.
        rows = ",\n".join("  " + json.dumps(c, ensure_ascii=False) for c in options["codes"])
        f.write(f'{{\n "updated": "{options["updated"]}",\n "codes": [\n{rows}\n ]\n}}\n')

    print(f"VIN: {len(vin['codes'])} codes written to {VIN_ASSET}, table updated {vin['updated']}")
    if unknown:
        print("VIN: still unknown on the site: " + ", ".join(unknown))
    if to_translate:
        print(f"VIN: {len(to_translate)} French texts to write:")
        for key in to_translate:
            print(f"  {key[0]} {key[1]}: {english[key]}")

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
