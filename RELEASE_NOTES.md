# Diar 1.2.1 — Clean up abandoned listing photos

Leaving an unfinished listing through Back or navigation now deletes its unsaved photo instead of leaving unused images in private storage. Finishing the composer activity also discards the unsaved photo. Replacing a draft photo removes the previous unused selection.

Photos remain intact when moving between composer steps or recreating the activity, and cleanup never deletes a photo referenced by a saved listing. Listings and photos remain device-local; no backend was added.

The APK is a debug-signed preview. No physical phone is updated by this scheduled run.

Validation: debug build, five unit tests and Android lint passed. All ten
instrumented tests passed on the Android 14 emulator, including three new
photo-lifecycle regressions and the existing English/Arabic, RTL, map, location,
listing persistence, search, deletion and photo import tests.

---

# Diar 1.2.0 — Tell your home’s story

- Replace the long listing form with three animated steps and a progress indicator.
- Choose sale or rent, apartment / house with a backyard / villa, and one / two / three-or-more floors.
- Enter one whole-number USD price and optionally check **Price is negotiable**. Rental prices are clearly monthly. No min/max controls.
- Choose the device’s current location with an on-demand permission request, or select another place on an interactive map. Save and refine the pin; coordinate entry remains available if map tiles cannot load.
- Add **My listings**, accessible from Home, Listings and Account.
- Save or replace a real property photo locally; resize and apply camera orientation.
- Keep existing listings readable, restore new drafts after recreation, and exclude local listing records/photos from Android backup and device transfer.
- Translate all new controls, validation and details into English and Arabic with RTL support.

All listing records and photos remain on this device. There is no backend/database or online publication. Map tiles require internet and use OpenStreetMap with bundled Leaflet. APK is debug-signed for preview use.

Validation: debug build succeeded; 5 unit tests and all 7 instrumented tests passed
on an isolated Android 14 emulator. Android lint completed with no errors.
English/Arabic composer and map screenshots were visually reviewed.

---

# Diar 1.1.1 — Branded launcher icon

Replace the default Android robot with a Diar monogram inspired by the existing brand: teal dome, doorway and olive branch on cream.

Includes adaptive icons, Android themed-icon support, and standard/round legacy icons at all launcher densities. Retains the existing in-app Diar wordmark and all 1.1 listing features.

Validation: debug APK builds successfully; icon artwork reviewed visually. This APK is a debug-signed offline preview. Listings stay on the device.

---

# Diar 1.1.0 — Homes with a story

- Create a home listing with a title, area (m²), asking price (USD), location and optional description.
- Choose a personal property photo; resized photos are stored privately on the device.
- Browse your saved properties on Home and Listings, search for them, view details and delete with confirmation.
- Keep saved listings across app restarts and screen recreation.
- English and Arabic UI, RTL layout and Arabic/Persian number entry.
- Fade/lift transitions, staggered screen reveals and animated favorite feedback, respecting system animation settings.
- Optional gentle sound when saving, controlled from Account and disabled by default.
- Correct sample-property search so Ramallah no longer incorrectly matches Jerusalem properties.
- Add number-validation and bilingual emulator regression coverage for creating, restoring, searching and deleting listings.

This is an offline presentation prototype. New listings stay on the device.
Authentication, payments, map services and remote messaging are still demonstrations.
The attached APK is debug-signed and intended for preview installations.

Validation: debug APK build, unit tests and Android lint completed successfully.
All 4 instrumented tests passed on the Pixel 7 emulator (Android 14), including
English/Arabic screen navigation and listing creation, validation, recreation,
search and confirmed deletion. Three unit tests passed. Lint reports existing
warnings with no errors. English/Arabic listing screenshots were visually reviewed.
