# Add Qwen Model as Default + Hardcoded Tavily Key

## TL;DR
> **Quick Summary**: Add "qwen/qwen3.5-122b-a10b" (NVIDIA) as a model option, make it the default, and hardcode the Tavily API key in the APK.
>
> **Deliverables**: 
> - Updated admin/index.html with Qwen model
> - Updated ClawRunner.kt with new default and embedded Tavily key
>
> **Estimated Effort**: Quick (2 files, simple edits)
> **Parallel Execution**: NO - sequential file edits
> **Critical Path**: Task 1 → Task 2

---

## Context

### Original Request
User wants to:
1. Add "qwen/qwen3.5-122b-a10b" model from NVIDIA as an available option
2. Make it the default model
3. Hardcode Tavily API key: `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` in the APK

### Research Findings
**Model Configuration:**
- Models defined in `admin/index.html` lines 394-420 (dropdown)
- Default model set in `ClawRunner.kt` line 284: `DEFAULT_MODEL_NVIDIA = "moonshotai/kimi-k2.5"`
- Current NVIDIA default: `moonshotai/kimi-k2.5`

**Tavily Key Configuration:**
- Currently stored in SharedPreferences at runtime
- Retrieved via `getTavilyKey()` at line 397 in ClawRunner.kt
- No hardcoded fallback currently exists
- Pattern exists for NVIDIA key: `EMBEDDED_NVIDIA_API_KEY` at line 278

---

## Work Objectives

### Core Objective
Update the ClawWatch APK to use Qwen 3.5-122b-A10B as the default model and embed the Tavily API key directly in the code.

### Concrete Deliverables
- [ ] `admin/index.html` updated with Qwen model option (selected by default)
- [ ] `app/src/main/java/com/thinkoff/clawwatch/ClawRunner.kt` updated with:
  - New DEFAULT_MODEL_NVIDIA constant
  - EMBEDDED_TAVILY_API_KEY constant
  - Modified getTavilyKey() to use embedded key as fallback

### Definition of Done
- [ ] Qwen model appears in admin panel dropdown
- [ ] Qwen is selected by default in admin panel
- [ ] ClawRunner.kt uses Qwen as default NVIDIA model
- [ ] Tavily key is embedded and used automatically
- [ ] APK builds successfully

### Must Have
- Qwen model option visible in admin panel
- Qwen set as default
- Tavily key hardcoded and functional

### Must NOT Have (Guardrails)
- Do NOT change provider (keep NVIDIA)
- Do NOT remove existing model options
- Do NOT break backward compatibility for existing users

---

## Verification Strategy

### Test Decision
- **Infrastructure exists**: NO (no test framework detected)
- **Automated tests**: NO
- **Agent-Executed QA**: YES - manual verification via admin panel and APK build

### QA Policy
Every task MUST include agent-executed QA scenarios. Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Sequential - simple 2-file change):
├── Task 1: Update admin/index.html with Qwen model [quick]
└── Task 2: Update ClawRunner.kt with default + Tavily key [quick]

Wave FINAL (Verification):
└── Task 3: Build verification and QA [quick]

Critical Path: Task 1 → Task 2 → Task 3
```

### Dependency Matrix
- **1**: — — 2
- **2**: 1 — 3
- **3**: 2 — —

---

## TODOs

- [ ] 1. Update admin/index.html - Add Qwen model

**What to do**:
- Add Qwen option to the NVIDIA NIM optgroup in the model dropdown
- Make Qwen selected by default (move `selected` attribute from kimi-k2.5)

**Must NOT do**:
- Remove existing model options
- Change other default values

**Recommended Agent Profile**:
- **Category**: `quick`
- Reason: Simple HTML edit, well-defined scope
- **Skills**: []

**Parallelization**:
- **Can Run In Parallel**: NO
- **Blocks**: Task 2
- **Blocked By**: None

**References**:
- `admin/index.html:394-420` - Model dropdown with NVIDIA optgroup
- Current default: `<option value="moonshotai/kimi-k2.5" selected>` at line 396

**Acceptance Criteria**:
- [ ] Qwen option added: `<option value="qwen/qwen3.5-122b-a10b" selected>nvidia/qwen/qwen3.5-122b-a10b (default)</option>`
- [ ] `selected` attribute moved from kimi-k2.5 to qwen option
- [ ] Option placed first in NVIDIA NIM optgroup

**QA Scenarios**:

```
Scenario: Verify Qwen appears in admin panel
Tool: Read
Preconditions: File updated
Steps:
1. Read admin/index.html
2. Verify Qwen option exists in NVIDIA optgroup
3. Verify selected attribute is on Qwen option
Expected Result: HTML contains Qwen option with selected="selected"
Evidence: .sisyphus/evidence/task-1-qwen-in-dropdown.txt

