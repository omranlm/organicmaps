"""Generates the app's icons from the campaign mark (Syria/brand/mark.svg).

Writes into android/libs/branding/src/main/res:
- drawable/logo.xml, drawable-night/logo.xml        mark (About, sign-in, map menu button); dark adds a light outline
- drawable/logo_on_dark.xml                         mark with the light outline, for dark cards in any theme
- drawable/ic_splash_system.xml                    Android 12+ splash screen icon (charcoal splash background)
- drawable/ic_splash.xml                            one-colour pin: notifications, widget, old splash screen
- drawable/ic_launcher_foreground.xml, drawable/ic_launcher_monochrome.xml, values/ic_launcher_background.xml
                                                    adaptive launcher icon (Android 8+, themed icons on 13+)
- mipmap-*/ic_launcher.png, mipmap-*/ic_launcher_round.png   launcher icon before Android 8
and android/app/ic_launcher-playstore.png (512 px).

Vector drawables have no dashed strokes, so the dashes of mark.svg are expanded into segments here. The PNGs are
rendered from the same geometry with headless Chrome.

Usage: python Syria/brand/app_icons.py [--chrome PATH]
"""

import argparse
import math
import os
import subprocess
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RES = os.path.join(ROOT, "android", "libs", "branding", "src", "main", "res")
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

INK, ROAD, TEAL, LEAF, RED, CLOUD, WHITE = "#21242A", "#404E5E", "#0C7C70", "#006C35", "#C1272D", "#F2F2F2", "#FFFFFF"
LAUNCHER_BACKGROUND = INK

# The mark, in mark.svg coordinates (240 x 300).
PIN = "M120,292C120,292 22,196 22,110A98,98 0,0 1,218 110C218,196 120,292 120,292Z"
CURVED_ROAD = ((-10, 52), (40, 50), (92, 70), (112, 112))
STARS = (
    ("M42,8l3.5,10.8h11.3l-9.2,6.7 3.5,10.8 -9.1,-6.7 -9.2,6.7 3.5,-10.8 -9.2,-6.7h11.4z", -12, 42, 22),
    ("M70,30l2.8,8.6h9l-7.3,5.3 2.8,8.6 -7.3,-5.3 -7.3,5.3 2.8,-8.6 -7.3,-5.3h9z", -8, 70, 41),
    ("M92,52l2.1,6.5h6.8l-5.5,4 2.1,6.5 -5.5,-4 -5.5,4 2.1,-6.5 -5.5,-4h6.8z", 0, 0, 0),
)
# Extent of the mark with the dark variant's outline (stroke 10 around the pin) and the stars.
BOX = (12, 0, 228, 298)


def circle(cx, cy, r):
    return f"M{cx - r},{cy}A{r},{r} 0,1 1,{cx + r},{cy}A{r},{r} 0,1 1,{cx - r},{cy}Z"


def bezier(p, t):
    u = 1 - t
    return tuple(u**3 * a + 3 * u * u * t * b + 3 * u * t * t * c + t**3 * d for a, b, c, d in zip(*p))


def dashes_along(points, dash, gap):
    """Dashes [dash, gap, dash, ...] along a polyline, as one path of separate segments."""
    out, pos, on, left = [], points[0], True, dash
    current = [pos]
    for nxt in points[1:]:
        seg = math.dist(pos, nxt)
        while seg >= left:
            k = left / seg
            pos = (pos[0] + (nxt[0] - pos[0]) * k, pos[1] + (nxt[1] - pos[1]) * k)
            seg -= left
            if on:
                current.append(pos)
                out.append(current)
            current = [pos]
            on = not on
            left = dash if on else gap
        left -= seg
        pos = nxt
        if on:
            current.append(pos)
    if on and len(current) > 1:
        out.append(current)
    return "".join("M" + "L".join(f"{x:.2f},{y:.2f}" for x, y in d) for d in out)


def mark(dark):
    """(clipped to the pin, on top) lists of (path, fill, stroke, width, round cap); stars come with a rotation."""
    curve = [bezier(CURVED_ROAD, i / 200) for i in range(201)]
    curve_path = "M-10,52C40,50 92,70 112,112"
    clipped = [
        ("M0,0H120V300H0Z", INK, None, 0, False),
        ("M120,0H240V300H120Z", TEAL, None, 0, False),
        (curve_path, None, ROAD, 22, False),
        (dashes_along(curve, 10, 9), None, CLOUD, 4, False),
        ("M120,0V112", None, ROAD, 22, False),
        (dashes_along([(120, 4), (120, 100)], 10, 9), None, CLOUD, 4, False),
        ("M120,130V300", None, ROAD, 22, False),
        ("M120,140V300", None, WHITE, 8, False),
        ("M144,190c5,-26 28,-40 52,-38 -3,24 -24,41 -52,38z", LEAF, None, 0, False),
        ("M149,186c14,-11 27,-21 41,-30", None, TEAL, 3, True),
    ]
    top = [(PIN, None, CLOUD, 10, False)] if dark else []
    top += [
        (circle(120, 124, 36), ROAD, None, 0, False),
        (circle(120, 124, 28), WHITE, None, 0, False),
        (circle(120, 124, 17), RED, None, 0, False),
    ]
    return clipped, top


