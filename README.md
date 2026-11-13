# SuggestBox — Digital Suggestion Box for Local Governance

A production-quality Kotlin & Jetpack Compose Android application designed for **Gram Panchayats, Municipalities, and Urban Wards**. Citizens submit geotagged infrastructure grievances (*Fix-It*) and participatory civic proposals (*Build-It*), while local governance officials triage, assign departments, enforce SLAs, broadcast ward announcements, and verify resolution proof.

---

## 1. Tech Stack & Architecture

- **Language & UI:** Kotlin, Jetpack Compose (Single-Activity, Navigation Compose with `SharedTransitionLayout`, Material 3 + custom Apple HIG-inspired `:core:designsystem`)
- **Architecture:** Clean Architecture (`domain` / `data` / `ui`) + MVVM + Hilt DI
- **Async & Streams:** Kotlin Coroutines + `StateFlow` / `Flow`
- **Offline-First & Security:** Room (`SuggestBoxDatabase`) for offline drafts, `EncryptedSharedPreferences` for DPDP consent/locale/theme settings, client-side `ImageCompressor`, rate-limiting & profanity screening (`SuggestionValidator`), and 150m spatial + semantic duplicate detection (`DuplicateDetectorUseCase`)
- **Swappable Backend:** Repository interfaces (`AuthRepository`, `SuggestionRepository`, `AdminRepository`) backed by Mock data out-of-the-box and ready to switch to Firebase (`FirebaseAuthRepository`, `FirebaseGovernanceRepository`, `SuggestBoxMessagingService`)
- **Localization:** English (`values`), Hindi (`values-hi`), Kannada (`values-kn`) — 100% string-resource driven

---

## 2. Project Structure

```text
SuggestBox/
├── gradle/libs.versions.toml                   # Centralized version catalog
├── core/designsystem/                          # Reusable iOS-inspired UI module
│   └── src/main/java/com/suggestbox/core/designsystem/
│       ├── theme/                              # Color, Typography (Inter), Spacing, Theme
│       ├── motion/                             # Springs, pressScaleEffect, shimmerEffect, Haptics
│       ├── icons/                              # SF-style Rounded icon catalog
│       └── components/                         # LargeTitleScaffold, TranslucentBottomBar,
│                                               # PrimaryButton, SuggestCard, GroupedList,
│                                               # SegmentedControl, Chip, TextField, BottomSheet,
│                                               # StatusBadge, EmptyState, AnimatedFeedback
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── res/values*/strings.xml         # EN, HI, KN localization
│       │   └── java/com/suggestbox/
│       │       ├── domain/                     # Models, Repository contracts, Use cases
│       │       ├── data/
│       │       │   ├── local/                  # Room database & DraftDao
│       │       │   ├── media/                  # Client-side ImageCompressor
│       │       │   ├── export/                 # CSV & native Android PdfDocument exporter
│       │       │   ├── preferences/            # EncryptedSharedPreferences
│       │       │   ├── mock/                   # Seed data & Mock repositories
│       │       │   └── remote/firebase/        # Firebase Auth/Firestore/Storage/FCM
│       │       ├── di/                         # Hilt modules (Mock/Firebase switcher)
│       │       └── ui/
│       │           ├── navigation/             # SuggestBoxRoutes & SuggestBoxNavHost
│       │           ├── components/             # Shared SuggestionFeedCard & formatters
│       │           └── screens/                # Onboarding, DPDP Consent, Login, Home,
│       │                                       # 5-Step Submit, Community, Track, Detail,
│       │                                       # Profile, Official Dashboard/Inbox/Detail,
│       │                                       # Broadcast Announcement, Super Admin
│       ├── test/                               # JVM Unit tests (Use cases & ViewModels)
│       └── androidTest/                        # Compose UI instrumentation tests
└── firebase/
    ├── firestore.rules                         # Role-based + DPDP security rules
    ├── storage.rules                           # Media upload validation rules
    └── FIRESTORE_DATA_MODEL.md                 # Collection schemas
```

---

## 3. Running Immediately (Mock Mode — Zero Setup)

1. Open `SuggestBox` directly in **Android Studio** (or run CLI `./gradlew assembleDebug`).
2. Press **Run** (`app`).
3. No Firebase project or Google Maps API key is required:
   - `BuildConfig.USE_FIREBASE` defaults to `false` (uses `MockAuthRepository` and `MockGovernanceRepository`).
   - `Location` step uses an interactive Ward Map Pin canvas when no `MAPS_API_KEY` is configured.
   - Switch between **Citizen**, **Official**, and **Super Admin** previews anytime under **Profile → Active Role Preview**.

---

## 4. Switching to Live Firebase & Google Maps

1. **Create a Firebase Project** in the Firebase Console:
   - Register Android package `com.suggestbox.governance`.
   - Enable **Authentication** (Phone OTP + Anonymous sign-in), **Cloud Firestore**, **Firebase Storage**, and **Cloud Messaging**.
   - Download `google-services.json` and place it in `app/google-services.json`.
2. **Deploy Security Rules & Indexes:**
   - Copy `firebase/firestore.rules` and `firebase/storage.rules` into your Firebase Console (or deploy via `firebase deploy --only firestore:rules,storage`).
   - See `firebase/FIRESTORE_DATA_MODEL.md` for collection schemas.
