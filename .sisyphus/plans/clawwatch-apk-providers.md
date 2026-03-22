# ClawWatch: APK + Multi-Provider Configuration

## TL;DR

> Update ClawWatch for Samsung Ultra Watch 2025 (Wear OS 6) with OpenCode Zen as default free provider and optional support for NVIDIA/Moonshot AI via API key.

**Deliverables**:
- Updated build config for Wear OS 6 (minSdk 33, targetSdk 35)
- OpenCode Zen as default provider (works without API key for free models)
- Optional API key input for premium models
- NVIDIA and Moonshot AI provider support via API key
- Debug APK ready for deployment

**Estimated Effort**: Medium  
**Parallel Execution**: Wave-based  
**Critical Path**: SDK Update → Provider Core → Build

---

## Context

### Original Request
User wants to:
1. Build APK for Samsung Ultra Watch 2025 (Wear OS 6)
2. Use OpenCode Zen as default "free without API key" provider
3. Support NVIDIA, moonshotai, kimi-k2.5 via API key when user adds one

### Key Findings

**OpenCode Zen Behavior**:
- Works WITHOUT API key for free models - uses `"public"` as placeholder
- When API key added → premium models unlock
- Endpoint: `https://opencode.ai/zen/v1/responses`
- Uses OpenAI-compatible `/v1/responses` API

**Provider Support**:
- **NVIDIA**: Endpoint `https://integrate.api.nvidia.com/v1`, model `moonshotai/kimi-k2.5`
- **Moonshot AI**: Direct API at `https://api.moonshot.cn/v1`
- **Kimi K2.5**: Available via both NVIDIA endpoint and direct Moonshot

**Current Architecture Issue**:
- ClawRunner.kt directly calls Anthropic API, NOT NullClaw binary
- Need to add multi-provider HTTP support in Kotlin

### Wear OS Version Strategy
- Wear OS 6 ships with Android 14 (API 34)
- For 2026 best practices: targetSdk 35 (Android 15), minSdk 33 (Wear OS 4+)
- This covers: Watch 4/5/6 (2021-2025), ultra 2025

---

## Work Objectives

### Core Objective
Enable ClawWatch to:
1. Run on Wear OS 6 (Samsung Ultra Watch 2025) and older watches
2. Use OpenCode Zen by default (no API key needed for free models)
3. Allow manual API key input for premium providers

### Concrete Deliverables
- Updated `app/build.gradle.kts` with Wear OS 6 SDK
- Modified `ClawRunner.kt` with multi-provider support
- New provider selection UI or config
- Debug APK at `app/build/outputs/apk/debug/app-debug.apk`

### Definition of Done
- [ ] APK builds successfully
- [ ] APK installs on Wear OS 6 device
- [ ] OpenCode Zen works without API key (free models)
- [ ] Can add API key for premium access
- [ ] NVIDIA provider callable when key provided
- [ ] Moonshot/Kimi provider callable when key provided

### Must Have
- OpenCode Zen as default (free models work)
- API key input for premium access
- At least one paid provider (NVIDIA or Moonshot) functional

### Must NOT Have
- Break existing Anthropic support for users who have keys
- Require API key for basic functionality
- Increase binary size significantly

---

## Verification Strategy

### Test Decision
- **Infrastructure exists**: YES (existing Android project)
- **Automated tests**: Existing Android test setup
- **Framework**: JUnit + Android test

### QA Policy
Every task includes agent-executed verification:
- **Build**: `./gradlew assembleDebug` must succeed
- **Lint**: `ktlintCheck` if configured
- **Install**: APK installs without errors

---

## Execution Strategy

### Wave 1 (SDK + Foundation)
```
Wave 1 (Foundation):
├── Task 1: Update SDK versions in build.gradle.kts
├── Task 2: Update gradle wrapper if needed for AGP 8.x
└── Task 3: Verify shell project builds
```