def mono():
    """One-colour pin: the roundabout ring and the road to the tip are cut out (even-odd), plus the stars."""
    pin = PIN + circle(120, 124, 28) + circle(120, 124, 17) + "M116,152H124V272H116Z"
    return [(pin, "evenOdd")]


def frame(size, height, dy=0.0):
    """Scale and offset that put the mark's box, [height] tall, in the middle of a [size] square."""
    x0, y0, x1, y1 = BOX
    s = height / (y1 - y0)
    tx = (size - (x1 - x0) * s) / 2 - x0 * s
    ty = (size - (y1 - y0) * s) / 2 - y0 * s + dy
    return s, tx, ty


# --- Vector drawables ---


def vd_path(d, fill, stroke, width, cap, fill_type=None, color=None):
    attrs = [f'android:pathData="{d}"']
    if fill or color:
        attrs.append(f'android:fillColor="{color or fill}"')
    if fill_type:
        attrs.append(f'android:fillType="{fill_type}"')
    if stroke:
        attrs += [f'android:strokeColor="{stroke}"', f'android:strokeWidth="{width}"']
        attrs.append(f'android:strokeLineCap="{"round" if cap else "butt"}"')
    return "<path " + " ".join(attrs) + " />"


def vector(size_dp, viewport, s, tx, ty, body, comment, tint=None):
    tint_attr = f'\n    android:tint="{tint}"' if tint else ""
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- {comment} Generated by Syria/brand/app_icons.py from the campaign mark: edit that script, not this file. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{size_dp}dp"
    android:height="{size_dp}dp"
    android:viewportWidth="{viewport}"
    android:viewportHeight="{viewport}"{tint_attr}>
  <group
      android:scaleX="{s:.5f}"
      android:scaleY="{s:.5f}"
      android:translateX="{tx:.4f}"
      android:translateY="{ty:.4f}">
{body}
  </group>
