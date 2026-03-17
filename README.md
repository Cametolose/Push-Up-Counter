# Push-Up Counter 💪

A gamified Android push-up tracking app with XP/leveling, daily quests, achievements, a lucky wheel item system, online leaderboard, and social trap mechanics. UI language is **German**.

---

## Project Structure

```
Push-Up-Counter/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/liege/counter/         # All source code
│   │   │   ├── res/
│   │   │   │   ├── layout/                 # XML layouts (11 files)
│   │   │   │   ├── drawable/               # Icons, button backgrounds, card shapes
│   │   │   │   ├── animator/               # Button press animation
│   │   │   │   ├── color/                  # Bottom nav color selector
│   │   │   │   ├── menu/                   # Bottom navigation menu
│   │   │   │   ├── values/                 # Colors, strings, themes
│   │   │   │   └── xml/                    # Backup rules
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                            # Unit tests (JUnit)
│   │   └── androidTest/                     # Instrumented tests
│   ├── build.gradle.kts                     # App-level build config
│   └── .gitignore
├── gradle/
│   └── libs.versions.toml                   # Dependency version catalog
├── build.gradle.kts                         # Project-level build config
├── settings.gradle.kts
├── gradlew / gradlew.bat
└── README.md
```

---

## Source Files (`app/src/main/java/liege/counter/`)

### Core Architecture

| File | Purpose |
|---|---|
| **MainActivity.java** (~1140 lines) | Central hub. Hosts `BottomNavigationView` with 5 fragments. Manages all app state (counter, XP, level, quests, daily log), Supabase networking, lucky wheel dialog, item processing, trap sending/receiving. Contains `OnStateChangedListener` interface for fragment updates. |
| **HomeFragment.java** | Main push-up counter screen. Shows counter, level, XP bar, streak, increment buttons (+1/+5/+10), daily joke, stats (daily/weekly/monthly), active effects, and inventory. |
| **QuestsFragment.java** | Daily rotating quests (3 per day, themed by weekday). Targets: 20 (Leicht), 50 (Mittel), 100 (Schwer). Bonus lucky wheel spin for completing all 3. |
| **AchievementsFragment.java** | Displays 41 progressive achievements across 8 categories with title rewards. |
| **LeaderboardFragment.java** | Online ranked leaderboard from Supabase. Shows name, push-ups, level, streak, title. Filters banned players. |
| **SettingsFragment.java** | Username, notification time picker, streak/leaderboard notification toggles, stats display. |

### Item & Wheel System

| File | Purpose |
|---|---|
| **ItemManager.java** | Singleton managing the lucky wheel item pool, inventory (Negate Trap / Streak Save), active buffs/traps with timers, and XP multiplier. Items persisted in `SharedPreferences` ("ItemPrefs"). |
| **LuckyWheelView.java** | Custom `View` drawing/animating a spinning wheel with proportional segments (based on item weight). Pointer at top (270° in canvas coordinates). |

### Data Models

| File | Purpose |
|---|---|
| **LeaderboardEntry.java** | Supabase `leaderboard` table mapping: id, name, pushups, level, XP, quests, streak, title, daily/weekly/monthly/yearly stats, averages, banned flag. |
| **PlayerTrap.java** | Supabase `player_traps` table mapping: sender/receiver info, trap_type, timestamps, active/negated status. |
| **JokeResponse.java** | JokeAPI v2 response: error, type, joke (single), setup/delivery (twopart). |

### API Interfaces (Retrofit)

| File | Purpose |
|---|---|
| **LeaderboardAPI.java** | `GET` leaderboard (sorted by pushups desc), `POST` upsert entry. |
| **ItemAPI.java** | `GET` active traps for receiver, `POST` send trap, `PATCH` update trap status. |
| **JokeApiService.java** | `GET` German jokes from JokeAPI v2 (Programming/Misc/Pun categories). |

### Configuration

| File | Purpose |
|---|---|
| **SupabaseConfig.java** | Supabase URL and anon key (from `BuildConfig` / `gradle.properties`). Contains table column documentation. |
| **AppConfig.java** | Base URL wrapper pointing to Supabase. |

### Notifications

| File | Purpose |
|---|---|
| **NotificationHelper.java** | Creates notification channels (streak, leaderboard). Randomized German motivational messages. |
| **NotificationScheduler.java** | Schedules daily streak alarms via `AlarmManager`. Handles Android 12+ exact alarm permissions. |
| **NotificationReceiver.java** | `BroadcastReceiver` for scheduled streak/leaderboard notifications. |
| **BootReceiver.java** | Reschedules streak alarm on device boot. |

### Other

| File | Purpose |
|---|---|
| **AchievementManager.java** | Singleton with 41 achievements, 16 titles. Auto-completion based on category milestones. |
| **LeaderboardAdapter.java** | `ArrayAdapter` for leaderboard `ListView` entries. |

---

