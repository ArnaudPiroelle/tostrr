"""Builds data/src/main/assets/vin_codes.json from the `vin_decoder` section of tesla-order-status-tracker.de.

Usage (from the repository root):
    python3 -I .claude/skills/refresh-vin-codes/build_asset.py <response.json> [asset]

<response.json> is the answer to `POST https://www.tesla-order-status-tracker.de/get/refresh_data.php` with the body
`{"include":["vin_decoder"]}`. Codes the site lists as `unknown` (no value yet) are left out. Positions 1-3 come as
two entries per code, `manufacturer` and `country`: they are merged into one `manufacturer` entry ("Tesla, China").
A French text already in the asset is kept while the English one is unchanged; new or reworded entries get an empty
`fr`, listed at the end, to translate before running check_asset.py.
"""
import json
import sys

ASSET = "data/src/main/assets/vin_codes.json"
# The app's sections (VinSection in :domain), in VIN order.
ORDER = ["manufacturer", "model", "body", "restraints", "battery", "motor", "model_year", "factory"]


def main():
    response_path = sys.argv[1]
    asset_path = sys.argv[2] if len(sys.argv) > 2 else ASSET
    with open(response_path, encoding="utf-8") as f:
        section = json.load(f).get("vin_decoder") or {}
    entries = [e for e in section.get("vin_codes", []) if e.get("status") == "known" and e.get("value")]
    if not entries:
        sys.exit("The response has no known VIN codes: is it the vin_decoder section?")

    english = {}
    countries = {e["code"]: e["value"] for e in entries if e["section"] == "country"}
    for e in entries:
        if e["section"] == "country":
            continue
        if e["section"] not in ORDER:
            print(f"skipped, unknown section: {e['section']} {e['code']}")
            continue
        value = e["value"].strip()
        if e["section"] == "manufacturer" and e["code"] in countries:
            value = f"{value}, {countries[e['code']].strip()}"
        english[(e["section"], e["code"])] = value
    for code in countries.keys() - {code for (s, code) in english if s == "manufacturer"}:
        print(f"skipped, country without manufacturer: {code}")

    try:
        with open(asset_path, encoding="utf-8") as f:
            previous = {(c["section"], c["code"]): c["value"] for c in json.load(f)["codes"]}
    except (OSError, ValueError, KeyError):
        previous = {}

    codes, to_translate = [], []
    for key in sorted(english, key=lambda k: (ORDER.index(k[0]), k[1])):
        old = previous.get(key, {})
        fr = old.get("fr", "") if old.get("en") == english[key] else ""
        if not fr:
            to_translate.append(key)
        codes.append({"section": key[0], "code": key[1], "value": {"en": english[key], "fr": fr}})

    updated = str(section.get("last_updated") or section.get("fetched_at") or "")[:10]
    with open(asset_path, "w", encoding="utf-8") as f:
        json.dump({"updated": updated, "codes": codes}, f, ensure_ascii=False, indent=1)
        f.write("\n")

    unknown = [f"{e['section']} {e['code']}" for e in section.get("vin_codes", []) if e.get("status") != "known"]
    print(f"{len(codes)} codes written to {asset_path}, table updated {updated}")
    if unknown:
        print("still unknown on the site: " + ", ".join(unknown))
    if to_translate:
        print(f"{len(to_translate)} French texts to write:")
        for key in to_translate:
            print(f"  {key[0]} {key[1]}: {english[key]}")


if __name__ == "__main__":
    main()