</vector>
"""


def color_body(dark):
    clipped, top = mark(dark)
    lines = ["    <group>", f'      <clip-path android:pathData="{PIN}" />']
    lines += ["      " + vd_path(*p) for p in clipped]
    lines.append("    </group>")
    lines += ["    " + vd_path(*p) for p in top]
    lines += stars_vd(RED)
    return "\n".join(lines)


def stars_vd(color):
    out = []
    for d, rot, px, py in STARS:
        if rot:
            out.append(f'    <group android:rotation="{rot}" android:pivotX="{px}" android:pivotY="{py}">')
            out.append("      " + vd_path(d, color, None, 0, False))
            out.append("    </group>")
        else:
            out.append("    " + vd_path(d, color, None, 0, False))
    return out


def mono_body(color):
    lines = ["    " + vd_path(d, None, None, 0, False, fill_type=ft, color=color) for d, ft in mono()]
    return "\n".join(lines + stars_vd(color))


# --- SVG for the PNGs ---


def svg_body(dark):
    clipped, top = mark(dark)

    def el(d, fill, stroke, width, cap):
        a = f'd="{d}" fill="{fill or "none"}"'
        if stroke:
            a += f' stroke="{stroke}" stroke-width="{width}" stroke-linecap="{"round" if cap else "butt"}"'
        return f"<path {a}/>"

    stars = "".join(
        f'<path d="{d}" fill="{RED}" transform="rotate({rot} {px} {py})"/>' for d, rot, px, py in STARS
    )
    return (
        f'<defs><clipPath id="pin"><path d="{PIN}"/></clipPath></defs>'
        + '<g clip-path="url(#pin)">' + "".join(el(*p) for p in clipped) + "</g>"
        + "".join(el(*p) for p in top) + stars
    )


def launcher_svg(px, shape):
    """Legacy launcher icon: the dark mark on a charcoal tile (rounded square, circle or full square)."""
    if shape == "round":
        tile = f'<circle cx="{px / 2}" cy="{px / 2}" r="{px * 0.46}" fill="{LAUNCHER_BACKGROUND}"/>'
        s, tx, ty = frame(px, px * 0.62)
    elif shape == "square":
        tile = f'<rect width="{px}" height="{px}" fill="{LAUNCHER_BACKGROUND}"/>'
        s, tx, ty = frame(px, px * 0.68)
    else:
        m = px * 0.04
        tile = (f'<rect x="{m}" y="{m}" width="{px - 2 * m}" height="{px - 2 * m}" rx="{px * 0.18}" '
                f'fill="{LAUNCHER_BACKGROUND}"/>')
        s, tx, ty = frame(px, px * 0.68)
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{px}" height="{px}" viewBox="0 0 {px} {px}">{tile}'
            f'<g transform="translate({tx:.3f} {ty:.3f}) scale({s:.5f})">{svg_body(True)}</g></svg>')


def render_png(svg, px, out, chrome):
    with tempfile.TemporaryDirectory() as tmp:
        page = os.path.join(tmp, "icon.html")
        with open(page, "w", encoding="utf-8") as f:
            f.write(f"<!doctype html><html><body style='margin:0;background:transparent'>{svg}</body></html>")
        subprocess.run(
            [chrome, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=1",
             "--default-background-color=00000000", f"--window-size={px},{px}", f"--screenshot={out}",
             f"--user-data-dir={os.path.join(tmp, 'profile')}", "file:///" + page.replace("\\", "/")],
            check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    print("wrote", os.path.relpath(path, ROOT))


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--chrome", default=CHROME)
    args = parser.parse_args()

    # Logo: the mark in a square, as tall as the square.
    s, tx, ty = frame(300, 300)
    write(os.path.join(RES, "drawable", "logo.xml"),
          vector(24, 300, s, tx, ty, color_body(False), "Syria Organic Maps mark."))
    write(os.path.join(RES, "drawable-night", "logo.xml"),
          vector(24, 300, s, tx, ty, color_body(True), "Syria Organic Maps mark, light outline for dark themes."))
    write(os.path.join(RES, "drawable", "logo_on_dark.xml"),
          vector(24, 300, s, tx, ty, color_body(True), "Syria Organic Maps mark on a dark background (any theme)."))

    # Android 12+ splash, on the charcoal splash background in both themes: the system shows only a circle in the
    # middle of the icon (about 0.46 of its width on a Pixel 5), so the whole pin, stars included, stays inside it.
    s, tx, ty = frame(480, 185)
    write(os.path.join(RES, "drawable", "ic_splash_system.xml"),
          vector(240, 480, s, tx, ty, color_body(True), "Splash screen icon (Android 12+)."))

    # One-colour pin for notifications (the system uses only its shape), the widget and the old splash screen.
    s, tx, ty = frame(300, 288)
    write(os.path.join(RES, "drawable", "ic_splash.xml"),
          vector(60, 300, s, tx, ty, mono_body(WHITE), "One-colour Syria Organic Maps pin."))

    # Adaptive launcher icon: 108 dp layers, visible content kept inside the 66 dp safe circle.
    s, tx, ty = frame(108, 58)
    write(os.path.join(RES, "drawable", "ic_launcher_foreground.xml"),
          vector(108, 108, s, tx, ty, color_body(True), "Launcher icon foreground."))
    write(os.path.join(RES, "drawable", "ic_launcher_monochrome.xml"),
          vector(108, 108, s, tx, ty, mono_body(WHITE), "Launcher icon for themed icons (Android 13+)."))
    write(os.path.join(RES, "values", "ic_launcher_background.xml"),
          f'<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated by Syria/brand/app_icons.py. -->\n'
          f'<resources>\n    <color name="ic_launcher_background">{LAUNCHER_BACKGROUND}</color>\n</resources>\n')

    for density, scale in (("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4)):
        px = round(48 * scale)
        folder = os.path.join(RES, f"mipmap-{density}")
        for name in ("ic_launcher_foreground.png", "ic_launcher_monochrome.png"):  # now vector drawables
            if os.path.exists(os.path.join(folder, name)):
                os.remove(os.path.join(folder, name))
        render_png(launcher_svg(px, "rounded"), px, os.path.join(folder, "ic_launcher.png"), args.chrome)
        render_png(launcher_svg(px, "round"), px, os.path.join(folder, "ic_launcher_round.png"), args.chrome)
        print("rendered", density, px)
    render_png(launcher_svg(512, "square"), 512, os.path.join(ROOT, "android", "app", "ic_launcher-playstore.png"),
               args.chrome)
    print("rendered Play Store icon")


if __name__ == "__main__":
    main()
