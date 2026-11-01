"""Turns the TSV collected by fetch-centers.js into the app's assets/delivery_centers.json.

Usage: python3 -I build_asset.py <rows.tsv> <assets/delivery_centers.json> [--allow-shrink]

TSV columns: slug, name, country, city, address, status, latitude, longitude.
Prints how many centres there are and which names appeared or disappeared since the current asset.
"""
import collections
import json
import sys


def main(tsv_path, asset_path):
    rows = []
    for n, line in enumerate(open(tsv_path, encoding="utf-8"), 1):
        if not line.strip():
            continue
        cols = line.rstrip("\n").split("\t")
        if len(cols) != 8:
            sys.exit(f"line {n}: expected 8 columns, got {len(cols)}")
        slug, name, country, city, address, status, lat, lng = (c.strip() for c in cols)
        if not name:
            print(f"skipped, no name: {slug}")
            continue
        rows.append({"name": name, "country": country, "city": city, "address": address,
                     "lat": float(lat), "lng": float(lng), "status": status})

    if not rows:
        sys.exit("no rows: the asset was left as it is")

    try:
        before = {c["name"] for c in json.load(open(asset_path, encoding="utf-8"))}
    except FileNotFoundError:
        before = set()
    after = {r["name"] for r in rows}
    # A run cut short must not wipe the list: refuse a big drop unless it is confirmed with --allow-shrink.
    if before and len(after) < 0.8 * len(before) and "--allow-shrink" not in sys.argv:
        sys.exit(f"{len(after)} centres against {len(before)} before: the asset was left as it is "
                 "(rerun with --allow-shrink if Tesla really removed that many)")

    rows.sort(key=lambda r: (r["country"], r["city"], r["name"]))
    # The app matches on name and country only; the status is reported here, not shipped.
    asset = [{k: r[k] for k in ("name", "country", "city", "address", "lat", "lng")} for r in rows]
    with open(asset_path, "w", encoding="utf-8") as f:
        json.dump(asset, f, ensure_ascii=False, separators=(",", ":"))

    print(f"{len(asset)} centres in {len({r['country'] for r in rows})} countries")
    print("statuses:", dict(collections.Counter(r["status"] for r in rows).most_common()))
    print("added:", sorted(after - before) or "none")
    print("removed:", sorted(before - after) or "none")
    dup = [n for n, c in collections.Counter((r["name"].lower(), r["country"]) for r in rows).items() if c > 1]
    if dup:
        print("same name in the same country (the app can't tell them apart):", dup)


if __name__ == "__main__":
    main(*sys.argv[1:3])