## Layouts (`app/src/main/res/layout/`)

| File | Description |
|---|---|
| `activity_main.xml` | Root layout: fragment container + `BottomNavigationView` |
| `fragment_home.xml` | Counter, level/XP, streak, buttons, joke card, stats row (daily/weekly/monthly), active effects, inventory |
| `fragment_quests.xml` | 3 quest cards with progress bars + bonus card |
| `fragment_achievements.xml` | Achievement `ListView` + title display |
| `fragment_leaderboard.xml` | Refresh button + leaderboard `ListView` |
| `fragment_settings.xml` | Username, notification toggles, time picker, stats |
| `item_leaderboard.xml` | Single leaderboard entry row |
| `item_achievement.xml` | Single achievement card with progress |
| `dialog_lucky_wheel.xml` | Lucky wheel spin dialog |
| `dialog_item_result.xml` | Item won result popup |
| `dialog_achievement_popup.xml` | Achievement unlock popup |

---

## Features

### Push-Up Counter
- Increment buttons: +1, +5, +10
- Undo/decrement support
- Daily push-up logging with date-keyed `HashMap`

### XP & Leveling
- XP gained per push-up (affected by multipliers)
- Level formula: `level * 10` XP per level
- Level-up animation and notification

### Daily Quests
- 3 quests per day (Leicht/Mittel/Schwer: 20/50/100 push-ups)
- Themed by day of week (7 different quest sets in German)
- Completing all 3 unlocks a bonus lucky wheel spin

### Lucky Wheel Item System
- 9 items with weighted chances (total weight 800):
  - ✨ +50 XP (weight 99, 12.375%)
  - 💎 +100 XP (weight 99)
  - 🚀 Doppel-XP buff (weight 99)
  - 🐌 Halbe XP trap (weight 99) — sent to random player
  - 💣 -50 XP trap (weight 99) — sent to random player
  - 💥 -100 XP trap (weight 99) — sent to random player
  - 🛡️ Fallen-Schutz / Negate Trap (weight 99) — inventory item
  - ❤️ Streak-Rettung / Streak Save (weight 99) — inventory item
  - 🃏 The Gambler title (weight 8, 1%) — removed from pool after earned

### Trap System
- Traps sent to random leaderboard players via Supabase `player_traps` table
- Incoming traps checked on app start
- Negate Trap inventory item auto-negates incoming traps
- Banned players and "unbekannt" users cannot send traps

### Achievements (41 total, 8 categories)
- Total Push-ups: 100 → 50,000
- Quests Completed: 10 → 365
- Streak: 3 → 100 days
- Bonus XP: 5 → 100
- Level: 5 → 100
- Daily Push-ups: 30 → 500
- Weekly Push-ups: 100 → 2,000
- Monthly Push-ups: 500 → 10,000
- 16 earnable titles displayed on leaderboard

### Online Leaderboard
- Supabase backend with PostgREST API
- Shows: rank, name, push-ups, level, streak, title
- Auto-updates via `JobScheduler` (`LeaderboardUpdateService`)
- Banned player filtering

### Notifications
- Daily streak reminder at configurable time
- Leaderboard update notifications
- Randomized German motivational messages
- Boot receiver for alarm rescheduling

### Daily Joke
- German jokes from JokeAPI v2
- Cached per day in `SharedPreferences`
- Categories: Programming, Misc, Pun

---

## Design & Theming

- **Dark theme**: Background `#121212`, card backgrounds `#1E1E2E`
- **Accent colors**: Cyan/teal (`#00E5FF` / `#00B8D4` / `#0097A7`) for buttons
- **XP/Level**: Green (`#81C784`)
- **Streak**: Gold (`#FFD600`)
- **Buttons**: Custom gradient drawables with ripple effect and bounce animation (`button_press.xml` animator)
- **Material Components**: Buttons require `app:backgroundTint="@null"` to show custom drawable backgrounds (Material Components library auto-applies a tint that overrides custom backgrounds)

---

## Tech Stack

- **Language**: Java
- **Min SDK**: Android (uses Material3 theme)
- **Build**: Android Gradle Plugin 8.5.2
- **UI**: Fragments + BottomNavigationView, custom Views
- **Backend**: Supabase (PostgREST API)
- **Networking**: Retrofit 2 + OkHttp + Gson
- **Notifications**: AlarmManager + BroadcastReceiver
- **Dependencies** (from `libs.versions.toml`):
  - AndroidX AppCompat 1.7.0
  - Material Design 1.12.0
  - ConstraintLayout 2.1.4
  - Firebase Auth 23.0.0 / Database 21.0.0
  - JUnit 4.13.2, Espresso 3.6.1

---

## Backend (Supabase)

### Tables

