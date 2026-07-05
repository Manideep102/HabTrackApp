# HabTrack — Android Habit Tracker

## Prompting guide for Fable 5 (`claude-fable-5`)

Fable 5 excels at long-context reasoning and multi-step execution. Give it rich upfront context rather than terse commands. Name exact file paths and API symbols — it handles large codebases well when anchored. Specify the **why** behind constraints so it can reason about edge cases rather than blindly following rules.

---

## Project identity

- **Stack**: Android Kotlin + Jetpack Compose (Material 3), Room, WorkManager
- **Build**: `app/build.gradle.kts`, kapt for Room annotation processing
- **SDK**: minSdk 26, compileSdk 36, targetSdk 35
- **Package**: `com.example.habtrack`
- **Room DB version**: 4 (any schema change requires a real `Migration`, NOT just `fallbackToDestructiveMigration`)

---

## Key file map

| Path | Purpose |
|---|---|
| `app/src/main/java/com/example/habtrack/MainActivity.kt` | All main Composables: `HabTrackApp`, `HabitCard`, `HabitLogBottomSheet`, `ReminderSettingsSheet`, `AddHabitSheet`, `DatePickerDialog` |
| `ui/HabitViewModel.kt` | Central ViewModel: habit state, analytics, AI insights, Health Connect sync |
| `ui/AnalyticsScreen.kt` | Analytics tab: stat cards, AI insights card, monthly calendar, heatmap, clusters |
| `ui/SettingsScreen.kt` | API key (EncryptedSharedPreferences) + Health Connect status/connect |
| `data/HabitEntity.kt` | Room entity with all habit fields incl. `autoSyncEnabled`, `autoSyncMetric` |
| `data/HabitDatabase.kt` | Room DB singleton, migrations, version |
| `data/DailyCompletionDao.kt` | DAO: `@Insert`, `@Update`, `@Query` for daily progress rows |
| `data/HabitRepository.kt` | Repository wrapping DAO; used by ViewModel |
| `data/ApiKeyStore.kt` | EncryptedSharedPreferences wrapper for Anthropic API key |
| `health/HealthConnectManager.kt` | HC availability check, permissions, per-metric `readTodayValueFor()` |
| `ai/HabitInsightsService.kt` | Anthropic Java SDK call; model: `claude-haiku-4-5` |
| `workers/HabitResetWorker.kt` | WorkManager: midnight daily habit value reset |
| `notifications/HabitNotificationManager.kt` | Notification channel + per-habit reminder scheduling |

---

## Architecture rules — do not violate

- **Room migrations**: DB is version 4. Any new column requires `ALTER TABLE` SQL in a `Migration(N, N+1)` object added via `.addMigrations(...)` before `.fallbackToDestructiveMigration()`. Skipping this wipes all user data on upgrade.
- **Health Connect is always try/catch**: `HealthConnectClient.getSdkStatus()` and `getOrCreate()` can throw on some OEM ROMs. Every call site wraps them. Health Connect sync runs at app start in `LaunchedEffect(Unit)` inside `HabTrackApp` — a crash here crashes the app before the user sees anything.
- **AI calls are user-triggered only**: `HabitInsightsService.generateInsights()` is called only when the user taps "Generate Insights" in `AnalyticsScreen`. Never call it automatically or on app start — it costs API credits.
- **Health Connect sync = LaunchedEffect, not WorkManager**: HC reads are on-device and immediate; WorkManager is reserved for background work that must survive process death (midnight reset, reminders).
- **API key is never logged**: `ApiKeyStore` wraps `EncryptedSharedPreferences`. Never `Log.d` the key value or transmit it anywhere other than the Anthropic API endpoint.

---

## Health Connect integration

- `HealthConnectManager(context).getAvailability()` returns `HealthConnectAvailability.Available / NotInstalled / UpdateRequired` — safe to call, never throws.
- `HealthConnectManager.getClient(context)` returns `HealthConnectClient?` — null if HC unavailable.
- `readTodayValueFor(client, metric)` returns today's aggregate as `Float` — steps count, kcal, or km. Never throws (returns 0f on any error).
- Per-habit sync: `HabitEntity.autoSyncEnabled: Boolean` + `autoSyncMetric: String?` (stores `HealthMetric.name`).
- `HabitViewModel.syncFromHealthConnect(context)` does the full sync pass; called once per app session from `HabTrackApp`.
- `HabitViewModel.setHabitAutoSync(habit, enabled, metric)` persists the per-habit toggle.

---

## AI Insights integration

- `HabitInsightsService.generateInsights(apiKey, habitSummary)` → `Result<String>` using Anthropic Java SDK.
- Model: `Model.of("claude-haiku-4-5")` — do not change without user approval.
- Content extraction: `response.content().joinToString("\n") { block -> block.text().map { it.text() }.orElse("") }` — the Java SDK returns `Optional<TextBlock>` from `.text()`.
- `InsightsState`: `Idle / Loading / Success(text) / Error(message)` sealed class in `HabitViewModel.kt`.

---

## UI conventions

- Color palette: indigo `0xFF6366F1`, purple `0xFF7C3AED`, green `0xFF10B981`, amber `0xFFF59E0B`, red `0xFFDC2626`, slate `0xFF64748B`, near-black `0xFF0F172A`
- Background: `0xFFF8FAFC` (off-white)
- Card shadow: `shadowElevation = 3.dp` on `Surface` with `RoundedCornerShape(20.dp)`
- Button height: `56.dp` (primary actions), `50.dp` (secondary)
- Habit icon mapping lives in `HabitCard` (`MainActivity.kt`): `when (habit.iconName)` with keys `"bolt"`, `"favorite"`, `"directions_run"`, `"bedtime"`, `"local_fire_department"`, `"self_improvement"`, `"fitness_center"`, `"directions_walk"`
- Streak badge: shown on the habit icon when `habit.currentStreak >= 2`, orange dot with white number

---

## Known issues (as of Jul 2026)

1. `HabitCard.onReminderClick` was unwired from the UI — now fixed with a `Tune` icon button.
2. `HealthConnectManager.getAvailability()` lacked try/catch — now fixed; OEM ROM exceptions return `NotInstalled`.
3. `SettingsScreen` LaunchedEffect lacked try/catch — now fixed; exception sets `hcAvailability = NotInstalled`.
4. Motivational quote used `habits.size` (always wrong) — now computes actual remaining count.

---

## Do not

- Add comments that explain WHAT the code does — only add a comment for a non-obvious WHY (a hidden constraint, OEM workaround, API quirk).
- Create new packages without reason; existing: `data`, `health`, `ai`, `workers`, `notifications`, `ui`.
- Call the Anthropic API without explicit user action.
- Use WorkManager for Health Connect sync.
- Amend commits — create new ones.
- Skip `--no-verify` on git hooks.
