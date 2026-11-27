"""Draws the proposed road report icons (64 x 64 SVG, road sign style) into Syria/brand/road-reports/.

Usage: python Syria/brand/road_report_icons.py [preview.html]
The optional preview page shows every icon large, small and on a dark background.
"""
import os
import sys

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "road-reports")
RED, INK, BLUE, YEL, GREY, GREEN, WHITE = "#C1272D", "#21242A", "#1F5FAD", "#FFC20E", "#9AA3B0", "#2E7D32", "#FFFFFF"


def svg(body, title):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64">'
            f'<title>{title}</title>{body}</svg>\n')


def triangle(fill=WHITE, stroke=RED):
    return (f'<path d="M32 8 58.5 54H5.5Z" fill="{fill}" stroke="{stroke}" stroke-width="6" '
            f'stroke-linejoin="round"/>')


def ring(fill=WHITE):
    return f'<circle cx="32" cy="32" r="26" fill="{fill}" stroke="{RED}" stroke-width="7"/>'


def blue_square():
    return f'<rect x="4" y="4" width="56" height="56" rx="10" fill="{BLUE}"/>'


def car_rear(cx, by, w):
    """A car seen from behind, with a white outline that separates it from the cars around it."""
    h = w * 0.62
    s = f'stroke="{WHITE}" stroke-width="1.4" paint-order="stroke" stroke-linejoin="round"'
    return (f'<g fill="{INK}" {s}>'
            f'<path d="M{cx - w * .32} {by - h * .5}L{cx - w * .22} {by - h}H{cx + w * .22}L{cx + w * .32} {by - h * .5}Z"/>'
            f'<rect x="{cx - w * .43}" y="{by - h * .25}" width="{w * .2}" height="{h * .3}" rx="{w * .05}"/>'
            f'<rect x="{cx + w * .23}" y="{by - h * .25}" width="{w * .2}" height="{h * .3}" rx="{w * .05}"/>'
            f'<rect x="{cx - w / 2}" y="{by - h * .58}" width="{w}" height="{h * .45}" rx="{w * .1}"/>'
            f'</g>')


CAR_SIDE = (f'<path d="M1 9V6.5Q1 5 2.5 5H6L9.5 1H16.5L20 5H22Q23.5 5 23.5 6.5V9Z" fill="{INK}"/>'
            f'<circle cx="6" cy="9.5" r="2.6" fill="{INK}" stroke="{WHITE}" stroke-width="1.2"/>'
            f'<circle cx="18.5" cy="9.5" r="2.6" fill="{INK}" stroke="{WHITE}" stroke-width="1.2"/>')


def badge(color, mark):
    return f'<circle cx="49" cy="49" r="10" fill="{color}" stroke="{WHITE}" stroke-width="2.5"/>' + mark


CHECK = f'<path d="M44.5 49.5 47.8 52.8 54 46" fill="none" stroke="{WHITE}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>'
CROSS = f'<path d="M45.2 45.2 52.8 52.8M52.8 45.2 45.2 52.8" stroke="{WHITE}" stroke-width="3" stroke-linecap="round"/>'