**`leaderboard`** — Player stats (upsert by `id`):
- `id` (text, PK), `name`, `pushups`, `level`, `totalXp`, `currentXp`, `xpForNextLevel`
- `totalQuestsCompleted`, `quest1Completions`, `quest2Completions`, `quest3Completions`, `bonusXpCollected`
- `dailyPushups`, `weeklyPushups`, `monthlyPushups`, `yearlyPushups`
- `avgPushupsPerDay`, `avgPushupsPerWeek`, `avgPushupsPerMonth`
- `streak`, `title`, `banned` (boolean)

**`player_traps`** — Trap system:
- `id` (auto), `sender_id`, `sender_name`, `receiver_id`, `receiver_name`
- `trap_type` (text: "half_xp", "minus_50", "minus_100")
- `created_at`, `expires_at`, `active` (boolean), `negated` (boolean)

### Setup
Credentials are loaded from `BuildConfig` (set in `gradle.properties` or `local.properties`):
```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

---

## Building

```bash
# Requires Android SDK
./gradlew assembleDebug

# Clean build (recommended after drawable/resource changes)
./gradlew clean assembleDebug
```

**Note**: After changing drawable resources, always clean build and reinstall the app to ensure updated button textures are applied (Android may cache old resources).

---

## OTA Updates (APK Distribution Without Play Store)

To distribute updates to users directly via APK — without the Play Store — and have the app update itself automatically, follow this approach:

### Recommended Approach: GitHub Releases + In-App Update Check

**What you need:**

1. **Host releases on GitHub** (or any HTTPS server you control):
   - Build a signed release APK: `./gradlew assembleRelease`
   - Sign it with your keystore (same key every release, so Android allows updates)
   - Create a GitHub Release and attach the APK as an asset
   - Publish a `version.json` alongside it:
     ```json
     { "versionCode": 2, "versionName": "1.1.0", "apkUrl": "https://github.com/you/repo/releases/download/v1.1.0/app-release.apk" }
     ```

2. **Add an in-app update checker** (`UpdateChecker.java`):
   - On app start (or in Settings), fetch `version.json` from your server
   - Compare `versionCode` against `BuildConfig.VERSION_CODE`
   - If a newer version exists, show a dialog: "Update verfügbar — jetzt installieren?"
   - On confirmation, download the APK with `DownloadManager` and install it via `FileProvider`

3. **Required in `AndroidManifest.xml`**:
   ```xml
   <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
   <!-- FileProvider for sharing the downloaded APK -->
   <provider
       android:name="androidx.core.content.FileProvider"
       android:authorities="${applicationId}.provider"
       android:exported="false"
       android:grantUriPermissions="true">
       <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
           android:resource="@xml/file_paths"/>
   </provider>
   ```

4. **Signing (critical — prevents "virus" warnings)**:
   - Always sign with the same release keystore (`./gradlew assembleRelease` with `signingConfig`)
   - A consistent signature proves the APK comes from you — Android's package manager trusts upgrades from the same signer
   - Android's Play Protect may still warn on first install of an "unknown" APK (this is normal); users tap "Install anyway"
   - **Never** use a debug keystore for distribution — debug-signed APKs trigger stronger warnings

5. **Install flow (Android 8+)**:
   - The user must have "Install from unknown sources" enabled for your app (Android will prompt automatically on first install)
   - After download, trigger install:
     ```java
     Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", apkFile);
     Intent install = new Intent(Intent.ACTION_VIEW);
     install.setDataAndType(apkUri, "application/vnd.android.package-archive");
     install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
     context.startActivity(install);
     ```

**Summary of steps:**
1. Set up a release keystore and sign every APK with it
2. Host APKs + a `version.json` on GitHub Releases (free)
3. Add an `UpdateChecker` that fetches `version.json` on app launch and prompts the user
4. Add `REQUEST_INSTALL_PACKAGES` permission and a `FileProvider` to the manifest
5. Users install the first APK manually; all future updates happen automatically via the in-app checker



1. **State Management**: All state in `MainActivity`. Fragments register as `OnStateChangedListener` in `onResume`, unregister in `onPause`. Call `notifyListeners()` after any state change.
2. **Adding Items**: Add to `ItemManager.getEligibleItems()` with `WheelItem(id, name, description, emoji, type, color, weight)`. Handle in `MainActivity.processWheelResult()`.
3. **Adding Achievements**: Append to `AchievementManager.ACHIEVEMENTS[]` array. Category must match `getCurrentValue()` method.
4. **Adding Titles**: Append to `AchievementManager.TITLES[]`. Set `titleReward` in achievement definition.
5. **Supabase Columns**: All `camelCase`. Add new columns to both `LeaderboardEntry.java` and the upsert in `MainActivity.updateOnlineLeaderboard()`.
6. **Button Styling**: Use `android:background="@drawable/button_increment_bg"` with `app:backgroundTint="@null"` and `android:stateListAnimator="@animator/button_press"`.
7. **Layout Order (Home)**: Counter → Level → XP bar → Streak → Buttons → Joke → Stats (daily/weekly/monthly) → Active Effects → Inventory.

