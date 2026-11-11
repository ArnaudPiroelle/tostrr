"""
Builds app/src/main/assets/globe_land.bin, the dots the shipping globe draws for land, from Natural Earth's 1:50m land
polygons (public domain):

    curl -sL -o ne_50m_land.geojson \
        https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_land.geojson
    python3 tools/globe/land_dots.py ne_50m_land.geojson app/src/main/assets/globe_land.bin

Dots sit on rows one STEP of latitude apart, each row holding as many as fit its circumference, so they are evenly
spread over the sphere; a dot is kept when its centre is on land (scanline even-odd fill over every ring). The file is
a big-endian int32 count, then per dot its latitude and longitude in hundredths of a degree as int16.
"""
import json
import math
import struct
import sys

STEP = 1.0


def rings(geojson):
    for feature in geojson["features"]:
        geometry = feature["geometry"]
        polygons = [geometry["coordinates"]] if geometry["type"] == "Polygon" else geometry["coordinates"]
        for polygon in polygons:
            yield from polygon


def crossings(all_rings, lat):
    xs = []
    for ring in all_rings:
        for (x1, y1), (x2, y2) in zip(ring, ring[1:]):
            if (y1 > lat) != (y2 > lat):
                xs.append(x1 + (lat - y1) * (x2 - x1) / (y2 - y1))
    return sorted(xs)


def main(source, target):
    with open(source) as f:
        all_rings = list(rings(json.load(f)))
    dots = []
    rows = int(180 / STEP)
    for row in range(rows):
        lat = -90 + (row + 0.5) * STEP
        xs = crossings(all_rings, lat)
        count = max(1, round(360 / STEP * math.cos(math.radians(lat))))
        for i in range(count):
            lon = -180 + (i + 0.5) * 360 / count
            inside = sum(1 for x in xs if x < lon) % 2 == 1
            if inside:
                dots.append((round(lat * 100), round(lon * 100)))
    with open(target, "wb") as f:
        f.write(struct.pack(">i", len(dots)))
        for lat, lon in dots:
            f.write(struct.pack(">hh", lat, lon))
    print(f"{len(dots)} land dots")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
