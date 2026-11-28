"""Renders the Google Play graphics into the app listings (Arabic is the default listing, English the second).

usage: python play_graphics.py   (needs Chrome, Pillow and an internet connection for the Google Fonts)
Writes listings/<ar|en-US>/graphics/: icon/1.png (512 px), feature-graphic/1.png (1024 × 500) and
phone-screenshots/1-4.png (1080 × 1920, from the app screenshots in play/shots/).
"""
import shutil
from pathlib import Path

from PIL import Image

from export import chrome, url

BRAND = Path(__file__).resolve().parent
LISTINGS = BRAND.parent.parent / "android" / "app" / "src" / "fdroid" / "play" / "listings"
ICON = BRAND.parent / "website" / "icon-512.png"
SCREENS = 4


def render(source: str, out: Path, w: int, h: int) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    chrome(f"--window-size={w},{h}", "--force-device-scale-factor=1", f"--screenshot={out}", url(source))
    # Play wants the feature graphic and screenshots without transparency.
    Image.open(out).convert("RGB").save(out)
    print(out.relative_to(LISTINGS))


def main() -> None:
    for locale, lang in (("ar", "ar"), ("en-US", "en")):
        graphics = LISTINGS / locale / "graphics"
        # The upstream Organic Maps pictures are replaced, not mixed with ours.
        for old in ("phone-screenshots", "tablet-screenshots", "feature-graphic", "icon"):
            shutil.rmtree(graphics / old, ignore_errors=True)
        (graphics / "icon").mkdir(parents=True)
        shutil.copy(ICON, graphics / "icon" / "1.png")
        render(f"designs/feature-graphic.html?lang={lang}", graphics / "feature-graphic" / "1.png", 1024, 500)
        for n in range(1, SCREENS + 1):
            render(f"designs/play-screenshot.html?n={n}&lang={lang}", graphics / "phone-screenshots" / f"{n}.png",
                   1080, 1920)


if __name__ == "__main__":
    main()
