CineMY - Setup and Execution Guide
==================================

This document provides instructions on how to set up the environment and execute the CineMY source code.

1. PREREQUISITES & TOOLS
------------------------

To run this project, you need the following tools installed:

- Android Studio (Ladybug or newer recommended)
  Download: https://developer.android.com/studio
  Version used in development: Android Studio 2026.1.1

- Java Development Kit (JDK) 11 or 17
  (Android Studio usually comes with a bundled JDK which is sufficient)
  Download (if needed): https://www.oracle.com/java/technologies/downloads/

- Android SDK (API Level 36)
  You can install this via the SDK Manager inside Android Studio.

2. PROJECT SPECIFICATIONS
-------------------------

- Minimum SDK: 26 (Android 8.0 Oreo)
- Target SDK: 35
- Compiled SDK: 36
- Gradle Version: 9.4.1
- Android Gradle Plugin (AGP): 9.2.1
- Language: Kotlin

3. LIBRARIES & DEPENDENCIES
---------------------------

The project uses the following major libraries:

- Firebase (Auth, Firestore, BOM 34.13.0)
- AndroidX (Core KTX, AppCompat, ConstraintLayout, Activity KTX, Recyclerview)
- Material Design Components (1.10.0)
- Biometric (1.1.0) - For fingerprint/face unlock
- ZXing (Core 3.5.3, Android Embedded 4.3.0) - For QR code generation/scanning
- WorkManager (2.10.0) - For background tasks

4. SETUP INSTRUCTIONS
---------------------

Step 1: Clone or Download the Source Code
   Ensure you have the full project folder on your local machine.

Step 2: Open the Project in Android Studio
   - Launch Android Studio.
   - Select "Open" and navigate to the "CineMY" root folder.
   - Wait for Gradle to finish syncing (this may take several minutes as it downloads the libraries).

Step 3: Firebase Configuration (Crucial)
   - The project requires a 'google-services.json' file to connect to Firebase.
   - If this file is missing, you must create a project in the Firebase Console (https://console.firebase.google.com/).
   - Add an Android App with package name 'com.fypcinemy'.
   - Download 'google-services.json' and place it in the 'CineMY/app/' directory.
   - Ensure Firestore and Authentication (Email/Password) are enabled in your Firebase console.

Step 4: Connect a Device or Emulator
   - Connect a physical Android device via USB (with Developer Options and USB Debugging enabled).
   - OR, create a Virtual Device (Emulator) via the Device Manager in Android Studio (Pixel 6/7 recommended with API 35 or 36).

5. EXECUTION
------------

- Click the green "Run" button (Play icon) in the top toolbar or press 'Shift + F10'.
- Select your device/emulator from the dropdown list.
- The app will build, install, and launch on your device.

6. TROUBLESHOOTING
------------------

- Gradle Sync Failed: Ensure you have an active internet connection to download dependencies. Try "File > Invalidate Caches / Restart".
- Missing SDK: Go to "Tools > SDK Manager" and ensure Android SDK 36 is installed.
- Firebase Errors: Double-check that your 'google-services.json' is correctly placed and matches your Firebase project configuration.