3. **Enable Live Backend & Maps in `local.properties`:**
   ```properties
   USE_FIREBASE=true
   MAPS_API_KEY=your_google_maps_android_api_key
   ```
4. Sync Gradle and rebuild (`./gradlew assembleDebug`). Hilt automatically switches bindings in `BackendModule` (`AppModule.kt`) without modifying UI or domain layers.

---

## 5. Email OTP Authentication & Cryptographic Security

SuggestBox features a production-ready, email-based authentication system replacing SMS gateways (Twilio/MSG91), ensuring tamper-proof delivery, zero plain-text credential leaks, and high deliverability across government and citizen email providers.

### Security Architecture

1. **Cryptographic OTP Generation (`OtpHasher`):**
   - Uses `java.security.SecureRandom` to generate a 6-digit one-time code in the range `[100000, 999999]`.
   - Never stores plain-text verification codes in the database, SharedPreferences, or in-memory session objects.
   - Generates a cryptographically random 16-byte salt per request and stores only a salted **SHA-256 hash**.
   - Performs constant-time comparison via `MessageDigest.isEqual(...)` to eliminate timing side-channel attacks.

2. **Expiry & Brute-Force Rate Limiting (`EmailOtpManager`):**
   - **Expiry:** Code expires strictly after **10 minutes** (`EXPIRY_DURATION_MS = 600,000 ms`).
   - **Attempt Limits:** Limited to **5 attempts** per verification code. On the 5th failed attempt, the session is immediately invalidated, preventing brute-force attacks.
   - **Single-Use Invalidation:** Upon successful verification, the code and session are destroyed immediately, preventing replay attacks.
   - **Resend Cooldown:** Citizens can only request a resend once every **60 seconds** (`RESEND_COOLDOWN_MS = 60,000 ms`).
   - **Hourly Abuse Limit:** Maximum **5 verification requests per hour** per email address to stop spam and exhaustion attacks.

3. **SMTP Email Delivery (`SmtpEmailService`):**
   - Supports any standard SMTP service: **Gmail SMTP**, **Amazon SES**, **Brevo (Sendinblue)**, **Zoho Mail**, or private government relays (`gov.in`).
   - Pure Kotlin/Java socket client with full **STARTTLS** (port 587) and **SSL** (port 465) support.
   - Dispatches a responsive civic-branded HTML email template with clear OTP display, expiry countdown, and security warnings, paired with an accessible plain-text MIME alternative.

4. **Legacy Account Migration:**
   - Detects existing phone-authenticated citizens who haven't verified an email address (`needsEmailMigration = true`).
   - Renders an Apple HIG-styled alert banner on the citizen profile encouraging them to verify and link their email address without logging them out or interrupting service.

### Configuring SMTP Credentials

Copy `.env.example` or define the following in your environment / CI/CD runner:

```bash
# SMTP Host and Port (587 STARTTLS or 465 SSL)
export SMTP_HOST="smtp.gmail.com"
export SMTP_PORT="587"
export SMTP_USE_TLS="true"

# SMTP Authentication
export SMTP_USERNAME="your-civic-email@gmail.com"
export SMTP_PASSWORD="your-app-specific-password"

# Sender Metadata
export SMTP_FROM="no-reply@suggestbox.gov.in"
export SMTP_FROM_NAME="SuggestBox Civic Governance"
```

*(In local demo/mock mode without SMTP credentials, generated OTPs are seamlessly surfaced in the in-app notification callout banner with 1-tap autofill for rapid testing.)*

---

## 6. Testing

- **Run JVM Unit Tests:**
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Run Compose UI Instrumentation Tests** (with an emulator/device connected):
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```

---

## 6. Generating a Signed Release APK / AAB

1. Generate an upload keystore (keep credentials outside version control):
   ```bash
   keytool -genkeypair -v -keystore suggestbox-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias suggestbox
   ```
2. In Android Studio: **Build → Generate Signed Bundle / APK...** → select **Android App Bundle (AAB)** for Google Play or **APK** for direct municipal MDM distribution.
3. Or via CLI after configuring signing in `keystore.properties`:
   ```bash
   ./gradlew bundleRelease   # Outputs app/build/outputs/bundle/release/app-release.aab
   ./gradlew assembleRelease # Outputs app/build/outputs/apk/release/app-release.apk
   ```
   *(R8 code shrinking and resource shrinking are pre-configured in `app/build.gradle.kts`.)*

---

## 7. Roadmap for v2

1. **AI Auto-Categorization & Urgency Scoring:** On-device / Cloud Functions NLP model (e.g., Gemini / DistilBERT) to auto-route unstructured complaints to departments and flag life-safety hazards (`live electric wire`, `sewage overflow`).
2. **PostGIS + Vector Semantic Duplicate Clustering:** Server-side `ST_DWithin(150m)` spatial indexing paired with multilingual embeddings across English, Hindi, and Kannada transcripts.
3. **Web Admin Portal (Next.js + Tailwind):** Full desktop command center for Municipal Commissioners, Ward Councilors, and multi-department budget allocation committees.
4. **WhatsApp & SMS Bidirectional Channel:** Official WhatsApp Business API & IVR voice-bot intake so citizens without smartphones can submit suggestions and receive tracking updates.
5. **Predictive Spatial Heatmap Analytics:** Time-series clustering of recurring ward infrastructure failures (chronic water leaks, pothole corridors) to guide preventive municipal maintenance.