### Wave 2 (Provider Core)
```
Wave 2 (Provider Implementation):
├── Task 4: Refactor ClawRunner.kt for multi-provider support
├── Task 5: Add OpenCode Zen provider implementation
├── Task 6: Add NVIDIA provider implementation  
└── Task 7: Add Moonshot AI provider implementation
```

### Wave 3 (UI + Config)
```
Wave 3 (UI & Configuration):
├── Task 8: Add provider selection to settings/config
├── Task 9: Add API key input storage (SecurePrefs)
└── Task 10: Build and verify debug APK
```

### Dependency Matrix
- **1**: — — 3, 2
- **2**: 1 — 3, 2
- **3**: 1, 2 — 4, 5, 6, 7
- **4**: 3 — 8, 9, 10
- **5**: 3, 4 — 8, 9, 10
- **6**: 3, 4 — 8, 9, 10
- **7**: 3, 4 — 8, 9, 10
- **8**: 5, 6, 7 — 10, 2
- **9**: 5, 6, 7 — 10, 2
- **10**: 8, 9 — FINAL

---

## TODOs

- [x] 1. **Update SDK versions for Wear OS 6**

  **What to do**:
  - Update `app/build.gradle.kts`:
    - Change `minSdk` from 30 to 33 (Wear OS 4+, covers 2021-2025 watches)
    - Change `targetSdk` from 34 to 35 (Android 15, 2026 best practice)
    - Change `compileSdk` to 35
  - Verify AGP version compatibility in `gradle/libs.versions.toml`
  - Update versionCode/versionName if needed

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Reason**: Simple config file changes, no complex logic

  **Parallelization**:
  - **Can Run In Parallel**: YES (Wave 1)
  - **Parallel Group**: Wave 1 (with Tasks 2, 3)
  - **Blocks**: Tasks 4-10
  - **Blocked By**: None

  **References**:
  - `app/build.gradle.kts` - Current SDK config
  - `gradle/libs.versions.toml` - AGP version

  **Acceptance Criteria**:
  - [ ] minSdk = 33
  - [ ] targetSdk = 35
  - [ ] compileSdk = 35
  - [ ] ./gradlew assembleDebug succeeds

- [x] 2. **Update NullClaw binary for multi-provider** (SKIPPED - using direct HTTP)

  **What to do**:
  - Current NullClaw binary may need update for provider support
  - OR use direct HTTP calls in Kotlin (simpler approach)
  - Research: Check if existing binary supports custom endpoints

  **Decision**: Using direct HTTP calls in Kotlin (simpler, more maintainable)
  - ClawRunner.kt already makes direct HTTP calls to Anthropic
  - Extending to support multiple providers via HTTP is straightforward
  - No need to rebuild NullClaw binary

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Reason**: Need to determine architecture approach

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Blocks**: 4-7
  - **Blocked By**: None

- [x] 3. **Refactor ClawRunner.kt for multi-provider**

  **What to do**:
  - Add provider enum: ANTHROPIC, OPENCODE_ZEN, NVIDIA, MOONSHOT
  - Create abstract Provider interface
  - Refactor HTTP call logic to use provider-specific endpoints
  - Keep backward compatibility with existing Anthropic users

  **Must NOT do**:
  - Break existing API key storage/retrieval
  - Remove Anthropic support

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Reason**: Complex refactoring of HTTP layer

  **Parallelization**:
  - **Can Run In Parallel**: NO (depends on Task 2)
  - **Blocks**: Tasks 5-7
  - **Blocked By**: Task 2

  **References**:
  - `app/src/main/java/com/thinkoff/clawwatch/ClawRunner.kt` - Current implementation

  **Acceptance Criteria**:
  - [ ] Provider enum supports all 4 providers
  - [ ] Existing Anthropic config still works

