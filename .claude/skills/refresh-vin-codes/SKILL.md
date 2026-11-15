---
name: refresh-vin-codes
description: Update the Tesla VIN code table bundled in the app (data/src/main/assets/vin_codes.json), used by the order detail's VIN decoder, from tesla-order-status-tracker.de. Use when asked to update or re-fetch the VIN decoder's codes, or when a VIN shows "Not yet known" for one of its parts.
---

# Update the VIN code table

The detail's "Decode the VIN" sheet cuts the order's VIN into its parts (`domain/vin/VinDecoder`) and words each one
from `data/src/main/assets/vin_codes.json`, read by `data/catalog/VinCodeCatalog`. **The app never calls the site**:
it ships with the file, and this skill is how the file is refreshed. A code missing from it shows as "Not yet known".

## Where the table comes from

The `vin_decoder` section of tesla-order-status-tracker.de's refresh endpoint (public, no token):

```bash
curl -s -X POST -H 'Content-Type: application/json' -d '{"include":["vin_decoder"]}' \
  https://www.tesla-order-status-tracker.de/get/refresh_data.php -o <scratchpad>/vin_response.json
```

A `GET` answers 405. The answer is `{ok, fetched_at, vin_decoder: {fetched_at, last_updated, count, vin_codes[]}}`,
each code being `{section, positions, code, value, status, updated_at}`: `status` is `known`, or `unknown` with a null
`value` (a code seen on cars but not documented yet). Sections: `manufacturer` and `country` (both at positions 1-3,
one entry each per code), `model` (4), `body` (5), `restraints` (6), `battery` (7), `motor` (8), `model_year` (10),
`factory` (11). The table is flat: a code meaning different things per model says so in its text.

Use only this endpoint for the content: don't look the codes up elsewhere, and send nothing about the user (no VIN,
no order) to it; the request body above is all it needs.

## The app's file

```json
{
 "updated": "2026-11-15",
 "codes": [
  { "section": "manufacturer", "code": "LRW", "value": { "en": "Tesla, China", "fr": "Tesla, Chine" } }
 ]
}
```

- `section`: the site's, except `country`, merged into `manufacturer` (`"Tesla, China"`). Positions 9 (check digit)
  and 12-17 (serial number) are computed from the VIN, never listed.
- `value.en`: the site's text, as is. `value.fr`: our French translation of it (the app ships English and French).
- `updated`: the site's `last_updated` day, shown under the table in the sheet.
- `unknown` codes are left out.

## Steps

1. Fetch the section with the `curl` above into the scratchpad directory. Check `ok` is true and `vin_codes` is not
   empty; if the call fails or the shape changed, stop and tell the user rather than writing anything.
2. Build the asset from the repository root:
   ```bash
   python3 -I .claude/skills/refresh-vin-codes/build_asset.py <scratchpad>/vin_response.json
   ```
   It keeps the French texts whose English is unchanged, gives new or reworded entries an empty `fr`, and lists
   them, with the codes the site still marks unknown.
3. Write the missing French texts in the file: short, faithful translations in the tone of the app's French
   strings (`conduite à gauche` for LHD, `Monospace` for MPV, `Berline` for sedan/hatchback, `détection d'occupant
   (PODS)`, `Un moteur` / `Deux moteurs` / `Trois moteurs`); model names and years stay as they are.
4. Check it:
   ```bash
   python3 -I .claude/skills/refresh-vin-codes/check_asset.py
   ```
   It refuses a malformed file (unknown section, wrong code length, I/O/Q, an empty English or French text, a
   duplicate) and prints the counts per section and the entries added (`+`), removed (`-`) or reworded (`~`) since
   the last commit.
5. Run the tests that read it (with the `JAVA_HOME` from CLAUDE.md):
   `./gradlew :domain:test --tests 'app.tostrr.domain.vin.*' :data:testDebugUnitTest --tests 'app.tostrr.data.catalog.VinCodeCatalogTest'`.
   A reworded entry can break `VinCodeCatalogTest`'s expected texts: update them to the new wording.
6. Report to the user: the table's date, what was added, removed or reworded, and the codes still unknown. Don't
   commit unless asked.
