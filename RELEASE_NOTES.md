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
