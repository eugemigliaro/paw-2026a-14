#!/usr/bin/env python3
"""Download raster map tiles for CABA into webapp assets.

Usage:
    python3 scripts/download-tiles.py
    TILE_URL='https://api.maptiler.com/maps/streets/{z}/{x}/{y}.png?key=XXX' \
        python3 scripts/download-tiles.py

Tile URL template must contain {z}, {x}, {y}. Default provider is OSM;
respect its tile usage policy (https://operations.osmfoundation.org/policies/tiles/)
and show "© OpenStreetMap contributors" attribution in the UI.
"""
import math
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

DEFAULT_BBOX = (-58.55, -34.71, -58.33, -34.52)  # CABA: minLon, minLat, maxLon, maxLat
DEFAULT_ZOOMS = (12, 16)                         # min..max inclusive
BBOX = tuple(float(v) for v in os.environ["BBOX"].split(",")) if "BBOX" in os.environ else DEFAULT_BBOX
_zmin, _zmax = (int(v) for v in os.environ.get("ZOOMS", f"{DEFAULT_ZOOMS[0]},{DEFAULT_ZOOMS[1]}").split(","))
ZOOMS = range(_zmin, _zmax + 1)
DEFAULT_TILE_URL = "https://api.maptiler.com/maps/openstreetmap/256/{z}/{x}/{y}.png"
TILE_URL = os.environ.get("TILE_URL", DEFAULT_TILE_URL)
MAPTILER_KEY = os.environ.get("MAPTILER_KEY", "")
if "api.maptiler.com" in TILE_URL and "key=" not in TILE_URL:
    if not MAPTILER_KEY:
        sys.stderr.write("error: set MAPTILER_KEY env var or include key= in TILE_URL\n")
        sys.exit(2)
    TILE_URL = f"{TILE_URL}?key={MAPTILER_KEY}"
OUT_DIR = Path(__file__).resolve().parent.parent / "webapp/src/main/webapp/assets/tiles"
USER_AGENT = "paw-2026a-14/1.0 (academic project; contact: emigliaro@tropical-it.com)"
SLEEP_SECONDS = 0.1
MAX_RETRIES = 3


def lonlat_to_tile(lon: float, lat: float, z: int) -> tuple[int, int]:
    n = 2 ** z
    x = int((lon + 180.0) / 360.0 * n)
    lat_rad = math.radians(lat)
    y = int((1.0 - math.asinh(math.tan(lat_rad)) / math.pi) / 2.0 * n)
    return x, y


def tile_range(bbox: tuple[float, float, float, float], z: int):
    min_lon, min_lat, max_lon, max_lat = bbox
    x0, y1 = lonlat_to_tile(min_lon, min_lat, z)
    x1, y0 = lonlat_to_tile(max_lon, max_lat, z)
    return range(min(x0, x1), max(x0, x1) + 1), range(min(y0, y1), max(y0, y1) + 1)


def download(url: str, dest: Path) -> bool:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                data = resp.read()
            dest.write_bytes(data)
            return True
        except (urllib.error.URLError, TimeoutError) as e:
            print(f"  attempt {attempt}/{MAX_RETRIES} failed: {e}", file=sys.stderr)
            time.sleep(2 * attempt)
    return False


def main() -> int:
    print(f"Source: {TILE_URL}")
    print(f"Output: {OUT_DIR}")
    plan = [(z, x, y) for z in ZOOMS for x_range, y_range in [tile_range(BBOX, z)]
            for x in x_range for y in y_range]
    print(f"Total tiles: {len(plan)}\n")

    downloaded = skipped = failed = 0
    for i, (z, x, y) in enumerate(plan, 1):
        dest = OUT_DIR / str(z) / str(x) / f"{y}.png"
        if dest.exists() and dest.stat().st_size > 0:
            skipped += 1
            continue
        dest.parent.mkdir(parents=True, exist_ok=True)
        url = TILE_URL.format(z=z, x=x, y=y)
        print(f"[{i}/{len(plan)}] z{z}/{x}/{y}")
        if download(url, dest):
            downloaded += 1
            time.sleep(SLEEP_SECONDS)
        else:
            failed += 1

    print(f"\nDone. downloaded={downloaded} skipped={skipped} failed={failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
