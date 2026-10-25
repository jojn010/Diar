# Diar Android prototype

The 16 screens in the supplied reference have individual `screen_*.xml` layouts in `app/src/main/res/layout`. Four supporting screens provide favorites, an account menu, a conversation and a screen gallery. All screens use native Android Views, with navigation handled by `MainActivity.kt`.

Run the app from Android Studio. Choose **Get started → Explore homes** to browse without filling out a form. **Account → All prototype screens** opens every reference screen directly.

Included: Diar's supplied logo, teal/cream styling, bundled property photographs, navigation/back handling, search filtering and its empty state, persistent saved-home toggle, local conversation messages, validated signup/login forms, and a sample checkout flow.

This is an offline prototype. Authentication and social sign-in are not connected. Messages remain local. Checkout does not charge, submit or store card information. The map is an illustrative vector, not a mapping service. Property names, pricing and notifications are demonstration content.

## Editing

Edit the XML layouts directly in Android Studio. Shared colors and styles live in `res/values`; the supplied logo and property photos live in `res/drawable-nodpi`. `tools/generate_layouts.py` records the initial scaffold; rerunning it overwrites layout/style edits, so it is not part of the build.

The visual system pairs Diar teal with sage, warm cream, peach/terracotta and honey gold. Shared button styles distinguish primary, secondary and outlined actions; the XML includes rounded photography, tinted cards and active navigation icons. The scaffold generator runs `tools/style_layouts.py` automatically to preserve this styling when regenerating from scratch.

Property photos: Unsplash image IDs `photo-1600596542815-ffad4c1539a9`, `photo-1600585154340-be6161a56a0c`, and `photo-1600047509807-ba8f99d2cdde`.

Build with `gradlew.bat assembleDebug`; run checks with `gradlew.bat lintDebug connectedDebugAndroidTest` with an emulator/device connected.
