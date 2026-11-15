---
name: refresh-code-tables
description: Update the Tesla code tables bundled in the app from tesla-order-status-tracker.de - the VIN decoder's codes (data/src/main/assets/vin_codes.json) and the option code labels (data/src/main/assets/option_codes.json). Use when asked to update or re-fetch the VIN codes or the option codes, when a VIN shows "Not yet known" for one of its parts, or when an order's option shows no label.
---

# Update the bundled code tables

Two tables ship with the app, and **the app never calls the site**: this skill is how they are refreshed.

- `data/src/main/assets/vin_codes.json`, read by `data/catalog/VinCodeCatalog`: the detail's "Decode the VIN" sheet
  (`domain/vin/VinDecoder`) words each part of the VIN with it. A code missing from it shows as "Not yet known".
- `data/src/main/assets/option_codes.json`, read by `data/catalog/OptionCatalog`: the labels of the order's option
  codes (detail's configuration block, option-change notifications). A code missing from it shows without a label.

## Where the tables come from

The `vin_decoder` and `option_codes` sections of tesla-order-status-tracker.de's refresh endpoint (public, no token):

```bash
curl -s -X POST -H 'Content-Type: application/json' -d '{"include":["vin_decoder","option_codes"]}' \
  https://www.tesla-order-status-tracker.de/get/refresh_data.php -o <scratchpad>/tables_response.json
```

A `GET` answers 405. The answer is `{ok, fetched_at, vin_decoder: {…}, option_codes: {…}}`, each section being
`{fetched_at, last_updated, count, …}`:

- `vin_decoder.vin_codes[]`: `{section, positions, code, value, status, updated_at}`. `status` is `known`, or
  `unknown` with a null `value` (a code seen on cars but not documented yet). Sections: `manufacturer` and `country`
  (both at positions 1-3, one entry each per code), `model` (4), `body` (5), `restraints` (6), `battery` (7), `motor`
  (8), `model_year` (10), `factory` (11). The table is flat: a code meaning different things per model says so in
  its text.
- `option_codes.option_codes[]`: `{code, category, label_en, label_en_short, tff_stats_mapping, updated_at}`. Only
  `code`, `label_en` and `category` are kept.

Use only this endpoint for the content: don't look the codes up elsewhere, and send nothing about the user (no VIN,
no order, no option code) to it; the request body above is all it needs.

## The app's files

```json
{
 "updated": "2026-11-15",
 "codes": [
  { "section": "manufacturer", "code": "LRW", "value": { "en": "Tesla, China", "fr": "Tesla, Chine" } }
 ]
}
```

`vin_codes.json`: `section` is the site's, except `country`, merged into `manufacturer` (`"Tesla, China"`);
positions 9 (check digit) and 12-17 (serial number) are computed from the VIN, never listed. `value.en` is the
site's text as is, `value.fr` our French translation of it (the app ships English and French). `unknown` codes are
left out.

```json
{
 "updated": "2026-11-13",
 "codes": [
  {"code": "PPSW", "label": "Pearl White Multi-Coat", "category": "paints"}
 ]
}
```

`option_codes.json`: one code per line, sorted, upper case, the site's English label (the labels stay in English:
Tesla's product names) and its category.

In both, `updated` is the section's `last_updated` day.

## Steps

1. Fetch both sections with the `curl` above into the scratchpad directory. Check `ok` is true and both sections are
   there and not empty; if the call fails or the shape changed, stop and tell the user rather than writing anything.
2. Build both files from the repository root:
   ```bash
   python3 -I .claude/skills/refresh-code-tables/build_assets.py <scratchpad>/tables_response.json
   ```
   For the VIN table it keeps the French texts whose English is unchanged, gives new or reworded entries an empty
   `fr`, and lists them, with the codes the site still marks unknown. For the options it prints the codes added (`+`),
   removed (`-`) or relabelled (`~`) since the last commit.
3. Write the missing French texts in `vin_codes.json`: short, faithful translations in the tone of the app's French
   strings (`conduite à gauche` for LHD, `Monospace` for MPV, `Berline` for sedan/hatchback, `détection d'occupant
   (PODS)`, `Un moteur` / `Deux moteurs` / `Trois moteurs`); model names and years stay as they are.
4. Check the VIN table:
   ```bash
   python3 -I .claude/skills/refresh-code-tables/check_vin_codes.py
   ```
   It refuses a malformed file (unknown section, wrong code length, I/O/Q, an empty English or French text, a
   duplicate) and prints the counts per section and the entries added (`+`), removed (`-`) or reworded (`~`) since
   the last commit.
5. Run the tests that read them (with the `JAVA_HOME` from CLAUDE.md):
   `./gradlew :domain:test --tests 'app.tostrr.domain.vin.*' :data:testDebugUnitTest --tests 'app.tostrr.data.catalog.*'`.
   A reworded entry can break the expected texts of `VinCodeCatalogTest` or `OptionCatalogTest`: update them.
6. Report to the user: each table's date, what was added, removed or reworded, and the VIN codes still unknown.
   Don't commit unless asked.