- [x] 4. **Add OpenCode Zen provider**

  **What to do**:
  - Implement OpenCodeZenProvider class
  - Endpoint: `https://opencode.ai/zen/v1/responses`
  - Without API key: use `"public"` as apiKey (enables free models)
  - With API key: use provided key for premium models
  - Use OpenAI-compatible /responses API format

  **API Format**:
  ```json
  {
    "model": "gpt-5-nano",
    "messages": [{"role": "user", "content": "..."}]
  }
  ```

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Reason**: New HTTP provider implementation

  **Parallelization**:
  - **Can Run In Parallel**: YES (Wave 2)
  - **Blocks**: Task 8-10
  - **Blocked By**: Task 3

  **References**:
  - OpenCode Zen docs: `https://opencode.ai/docs/zen/`
  - OpenAI-compatible format

  **Acceptance Criteria**:
  - [x] Works without API key (free models)
  - [x] Works with API key (premium models)
  - [x] Uses /v1/responses endpoint

- [x] 5. **Add NVIDIA provider**

  **What to do**:
  - Implement NVIDIaprovider class
  - Endpoint: `https://integrate.api.nvidia.com/v1`
  - Model: `moonshotai/kimi-k2.5` (default) or user-selectable
  - Uses OpenAI-compatible /chat/completions API

  **API Format**:
  ```json
  {
    "model": "moonshotai/kimi-k2.5",
    "messages": [{"role": "user", "content": "..."}]
  }
  ```

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`

  **Parallelization**:
  - **Can Run In Parallel**: YES (Wave 2)
  - **Blocks**: Task 8-10
  - **Blocked By**: Task 3

  **Acceptance Criteria**:
  - [x] Requires API key (nvapi-xxx)
  - [x] Uses NVIDIA endpoint
  - [x] Supports Kimi K2.5 model

- [x] 6. **Add Moonshot AI provider**

  **What to do**:
  - Implement MoonshotProvider class
  - Endpoint: `https://api.moonshot.cn/v1` (or international)
  - Models: kimi-k2.5, kimi-k2-thinking, etc.
  - Uses OpenAI-compatible /chat/completions API

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`

  **Parallelization**:
  - **Can Run In Parallel**: YES (Wave 2)
  - **Blocks**: Task 8-10
  - **Blocked By**: Task 3

  **Acceptance Criteria**:
  - [x] Requires API key
  - [x] Uses Moonshot endpoint
  - [x] Supports Kimi models

- [x] 7. **Add provider selection to settings**

  **What to do**:
  - Add provider selection in SecurePrefs
  - Add UI or config option to choose provider
  - Add API key input for each provider
  - Default to OpenCode Zen (no key needed)

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Reason**: Config storage and simple UI

  **Parallelization**:
  - **Can Run In Parallel**: YES (Wave 3)
  - **Blocks**: Task 10
  - **Blocked By**: Tasks 4-6

  **References**:
  - `app/src/main/java/com/thinkoff/clawwatch/SecurePrefs.kt`

  **Acceptance Criteria**:
  - [ ] Provider selection persists
  - [ ] API keys stored securely
  - [ ] Defaults to OpenCode Zen

- [x] 8. **Build and verify debug APK**

  **What to do**:
  - Run `./gradlew assembleDebug`
  - Verify APK generates at `app/build/outputs/apk/debug/`
  - Check APK size is reasonable (<100MB)

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Reason**: Build verification

  **Parallelization**:
  - **Can Run In Parallel**: NO (final task)
  - **Blocks**: None
  - **Blocked By**: Tasks 1, 7

  **Acceptance Criteria**:
  - [ ] Build succeeds
  - [ ] APK exists at expected path
  - [ ] APK size < 100MB

---

## Final Verification Wave

- [ ] F1. **Plan Compliance Audit** — Verify all tasks completed
- [ ] F2. **Build Verification** — APK compiles and runs
- [ ] F3. **Provider Test** — Each provider callable when configured

---

## Success Criteria

### Verification Commands
```bash
./gradlew assembleDebug
# Expected: BUILD SUCCESSFUL
ls -la app/build/outputs/apk/debug/
# Expected: app-debug.apk exists
```

### Final Checklist
- [ ] minSdk 33, targetSdk 35
- [ ] OpenCode Zen default without API key
- [ ] API key input works for premium providers
- [ ] NVIDIA provider configurable
- [ ] Moonshot/Kimi provider configurable
- [ ] APK builds successfully
