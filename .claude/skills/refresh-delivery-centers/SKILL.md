---
name: refresh-delivery-centers
description: Refresh the Tesla delivery centres bundled in the app (data/src/main/assets/delivery_centers.json) from tesla.com/findus, through the user's Chrome. Use when asked to update, regenerate or re-fetch the delivery centre list, or when an order's centre isn't found in it.
---

# Refresh the delivery centre list

The app finds an order's delivery centre by the name Tesla gives (`deliveryAddressTitle`) in
`data/src/main/assets/delivery_centers.json`, then opens it in Google Maps. Tesla has no public API for this list;
it comes from the two endpoints behind the map at https://www.tesla.com/fr_FR/findus:

- `GET /api/findus/get-locations?country=FR&view=map`: every Tesla location in the world (despite `country=FR`),
  with slug, coordinates and `location_type`. Delivery centres have `delivery_center` in it (about 440 sites).
- `GET /api/findus/get-location-details?locationSlug=<slug>&functionTypes=delivery_center&locale=fr_FR&isInHkMoTw=<bool>`:
  name, address and status of one site.

## Constraints learned the hard way

- **Use Claude in Chrome** (the user's Chrome, `mcp__claude-in-chrome__*`), not the built-in browser. Akamai in front
  of tesla.com blocked the built-in browser's session after a few hundred calls and kept blocking it at any pace,
  while the user's Chrome went through. `curl` is always refused.
- **One detail request every 4 seconds**, one at a time. Three in parallel every 300 ms got blocked after ~260 calls;
  4 s went through all of them. A full run takes about 30 minutes.
- A block is a **403 with an HTML "Access Denied" page** (reference `errors.edgesuite.net`); real API errors are JSON.
  The script pauses 90 s on a 403, retries once, then stops. Never try to get around the block (other headers,
  other tools, faster retries): stop and tell the user.
- `/cua-api/tesla-locations` answers 429 with a bot challenge (`cpr_chlge`): don't use it.
- `javascript_tool` truncates its result: a string at about 1,000 characters, an array after 100 items. Read the
  rows as **arrays of at most 50 strings**.

## Steps

1. Load the Claude in Chrome tools in one ToolSearch call: `tabs_context_mcp`, `tabs_create_mcp`, `navigate`,
   `javascript_tool`, `tabs_close_mcp`. Call `tabs_context_mcp`, open a new tab, navigate to
   https://www.tesla.com/fr_FR/findus. If a site permission is asked, wait for the user.
2. Run the content of `fetch-centers.js` (next to this file) with `javascript_tool`. It returns `started` and keeps
   collecting in the page; leave the tab open.
3. Check progress with `[window.__dc.status, window.__dc.done, window.__dc.total, window.__dc.errors]`. Between
   checks, wait with a background `sleep 300` (Bash, `run_in_background`) rather than polling. Statuses: `listing`,
   `details`, `paused after 403`, `blocked`, `stopped`, `finished`, `failed: …`. To abort (the user changes their
   mind, or a dry run), set `window.__dc.stop = true`.
   - `blocked`: stop, report how far it got and the error, and ask the user whether to retry later. Don't write a
     partial asset.
   - Other errors (404 and the like) are sites without details; they are left out, list them in the report.
4. When `finished`, read the rows in chunks: `window.__dc.rows.slice(0, 50)`, then `slice(50, 100)`, and so on until
   an empty array. Each item is one TSV line: slug, name, country, city, address, status, latitude, longitude.
   Write them, in order, one per line, to `rows.tsv` in the scratchpad directory, then check the line count matches
   `window.__dc.rows.length`.
5. Build the asset from the repository root:
   ```bash
   python3 -I .claude/skills/refresh-delivery-centers/build_asset.py <scratchpad>/rows.tsv data/src/main/assets/delivery_centers.json
   ```
   It refuses an empty or malformed file, and a list more than 20 % shorter than the current one (a run cut short);
   pass `--allow-shrink` only after the user confirms Tesla really removed that many. It prints the count, the
   statuses, and the names added and removed since the previous list.
6. Close the tab you opened. Run `./gradlew :domain:test` (with the `JAVA_HOME` from CLAUDE.md), then report
   to the user: how many centres, the added and removed names (the French ones in particular) and any errors. Don't
   commit unless asked; if asked, update the snapshot date in the `DeliveryCenterCatalog` KDoc too.

Keep the list complete: closed and pending sites stay in, since an order can name a centre before it opens. Only
the app's fields (name, country, city, address, coordinates) are shipped; no intermediate CSV is kept in the repository.