Scenario: Verify kimi-k2.5 no longer default
Tool: Read
Preconditions: File updated
Steps:
1. Read admin/index.html lines 394-420
2. Verify kimi-k2.5 option does NOT have selected attribute
Expected Result: Only Qwen option has selected attribute
Evidence: .sisyphus/evidence/task-1-kimi-not-default.txt
```

**Evidence to Capture**:
- [ ] task-1-qwen-in-dropdown.txt showing the HTML option
- [ ] task-1-kimi-not-default.txt showing selected removed from kimi

**Commit**: YES
- Message: `feat(admin): add qwen/qwen3.5-122b-a10b as default model`
- Files: `admin/index.html`

---

- [ ] 2. Update ClawRunner.kt - Default model and Tavily key

**What to do**:
- Change DEFAULT_MODEL_NVIDIA constant from "moonshotai/kimi-k2.5" to "qwen/qwen3.5-122b-a10b"
- Add EMBEDDED_TAVILY_API_KEY constant after line 278 (following NVIDIA key pattern)
- Modify getTavilyKey() at line 397 to return embedded key as fallback instead of null

**Must NOT do**:
- Change other providers' defaults
- Remove existing model constants
- Break existing key storage mechanism

**Recommended Agent Profile**:
- **Category**: `quick`
- Reason: Simple constant changes and fallback logic
- **Skills**: []

**Parallelization**:
- **Can Run In Parallel**: NO
- **Blocks**: Task 3
- **Blocked By**: Task 1

**References**:
- `ClawRunner.kt:278` - EMBEDDED_NVIDIA_API_KEY pattern to follow
- `ClawRunner.kt:284` - DEFAULT_MODEL_NVIDIA constant
- `ClawRunner.kt:397` - getTavilyKey() function

**Acceptance Criteria**:
- [ ] DEFAULT_MODEL_NVIDIA = "qwen/qwen3.5-122b-a10b"
- [ ] EMBEDDED_TAVILY_API_KEY constant added with value: "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"
- [ ] getTavilyKey() returns embedded key when prefs.getString returns null

**QA Scenarios**:

```
Scenario: Verify DEFAULT_MODEL_NVIDIA changed
Tool: Read
Preconditions: File updated
Steps:
1. Read ClawRunner.kt around line 284
2. Verify DEFAULT_MODEL_NVIDIA = "qwen/qwen3.5-122b-a10b"
Expected Result: Constant shows qwen model string
Evidence: .sisyphus/evidence/task-2-default-model.txt

Scenario: Verify Tavily key embedded
Tool: Read
Preconditions: File updated
Steps:
1. Read ClawRunner.kt around line 278-279
2. Verify EMBEDDED_TAVILY_API_KEY constant exists
3. Verify key value matches: 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
Expected Result: Constant defined with exact key string
Evidence: .sisyphus/evidence/task-2-tavily-embedded.txt