def stripes(x, y, w, h, clip):
    """A red and white striped bar with a charcoal outline."""
    bars = "".join(f'<path d="M{x + i} {y + h}l{h} {-h}h4l{-h} {h}z" fill="{RED}"/>' for i in range(-h, w + h, 8))
    return (f'<clipPath id="{clip}"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="1.5"/></clipPath>'
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="1.5" fill="{WHITE}"/>'
            f'<g clip-path="url(#{clip})">{bars}</g>'
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="1.5" fill="none" stroke="{INK}" stroke-width="1.6"/>')


ICONS = {
    # A white edge keeps the charcoal border visible on the dark map.
    "report-button": ("Report button", f'<path d="M32 8 58.5 54H5.5Z" fill="none" stroke="{WHITE}" stroke-width="9" stroke-linejoin="round"/>'
                      + triangle(YEL, INK)
                      + f'<path d="M32 23V37" stroke="{INK}" stroke-width="6" stroke-linecap="round"/>'
                      + f'<circle cx="32" cy="45.5" r="3.6" fill="{INK}"/>'),
    "traffic-jam": ("Traffic jam", triangle() + car_rear(32, 32, 12) + car_rear(32, 41, 18) + car_rear(32, 51, 25)),
    "closed-road": ("Closed road", f'<circle cx="32" cy="32" r="28" fill="{RED}"/>'
                    + f'<rect x="13" y="27" width="38" height="10" rx="2" fill="{WHITE}"/>'),
    "accident": ("Accident", triangle()
                 + f'<g transform="translate(10.5 41) rotate(-20) scale(.92)">{CAR_SIDE}</g>'
                 + f'<g transform="translate(64 0) scale(-1 1)"><g transform="translate(10.5 41) rotate(-20) scale(.92)">{CAR_SIDE}</g></g>'
                 + f'<path d="M32 22.5 33.6 27.2 37.8 25.6 35.6 29.6 39.4 32 34.6 32.4 32 36 29.4 32.4 24.6 32 28.4 29.6 26.2 25.6 30.4 27.2Z" fill="{INK}"/>'),
    "road-works": ("Road works", triangle()
                   + f'<g stroke="{INK}" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round" fill="none">'
                     f'<path d="M26.5 33 23.5 41.5M23.5 41.5 19.5 49M23.5 41.5 27 49M25.5 35 31.5 38.5"/>'
                     f'<path d="M29 31.5 36.5 45.5" stroke-width="2.4"/></g>'
                   + f'<path d="M34 45.5 38.5 43.5 40 47.5Z" fill="{INK}"/>'
                   + f'<circle cx="27.5" cy="29" r="3" fill="{INK}"/>'
                   + f'<path d="M37 49.5Q41.5 40 49 49.5Z" fill="{INK}"/>'),
    "hazard": ("Hazard on the road", triangle()
               + f'<path d="M15.5 49.5H48.5" stroke="{INK}" stroke-width="2.6" stroke-linecap="round"/>'
               + f'<path d="M20 49 21.5 42 27 36.5 33.5 37.5 37.5 43 38 49Z" fill="{INK}"/>'
               + f'<path d="M39.5 49 40.5 45 44 43.5 46.5 46.5 46.5 49Z" fill="{INK}"/>'),
    "bad-surface": ("Bad road surface", triangle()
                    + f'<path d="M14 50.5V46H18.5Q20.5 46 21.5 42.5Q23.5 35.5 25.5 42.5Q26.5 46 28.5 46H35.5Q37.5 46 38.5 42.5'
                      f'Q40.5 35.5 42.5 42.5Q43.5 46 45.5 46H50V50.5Z" fill="{INK}"/>'),
    "weather": ("Flooding, snow or ice", triangle()
                + f'<g transform="rotate(-12 32 30)">{car_rear(32, 34, 15)}</g>'
                + f'<path d="M27 36.5C21 41 33 44 25.5 50.5M37 36.5C31 41 43 44 35.5 50.5" fill="none" stroke="{INK}" '
                  f'stroke-width="2.4" stroke-linecap="round"/>'),
    "checkpoint": ("Checkpoint", ring()
                   + f'<rect x="15" y="25" width="6" height="21" rx="1.5" fill="{INK}"/>'
                   + f'<path d="M12.5 46H23.5" stroke="{INK}" stroke-width="3" stroke-linecap="round"/>'
                   + stripes(19, 27.5, 30, 7, "boom")),
    "traffic-light": ("Traffic light not working", triangle()
                      + f'<rect x="26.5" y="25" width="11" height="25" rx="3" fill="{INK}"/>'
                      + "".join(f'<circle cx="32" cy="{y}" r="3" fill="{GREY}"/>' for y in (30.5, 37.5, 44.5))),
    "fuel-available": ("Fuel available", blue_square()
                       + f'<rect x="15" y="14" width="18" height="33" rx="2.5" fill="{WHITE}"/>'
                       + f'<rect x="18.5" y="18" width="11" height="9" rx="1" fill="{BLUE}"/>'
                       + f'<rect x="12" y="46" width="24" height="4" rx="1" fill="{WHITE}"/>'
                       + f'<path d="M33 23H36.5Q39 23 39 25.5V36Q39 39 41.5 39Q44 39 44 36V22L40.5 18" fill="none" '
                         f'stroke="{WHITE}" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/>'
                       + badge(GREEN, CHECK)),
    "fuel-unavailable": ("No fuel", None),
    "map-error": ("Map error", blue_square()
                  + f'<path d="M11 19 22 15 32 19 43 15V43L32 47 22 43 11 47Z" fill="{WHITE}"/>'
                  + f'<path d="M22 15V43M32 19V47" stroke="{BLUE}" stroke-width="1.6"/>'
                  + f'<g transform="rotate(45 44 38)"><rect x="40" y="22" width="8" height="22" fill="{YEL}" stroke="{INK}" '
                    f'stroke-width="1.6"/><path d="M40 44 44 51 48 44Z" fill="{WHITE}" stroke="{INK}" stroke-width="1.6" '
                    f'stroke-linejoin="round"/><path d="M42.6 48.6 44 51 45.4 48.6Z" fill="{INK}"/></g>'),
    "wrong-route": ("Wrong route or turn instruction", f'<circle cx="32" cy="32" r="28" fill="{BLUE}"/>'
                    + f'<path d="M24 49V33Q24 26 31 26H38" fill="none" stroke="{WHITE}" stroke-width="6" stroke-linecap="round"/>'
                    + f'<path d="M36 17.5 46.5 26 36 34.5Z" fill="{WHITE}" stroke="{WHITE}" stroke-width="2" stroke-linejoin="round"/>'
                    + badge(RED, CROSS)),
    "app-feedback": ("App problem or suggestion", blue_square()
                     + f'<path d="M16 14H48Q52 14 52 18V38Q52 42 48 42H30L20 51V42H16Q12 42 12 38V18Q12 14 16 14Z" fill="{WHITE}"/>'
                     + f'<circle cx="32" cy="21" r="3" fill="{BLUE}"/>'
                     + f'<path d="M32 27.5V36" stroke="{BLUE}" stroke-width="4.6" stroke-linecap="round"/>'),
}
title, body = ICONS["fuel-available"]
ICONS["fuel-unavailable"] = ("No fuel", body.replace(badge(GREEN, CHECK), badge(RED, CROSS)))

os.makedirs(OUT, exist_ok=True)
for name, (title, body) in ICONS.items():
    with open(os.path.join(OUT, f"{name}.svg"), "w", encoding="utf-8", newline="\n") as f:
        f.write(svg(body, title))

if len(sys.argv) < 2:
    sys.exit(0)
cells = "".join(f'<figure><img src="file:///{OUT.replace(os.sep, "/")}/{n}.svg" width="96"><img src="file:///{OUT.replace(os.sep, "/")}/{n}.svg" width="32">'
                f'<figcaption>{t}</figcaption></figure>' for n, (t, _) in ICONS.items())
with open(sys.argv[2], "w", encoding="utf-8") as f:
    f.write(f'<!doctype html><body style="margin:0;padding:16px;font:13px sans-serif;display:grid;'
            f'grid-template-columns:repeat(5,1fr);gap:12px;background:#f7f5f0">'
            f'<style>figure{{margin:0;text-align:center}}img{{vertical-align:bottom;margin:4px}}</style>{cells}'
            f'<div style="grid-column:1/-1;background:#15171b;padding:12px;color:#fff">dark: '
            + "".join(f'<img src="file:///{OUT.replace(os.sep, "/")}/{n}.svg" width="40">' for n in ICONS) + '</div></body>')
