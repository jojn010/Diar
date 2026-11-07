# Diar — a home for every story

A native Android app with English/Arabic UI, Supabase accounts, shared property listings and photo storage when configured. Open the repository root in Android Studio. Choose **Get started → Explore homes** to browse without registration.

## List a home

Choose **List your home** from Home, Listings or Account. The animated composer has three steps:

1. **Meet your home:** title, sale or rent, apartment / house with a backyard / villa, one / two / three-or-more floors, and area in square metres.
2. **Find its place:** use the device’s current location or open a small interactive map and tap a pin. Add a city or neighborhood name. The app requests approximate foreground location permission only after tapping **Use my location**. Permission denial or disabled/unavailable location services leave the map option available.
3. **Price it. Picture it.:** enter one whole-number USD price (monthly for rent), optionally check **Price is negotiable**, choose a photo, add a description, review and save.

Prices accept English, Arabic and Persian digits from 1 to one billion; fractional prices are rejected. Area may have up to two decimal places. There are no minimum/maximum price controls.

Choose **View my listings** to see only your own saved homes. Listings also appear on Home and Listings and in Search. Open a listing to see its purpose, type, floors, price, negotiability, area, location and photo. You can replace its photo, adjust its map pin, or delete it with confirmation.

## Supabase setup and existing local homes

The configured app uses Supabase Auth (email/password), PostgreSQL listings and a public `listing-photos` Storage bucket. Published homes appear on other devices after refreshing. Only owners can change/delete listings; server-side row-level security enforces ownership. All chosen photos are resized, camera orientation is applied, and JPEGs are uploaded. Publication explicitly warns that details, map coordinates and the photo become public.

1. Run the SQL files in `supabase/migrations` in filename order in your Supabase SQL Editor, once per project. Never disable RLS to fix an error.
2. Add these lines to untracked `local.properties` alongside `sdk.dir`:
   ```properties
   supabase.url=https://YOUR_PROJECT_REF.supabase.co
   supabase.publishableKey=YOUR_PUBLISHABLE_OR_LEGACY_ANON_KEY
   ```
3. Sync Android Studio and run the `app` configuration. The publishable key is intended for mobile clients; **never** use a secret, service-role key or database password in the app.
4. Create an account, confirm your email, then sign in. Hosted Supabase email confirmation is supported; confirm the email and return to the app to sign in. Configure your project's email delivery and confirmation destination before inviting a pilot group. OAuth, password recovery and account deletion are not implemented yet.

Access/refresh tokens are encrypted with Android Keystore. Passwords are never persisted. Public listing metadata is cached privately for browsing; online saves require a successful server response. There is no offline write queue or realtime subscription: use **Refresh homes** for changes from another device. This initial implementation fetches listings in pages and displays the loaded collection; server-side search/pagination should precede large-scale use.

Existing device-local homes/photos are preserved and **never automatically uploaded**. Open one and choose **Publish this local home**. Incomplete older listings need a new listing with the required fields. Successfully migrated listings use the cloud as their source of truth. Local homes, cached metadata, session data and private photos are excluded from Android backup/device transfer. Uninstalling removes private local homes; published homes remain in Supabase.

Without Supabase settings, or with `-PdiarOffline=true`, the app runs the original device-local prototype. Leaving the composer discards its unsaved photo; step changes and recreation retain the draft. Photos referenced by saved local homes are protected.

Social sign-in, checkout, remote messaging, notifications and the original sample homes/map remain demonstrations. Checkout does not charge or store card details. This is a development preview, not a production-ready marketplace.

All sample and real property cards use `item_property_card.xml`, including the same rounded image, sage information panel and overlaid heart action. Shared styles keep their typography and spacing consistent in English and Arabic. Demo cards are labelled; real cards show actual listing metadata without fabricated ratings.

## Favorites and listing actions

Real local/published homes have a heart action on their cards and details. Favorites shows the same home cards and opens the same detail view, with remove controls. Saved IDs persist privately on this device and are separated by signed-in account (and guest mode); they do not sync through Supabase yet. A missing/deleted listing is shown as unavailable, without retaining a second copy of its details. Refresh checks the latest loaded homes.