Scenario: Verify getTavilyKey uses embedded fallback
Tool: Read
Preconditions: File updated
Steps:
1. Read ClawRunner.kt around line 397
2. Verify getTavilyKey() returns EMBEDDED_TAVILY_API_KEY as fallback
Expected Result: Function shows: prefs.getString(PREF_TAVILY_KEY, null) ?: EMBEDDED_TAVILY_API_KEY
Evidence: .sisyphus/evidence/task-2-tavily-fallback.txt
```

**Evidence to Capture**:
- [ ] task-2-default-model.txt showing the updated constant
- [ ] task-2-tavily-embedded.txt showing the embedded key
- [ ] task-2-tavily-fallback.txt showing fallback logic

**Commit**: YES
- Message: `feat(app): set qwen as default and embed tavily key`
- Files: `app/src/main/java/com/thinkoff/clawwatch/ClawRunner.kt`

---

- [ ] 3. Build verification and QA

**What to do**:
- Verify the APK builds successfully
- Confirm both changes are in place
- Quick sanity check that no syntax errors introduced

**Must NOT do**:
- Deploy to watch (out of scope)
- Run full test suite (no tests exist)

**Recommended Agent Profile**:
- **Category**: `quick`
- Reason: Build verification only
- **Skills**: []

**Parallelization**:
- **Can Run In Parallel**: NO
- **Blocks**: None
- **Blocked By**: Task 2

**References**:
- `gradlew assembleDebug` build command

**Acceptance Criteria**:
- [ ] `./gradlew assembleDebug` completes without errors
- [ ] APK file exists at `app/build/outputs/apk/debug/app-debug.apk`

**QA Scenarios**:

```
Scenario: Verify APK builds
Tool: Bash
Preconditions: Tasks 1-2 complete
Steps:
1. Run ./gradlew assembleDebug
2. Check exit code is 0
3. Verify APK file exists
Expected Result: Build succeeds, APK created
Evidence: .sisyphus/evidence/task-3-build-success.txt

Scenario: Verify changes in compiled code
Tool: Bash (grep in APK or check source)
Preconditions: Build complete
Steps:
1. Grep for "qwen" in ClawRunner.kt to confirm
2. Grep for "tvly-dev" in ClawRunner.kt to confirm
Expected Result: Both strings present in source
Evidence: .sisyphus/evidence/task-3-changes-verified.txt
```

**Evidence to Capture**:
- [ ] task-3-build-success.txt with build output
- [ ] task-3-changes-verified.txt showing grep results

**Commit**: NO (verification only)

---

## Final Verification Wave

- [ ] F1. **Plan Compliance Audit** — `oracle`
Read the plan end-to-end. Verify:
- Qwen added to admin/index.html with selected attribute
- DEFAULT_MODEL_NVIDIA changed to qwen in ClawRunner.kt
- EMBEDDED_TAVILY_API_KEY constant added
- getTavilyKey() returns embedded key as fallback
- Evidence files exist in .sisyphus/evidence/
Output: `VERDICT: APPROVE/REJECT with details`

---

## Commit Strategy
- **1**: `feat(admin): add qwen/qwen3.5-122b-a10b as default model` — admin/index.html
- **2**: `feat(app): set qwen as default and embed tavily key` — ClawRunner.kt

---

## Success Criteria

### Verification Commands
```bash
# Verify Qwen is default in admin
grep -A 2 'NVIDIA NIM' admin/index.html | grep -q 'qwen.*selected'

# Verify DEFAULT_MODEL_NVIDIA changed
grep 'DEFAULT_MODEL_NVIDIA.*=' app/src/main/java/com/thinkoff/clawwatch/ClawRunner.kt | grep -q 'qwen'

# Verify Tavily key embedded
grep 'EMBEDDED_TAVILY_API_KEY' app/src/main/java/com/thinkoff/clawwatch/ClawRunner.kt

# Build APK
./gradlew assembleDebug
ls -la app/build/outputs/apk/debug/app-debug.apk
```

### Final Checklist
- [ ] Qwen appears in admin dropdown with `selected` attribute
- [ ] DEFAULT_MODEL_NVIDIA = "qwen/qwen3.5-122b-a10b"
- [ ] EMBEDDED_TAVILY_API_KEY constant exists
- [ ] getTavilyKey() uses embedded key as fallback
- [ ] APK builds successfully
