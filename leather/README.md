# Leather Dark — a second UI direction for Screen Time Reminder

A leather-and-suede take on the app, carrying the JUST APPS material system
(deep teal leather, sand suede, rust, olive, a restrained lime accent) into the
product itself. It is a working prototype, not a screenshot: every screen,
switch, reminder and the break timer behave like the Android app.

Open `index.html` (or the workspace live preview) and use the list on the left
to move between **Today**, **Seven days**, **Settings**, the **break nudge** and
the **first run** screen. The sun button in the header switches between
**Leather Dark** and a **Paper Light** variant; the choice is remembered.

## Palette

| Role | Colour |
| --- | --- |
| Brand, primary | `#00BCC8` cyan teal |
| Accent, secondary | `#D0FF00` electric lime |
| Deep teal leather | `#064B4D` |
| Sand suede | `#D8C2A0` → `#F0E3CC` |
| Rust leather | `#B9572D` |
| Olive suede | `#62653A` |
| Chocolate leather | `#38251E` |
| Charcoal / void | `#171B19` / `#141917` |
| Warm ivory ink | `#F7F2E8` |

Lime never carries a whole surface — it is reserved for the live dot, the
current-hour bar, the active tab dot and the primary action in a reminder.

## What is inside

- `index.html` — layered leather flaps behind an editorial page, with the phone
  as the centrepiece.
- `styles.css` — the material system: organic flap shapes built from layered
  radial and linear gradients with an inline SVG grain, soft lit edges, contact
  shadows, dashed stitching and a dark-leather phone body.
- `app.js` — same logic as `../preview/app.js`: live counter, session engine,
  break timer, daily limit, seven-day history and locally saved settings.

Screen-time figures and app names are simulated in the browser; nothing is
sent anywhere.
