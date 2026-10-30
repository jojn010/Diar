# Diar — a home for every story

A native Android presentation app with English/Arabic UI and device-local property listings. Open the repository root in Android Studio. Choose **Get started → Explore homes** to browse without registration.

## List a home

Choose **List your home** from Home, Listings or Account. The animated composer has three steps:

1. **Meet your home:** title, sale or rent, apartment / house with a backyard / villa, one / two / three-or-more floors, and area in square metres.
2. **Find its place:** use the device’s current location or open a small interactive map and tap a pin. Add a city or neighborhood name. The app requests approximate foreground location permission only after tapping **Use my location**. Permission denial or disabled/unavailable location services leave the map option available.
3. **Price it. Picture it.:** enter one whole-number USD price (monthly for rent), optionally check **Price is negotiable**, choose a photo, add a description, review and save.

Prices accept English, Arabic and Persian digits from 1 to one billion; fractional prices are rejected. Area may have up to two decimal places. There are no minimum/maximum price controls.

Choose **View my listings** to see only your own saved homes. Listings also appear on Home and Listings and in Search. Open a listing to see its purpose, type, floors, price, negotiability, area, location and photo. You can replace its photo, adjust its map pin, or delete it with confirmation.

## Local storage and photos

There is no backend or database service. Property records are JSON in app-private preferences; selected photos are resized, camera orientation is applied, and JPEG copies are saved in app-private files. Original picker permissions are not needed to view saved photos later. Listings survive restarts and screen recreation. Older 1.1 listings remain readable without invented floor/type/location details. Device-local listing records and photos are excluded from Android cloud backup and device transfer. Uninstalling the app removes them.

A clearly labelled sample photo appears until a personal photo is selected. New listings are not published online. Authentication, social sign-in, checkout and remote messaging remain demonstrations; checkout does not charge or store card details. The original sample map screen remains an illustration.

## Map picker

The actual listing map picker uses bundled Leaflet 1.9.4 with standard OpenStreetMap tiles and visible attribution. Pan, zoom and tap to select a pin. **Map tiles need internet**; a loading/error message and latitude/longitude fields provide a fallback when tiles cannot load. Opening the map does not publish listing data or photos. The map provider receives the normal requests needed to display map tiles. Coordinates are saved only when confirmed. Current device location may be approximate and can be refined using the map.

Leaflet’s license is in `app/src/main/assets/LEAFLET-LICENSE.txt`. Map integration follows the [Leaflet guide](https://leafletjs.com/examples/quick-start/) and [OpenStreetMap tile policy](https://operations.osmfoundation.org/policies/tiles/).

## Presentation and languages

Cream, teal, sage and gold styling, bundled property photography and Diar-branded adaptive/themed launcher icons are included. Screen entrances and composer steps use fade/lift animations; system animator preferences are respected. Account offers optional gentle save feedback, off by default.

Use **Language / العربية** on the start, welcome or account screen. The choice persists. Arabic uses RTL layouts with mirrored back arrows. New composer text, errors, choices and listing metadata are localized. Base-map place labels come from the tile provider.

## Develop and verify

This checkout is `/home/jojn10/Documents/ChatGPT/diar`. Android SDK configuration belongs in untracked `local.properties`.

Run `./tools/check_app.sh` to build the debug APK, run unit tests, lint, and run instrumented tests on a connected `emulator-*` device. The script explicitly selects an emulator and reports skipped instrumented tests if none is running. It does not install onto a physical phone. To select a dedicated test emulator, run `ANDROID_SERIAL=emulator-5556 ./tools/check_app.sh`; physical-device serials are rejected.

Emulator coverage includes English/Arabic screen navigation and RTL, the three-step composer, integer price validation, negotiability, sale/rent, all property and floor choices, map pin selection, current location with a mock GPS fix, recreation, listing search/deletion, migration of older listings and locally imported camera-photo orientation.

Layouts live in `app/src/main/res/layout`; shared colors/styles in `res/values`; translations in `res/values-ar`. The old `tools/generate_layouts.py` creates the initial reference scaffold and **overwrites edits**, so it is not part of the build and should not be rerun to update this composer.

The GitHub preview APK is debug-signed for installation and demonstration, not a Play Store production build. Production signing credentials are not included.

Bundled property photos: Unsplash IDs `photo-1600596542815-ffad4c1539a9`, `photo-1600585154340-be6161a56a0c`, `photo-1600047509807-ba8f99d2cdde`.