All real listing detail views offer sharing through the Android share sheet and opening the confirmed pin in an installed maps app. Photo, price and attributes use the same visual style for owners and visitors. Only owners see photo replacement, pin editing and deletion. Sharing sends listing text, not an invented public web link. Sample-home tour/chat/payment actions remain demonstrations and are not represented as working actions on real listings.

## Map picker

The actual listing map picker uses bundled Leaflet 1.9.4 with standard OpenStreetMap tiles and visible attribution. Pan, zoom and tap to select a pin. **Map tiles need internet**; a loading/error message and latitude/longitude fields provide a fallback when tiles cannot load. Opening the map does not publish listing data or photos. The map provider receives the normal requests needed to display map tiles. Coordinates are applied to the form only when confirmed and are uploaded only when the home is published or its published pin is updated. Current device location may be approximate and can be refined using the map.

Leaflet’s license is in `app/src/main/assets/LEAFLET-LICENSE.txt`. Map integration follows the [Leaflet guide](https://leafletjs.com/examples/quick-start/) and [OpenStreetMap tile policy](https://operations.osmfoundation.org/policies/tiles/).

## Presentation and languages

Cream, teal, sage and gold styling, bundled property photography and Diar-branded adaptive/themed launcher icons are included. Screen entrances and composer steps use fade/lift animations; system animator preferences are respected. Account offers optional gentle save feedback, off by default.

Use **Language / العربية** on the start, welcome or account screen. The choice persists. Arabic uses RTL layouts with mirrored back arrows. New composer text, errors, choices and listing metadata are localized. Base-map place labels come from the tile provider.

## Develop and verify

This checkout is `/home/jojn10/Documents/ChatGPT/diar`. Android SDK configuration belongs in untracked `local.properties`.

Run `./tools/check_app.sh` to build the debug APK, run unit tests, lint, and run instrumented tests on a connected `emulator-*` device. The script builds with `-PdiarOffline=true` so tests cannot publish or delete live data. It explicitly selects an emulator and reports skipped instrumented tests if none is running. It does not install onto a physical phone. To select a dedicated test emulator, run `ANDROID_SERIAL=emulator-5556 ./tools/check_app.sh`; physical-device serials are rejected.

Emulator coverage includes English/Arabic screen navigation and RTL, the three-step composer, integer price validation, negotiability, sale/rent, all property and floor choices, map pin selection, current location with a mock GPS fix, recreation, listing search/deletion, migration of older listings and locally imported camera-photo orientation.

Additional fake-transport emulator tests cover encrypted session restoration, token refresh, owner checks, email-confirmation signup, failed-upload cleanup, offline logout and cloud-cache serialization. To run the configured **read-only** cloud smoke test (English/Arabic login, RTL and listing API) on an emulator:

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.diar.CloudUiSmokeTest
```

Do not run the legacy local-mutation tests against a configured live project. Rebuild normally with `./gradlew assembleDebug` after offline checks before using/publishing the connected APK. Live REST checks with two disposable confirmed QA users verified photo upload/removal, listing create/edit/delete, public reads, and server rejection of cross-owner writes/deletes and forged ownership. The accounts and test data were removed afterward. The configured Android smoke test verifies live reads and bilingual login/RTL. A signed-in Android publish walkthrough remains to be completed; the separate opt-in `SupabaseLiveTest` accepts disposable QA credentials through instrumentation arguments. The default suite never creates real users or sends confirmation emails.

Layouts live in `app/src/main/res/layout`; shared colors/styles in `res/values`; translations in `res/values-ar`. The old `tools/generate_layouts.py` creates the initial reference scaffold and **overwrites edits**, so it is not part of the build and should not be rerun to update this composer.

The GitHub preview APK is debug-signed for installation and demonstration, not a Play Store production build. Production signing credentials are not included.

Bundled property photos: Unsplash IDs `photo-1600596542815-ffad4c1539a9`, `photo-1600585154340-be6161a56a0c`, `photo-1600047509807-ba8f99d2cdde`.
