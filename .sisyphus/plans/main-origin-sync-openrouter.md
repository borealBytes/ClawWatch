# Realign Main to Upstream + Preserve OpenRouter Work

## TL;DR
> **Summary**: Preserve current local provider-related work on a dedicated backup branch, hard-reset local `main` to `origin/main`, then prepare a precise reintegration map for selected OpenRouter/custom-provider changes.
> **Deliverables**:
> - Snapshot branch `openrouter-mainline-sync` with backup commit (excluding `android-sdk/`)
> - Local `main` realigned to `origin/main`
> - Reintegration prep artifacts (commit inventory, conflict map, cherry-pick plan)
> **Effort**: Short
> **Parallel**: NO
> **Critical Path**: Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8

## Context
### Original Request
Create a branch from current local `main` (OpenRouter-related local changes), pull upstream changes from `origin/main`, get onto updated mainline, and then prepare to merge minimal provider changes later.

### Interview Summary
- Preservation method: **Backup branch + commit**
- Backup branch name: **`openrouter-mainline-sync`**
- Scope: **Sync + merge-prep** (no provider refactor execution yet)
- Realignment method: **Reset local `main` to `origin/main`**
- Snapshot policy: **Exclude `android-sdk/`**

### Metis Review (gaps addressed)
- Subagent consultation was attempted but blocked by credit constraint.
- Gap handling done via local self-review with explicit guardrails:
  - Prevent data loss before reset
  - Prevent accidental inclusion of `android-sdk/` and planning artifacts
  - Require commit/divergence proof before destructive git actions

## Work Objectives
### Core Objective
Safely move the repository to upstream `origin/main` while preserving and documenting local OpenRouter/provider customizations for controlled reintegration.

### Deliverables
1. Snapshot backup branch at current state: `openrouter-mainline-sync`
2. Snapshot commit containing intended local work (excluding `android-sdk/`)
3. Local `main` exactly matching `origin/main`
4. Reintegration prep package:
   - Local-only commit inventory
   - File hotspot map for provider-related areas
   - Ordered cherry-pick/reapply strategy with conflict expectations

### Definition of Done (verifiable conditions with commands)
- `git branch --list openrouter-mainline-sync` returns branch name.
- `git log --oneline openrouter-mainline-sync -n 5` includes snapshot commit.
- `git rev-parse main` equals `git rev-parse origin/main`.
- `git status --short --branch` on `main` shows clean tree and no ahead/behind divergence.
- Reintegration prep document exists at `.sisyphus/evidence/reintegration-prep.md` with commit/file/conflict sections completed.

### Must Have
- No loss of current uncommitted work.
- `android-sdk/` excluded from snapshot commit.
- Mainline reset performed only after backup proof.
- Reintegration plan anchored to actual provider integration files.

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- Must NOT run `git reset --hard origin/main` before backup branch + snapshot commit are verified.
- Must NOT include `android-sdk/` in snapshot commit.
- Must NOT perform provider refactor implementation in this plan.
- Must NOT merge directly from backup branch into `main` without reintegration prep.
- Must NOT push forcefully to remote.

## Verification Strategy
> ZERO HUMAN INTERVENTION — all verification is agent-executed.
- Test decision: **tests-after** (git-state and file-diff verification via Bash; no app test framework needed for this scope)
- QA policy: Every task includes happy + failure/edge scenario with binary outcomes.
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
Wave 1: Safety and snapshot foundation
- Task 1: Preflight repository safety audit
- Task 2: Create backup branch `openrouter-mainline-sync`
- Task 3: Create snapshot commit (exclude `android-sdk/`)

Wave 2: Mainline realignment
- Task 4: Fetch/prune and verify upstream graph
- Task 5: Reset local `main` to `origin/main`
- Task 6: Post-reset validation and baseline capture

Wave 3: Reintegration preparation
- Task 7: Build local-change inventory from backup branch
- Task 8: Create reintegration playbook (ordered cherry-pick plan + conflict matrix)

### Dependency Matrix (full, all tasks)
- 1: Blocks 2,3,4,5,6,7,8
- 2: Blocked by 1 | Blocks 3,7,8
- 3: Blocked by 2 | Blocks 5
- 4: Blocked by 1 | Blocks 5
- 5: Blocked by 3,4 | Blocks 6,7,8
- 6: Blocked by 5 | Blocks 8
- 7: Blocked by 2,5 | Blocks 8
- 8: Blocked by 6,7

### Agent Dispatch Summary (wave → task count → categories)
- Wave 1 → 3 tasks → `quick`
- Wave 2 → 3 tasks → `quick`
- Wave 3 → 2 tasks → `quick`

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task includes Agent Profile + Parallelization + QA Scenarios.

- [ ] 1. Run preflight safety audit before any branch mutations

  **What to do**:
  - Capture current git state with:
    - `git status --short --branch`
    - `git branch -vv`
    - `git log --oneline --decorate --graph --max-count=20 --all`
  - Save output to `.sisyphus/evidence/task-1-preflight.txt`.
  - Confirm current local-only commits and dirty files are visible in evidence.

  **Must NOT do**:
  - Do not change files.
  - Do not run reset/rebase/checkout yet.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: read-only repo audit commands.
  - Skills: []
  - Omitted: [`git-master`] — not required for this simple audit capture.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 2-8 | Blocked By: none

  **References**:
  - `admin/server.js` — currently modified locally.
  - `app/src/main/java/com/thinkoff/clawwatch/ConfigSyncService.kt` — currently modified locally.
  - `app/src/main/java/com/thinkoff/clawwatch/MainActivity.kt` — currently modified locally.
  - `app/src/main/res/layout/activity_main.xml` — currently modified locally.

  **Acceptance Criteria**:
  - [ ] Evidence file exists and includes branch divergence plus dirty file list.
  - [ ] Evidence shows local commit(s) ahead of `origin/main`.

  **QA Scenarios**:
  ```
  Scenario: Happy path preflight evidence capture
    Tool: Bash
    Steps:
      1) Run: git status --short --branch
      2) Run: git branch -vv
      3) Run: git log --oneline --decorate --graph --max-count=20 --all
      4) Save outputs into .sisyphus/evidence/task-1-preflight.txt
    Expected: Evidence file contains current branch, ahead count, dirty files.
    Evidence: .sisyphus/evidence/task-1-preflight.txt

  Scenario: Failure guard when repo data missing
    Tool: Bash
    Steps:
      1) If any command fails, capture stderr to .sisyphus/evidence/task-1-preflight-error.txt
      2) Abort workflow before Task 2
    Expected: Error evidence exists and no branch/reset action has been executed.
    Evidence: .sisyphus/evidence/task-1-preflight-error.txt
  ```

  **Commit**: NO | Message: `n/a` | Files: `n/a`

- [ ] 2. Create backup branch `openrouter-mainline-sync` from current local main

  **What to do**:
  - Ensure currently checked-out branch is `main`.
  - Create branch: `git checkout -b openrouter-mainline-sync`.
  - Verify with `git branch --list openrouter-mainline-sync` and `git rev-parse --abbrev-ref HEAD`.
  - Capture evidence to `.sisyphus/evidence/task-2-branch-created.txt`.

  **Must NOT do**:
  - Do not switch to any upstream branch yet.
  - Do not commit/reset in this task.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: single safe git branch operation.
  - Skills: []
  - Omitted: [`git-master`] — not required.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 3,7,8 | Blocked By: 1

  **References**:
  - Branch name decision: `openrouter-mainline-sync`.

  **Acceptance Criteria**:
  - [ ] Current branch equals `openrouter-mainline-sync`.
  - [ ] Branch list contains `openrouter-mainline-sync`.

  **QA Scenarios**:
  ```
  Scenario: Happy path branch creation
    Tool: Bash
    Steps:
      1) Run: git checkout -b openrouter-mainline-sync
      2) Run: git rev-parse --abbrev-ref HEAD
      3) Run: git branch --list openrouter-mainline-sync
    Expected: HEAD and listing both show openrouter-mainline-sync.
    Evidence: .sisyphus/evidence/task-2-branch-created.txt

  Scenario: Edge case branch already exists
    Tool: Bash
    Steps:
      1) If checkout -b fails with "already exists", run: git checkout openrouter-mainline-sync
      2) Capture message/output
    Expected: Workflow continues on openrouter-mainline-sync without creating duplicate.
    Evidence: .sisyphus/evidence/task-2-branch-exists.txt
  ```

  **Commit**: NO | Message: `n/a` | Files: `n/a`

- [ ] 3. Snapshot local state on backup branch (exclude `android-sdk/`)

  **What to do**:
  - Stage intended changes only; explicitly exclude `android-sdk/` and `.sisyphus/`.
  - Validate staged set with `git diff --cached --name-status`.
  - Commit snapshot with message:
    - `chore(snapshot): preserve local openrouter/provider state before mainline reset`
  - Record commit hash and staged file list in `.sisyphus/evidence/task-3-snapshot-commit.txt`.

  **Must NOT do**:
  - Must NOT include `android-sdk/`.
  - Must NOT include `.sisyphus/` planning artifacts.
  - Must NOT amend earlier history.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: straightforward snapshot commit with strict include/exclude.
  - Skills: []
  - Omitted: [`git-master`] — not required.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 5 | Blocked By: 2

  **References**:
  - Modified files identified in preflight evidence.

  **Acceptance Criteria**:
  - [ ] Snapshot commit exists on `openrouter-mainline-sync`.
  - [ ] Snapshot commit file list excludes `android-sdk/` and `.sisyphus/`.

  **QA Scenarios**:
  ```
  Scenario: Happy path snapshot commit
    Tool: Bash
    Steps:
      1) Stage desired files only (exclude android-sdk/ and .sisyphus/)
      2) Run: git diff --cached --name-status
      3) Commit with required message
      4) Run: git log --oneline -n 1
    Expected: New commit created with only intended project files.
    Evidence: .sisyphus/evidence/task-3-snapshot-commit.txt

  Scenario: Failure if excluded paths are staged
    Tool: Bash
    Steps:
      1) Check staged list for ^android-sdk/ or ^.sisyphus/
      2) If present, unstage excluded paths and revalidate before commit
    Expected: Commit blocked until staged list is clean.
    Evidence: .sisyphus/evidence/task-3-snapshot-exclusion-check.txt
  ```

  **Commit**: YES | Message: `chore(snapshot): preserve local openrouter/provider state before mainline reset` | Files: selected working-tree files only

- [ ] 4. Fetch upstream and verify realignment target

  **What to do**:
  - Run `git fetch origin --prune`.
  - Capture `git log --oneline --decorate --graph --max-count=30 --all` and `git rev-parse origin/main`.
  - Save to `.sisyphus/evidence/task-4-fetch-verify.txt`.

  **Must NOT do**:
  - Do not reset any branch in this task.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: one fetch + state verification.
  - Skills: []
  - Omitted: [`git-master`] — not required.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 5 | Blocked By: 1

  **References**:
  - Remote: `origin https://github.com/SuperiorByteWorks-LLC/ClawWatch.git`

  **Acceptance Criteria**:
  - [ ] Fetch completes successfully.
  - [ ] Evidence contains latest `origin/main` hash.

  **QA Scenarios**:
  ```
  Scenario: Happy path fetch and target capture
    Tool: Bash
    Steps:
      1) Run: git fetch origin --prune
      2) Run: git rev-parse origin/main
      3) Run: git log --oneline --decorate --graph --max-count=30 --all
    Expected: origin/main resolves and graph output includes fetched refs.
    Evidence: .sisyphus/evidence/task-4-fetch-verify.txt

  Scenario: Network/auth failure handling
    Tool: Bash
    Steps:
      1) If fetch fails, capture stderr
      2) Stop before Task 5
    Expected: No reset attempted without successful fetch.
    Evidence: .sisyphus/evidence/task-4-fetch-error.txt
  ```

  **Commit**: NO | Message: `n/a` | Files: `n/a`

- [ ] 5. Reset local main to exact upstream main

  **What to do**:
  - Checkout main: `git checkout main`.
  - Hard reset: `git reset --hard origin/main`.
  - Verify equality via:
    - `git rev-parse main`
    - `git rev-parse origin/main`
  - Save verification in `.sisyphus/evidence/task-5-reset-main.txt`.

  **Must NOT do**:
  - Must NOT run this task unless Task 3 snapshot commit is confirmed.
  - Must NOT force-push anything.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: deterministic git realignment step.
  - Skills: []
  - Omitted: [`git-master`] — not required.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 6,7,8 | Blocked By: 3,4

  **References**:
  - Backup branch: `openrouter-mainline-sync`
  - Upstream branch: `origin/main`

  **Acceptance Criteria**:
  - [ ] `main` and `origin/main` hashes are identical.
  - [ ] `git status --short --branch` on `main` shows clean state.

  **QA Scenarios**:
  ```
  Scenario: Happy path hard reset
    Tool: Bash
    Steps:
      1) Run: git checkout main
      2) Run: git reset --hard origin/main
      3) Compare: git rev-parse main and git rev-parse origin/main
      4) Run: git status --short --branch
    Expected: Hashes match and working tree is clean.
    Evidence: .sisyphus/evidence/task-5-reset-main.txt

  Scenario: Guard fail if snapshot commit missing
    Tool: Bash
    Steps:
      1) Check: git log openrouter-mainline-sync --oneline -n 1
      2) If missing expected snapshot, abort reset and capture output
    Expected: Reset is not run without snapshot proof.
    Evidence: .sisyphus/evidence/task-5-snapshot-guard.txt
  ```

  **Commit**: NO | Message: `n/a` | Files: `n/a`

- [ ] 6. Validate post-reset baseline and preserve proof

  **What to do**:
  - On `main`, capture:
    - `git status --short --branch`
    - `git log --oneline --decorate -n 10`
  - Save to `.sisyphus/evidence/task-6-post-reset-baseline.txt`.
  - Confirm local-only commits are no longer on `main` but remain on `openrouter-mainline-sync`.

  **Must NOT do**:
  - Do not cherry-pick/merge yet.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: read-only verification.
  - Skills: []
  - Omitted: [`git-master`] — not required.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 8 | Blocked By: 5

  **References**:
  - Local-only commits previously observed: `0967440`, `338f97c`, `beb80d6`.

  **Acceptance Criteria**:
  - [ ] Baseline evidence proves clean, upstream-aligned `main`.
  - [ ] Evidence shows backup branch retains local custom history.

  **QA Scenarios**:
  ```
  Scenario: Happy path baseline verification
    Tool: Bash
    Steps:
      1) Run baseline status/log on main
      2) Run: git log --oneline openrouter-mainline-sync -n 10
      3) Compare expected local commits visibility
    Expected: main is aligned; backup branch still contains local history.
    Evidence: .sisyphus/evidence/task-6-post-reset-baseline.txt

  Scenario: Edge case missing backup history
    Tool: Bash
    Steps:
      1) If backup branch does not show expected commits, capture mismatch
      2) Block further reintegration prep until mismatch resolved
    Expected: No reintegration plan produced from uncertain source.
    Evidence: .sisyphus/evidence/task-6-history-mismatch.txt
  ```

  **Commit**: NO | Message: `n/a` | Files: `n/a`

- [ ] 7. Produce local-change inventory for reintegration

  **What to do**:
  - Generate commit list unique to backup branch vs main:
    - `git log --oneline main..openrouter-mainline-sync`
  - Generate file-level diff summary:
    - `git diff --name-status main...openrouter-mainline-sync`
  - Save both into `.sisyphus/evidence/task-7-inventory.txt`.

  **Must NOT do**:
  - Do not apply/cherry-pick commits in this task.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: analysis-only git diff/log inventory.
  - Skills: []
  - Omitted: [`git-master`] — not required.

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: 8 | Blocked By: 2,5

  **References**:
  - Provider hotspot files from research:
    - `app/src/main/java/com/thinkoff/clawwatch/ClawRunner.kt`
    - `admin/server.js`
    - `admin/index.html`
    - `app/src/main/java/com/thinkoff/clawwatch/MainActivity.kt`
    - `app/src/main/java/com/thinkoff/clawwatch/ConfigSyncService.kt`
    - `app/src/main/assets/nullclaw.json.example`
    - `set_key.sh`

  **Acceptance Criteria**:
  - [ ] Inventory evidence includes unique commits and changed file set.
  - [ ] Inventory flags provider-related files explicitly.

  **QA Scenarios**:
  ```
  Scenario: Happy path inventory generation
    Tool: Bash
    Steps:
      1) Run log and name-status commands for main...backup branch
      2) Save outputs to evidence file
      3) Check that provider hotspot file names appear when relevant
    Expected: Evidence is sufficient to guide selective reintegration.
    Evidence: .sisyphus/evidence/task-7-inventory.txt

  Scenario: Edge case empty diff
    Tool: Bash
    Steps:
      1) If diff/log outputs are empty, capture output and stop reintegration planning
    Expected: Plan flags no-op reintegration instead of fabricating steps.
    Evidence: .sisyphus/evidence/task-7-empty-diff.txt
  ```

  **Commit**: NO | Message: `n/a` | Files: `n/a`

- [ ] 8. Create reintegration playbook for minimal provider changes (no execution)

  **What to do**:
  - Write `.sisyphus/evidence/reintegration-prep.md` containing:
    1) **Selected-commit candidates** (from Task 7 inventory)
    2) **Cherry-pick order recommendation** (minimal risk sequence)
    3) **Conflict hotspot matrix** for provider surfaces:
       - `ClawRunner.kt` abstraction and provider enum/config
       - `admin/server.js` provider validation list
       - `admin/index.html` provider/model UI mappings
       - `MainActivity.kt` provider selection mappings
       - `ConfigSyncService.kt` provider key sync coverage
    4) **Out-of-scope hold list** (Matrix client and Anthropic-removal refactor deferred)
  - Include exact proposed command block for future execution (not run now):
    - `git checkout -b openrouter-reintegrate main`
    - `git cherry-pick <hash1> <hash2> ...`
    - conflict resolution checkpoints per hotspot file.

  **Must NOT do**:
  - Do not run cherry-pick/merge commands.
  - Do not alter source files.

  **Recommended Agent Profile**:
  - Category: `quick` — Reason: documentation artifact using gathered evidence.
  - Skills: []
  - Omitted: [`git-master`] — not required.

  **Parallelization**: Can Parallel: NO | Wave 3 | Blocks: none | Blocked By: 6,7

  **References**:
  - Inventory: `.sisyphus/evidence/task-7-inventory.txt`
  - Current plan: `.sisyphus/plans/main-origin-sync-openrouter.md`

  **Acceptance Criteria**:
  - [ ] `reintegration-prep.md` exists with all 4 required sections.
  - [ ] Contains explicit ordered command plan for later reintegration.
  - [ ] Clearly marks Matrix client work as deferred.

  **QA Scenarios**:
  ```
  Scenario: Happy path reintegration playbook completeness
    Tool: Read
    Steps:
      1) Read .sisyphus/evidence/reintegration-prep.md
      2) Verify sections: candidates, order, conflict matrix, deferred scope
      3) Verify command block exists but is marked "not executed"
    Expected: Playbook is decision-complete for follow-up execution.
    Evidence: .sisyphus/evidence/task-8-playbook-check.txt

  Scenario: Edge case missing hotspot coverage
    Tool: Read
    Steps:
      1) Validate each hotspot file appears in conflict matrix
      2) If missing, add before task closure
    Expected: No provider-critical file omitted from reintegration prep.
    Evidence: .sisyphus/evidence/task-8-hotspot-gap.txt
  ```

  **Commit**: NO | Message: `n/a` | Files: `n/a`

## Final Verification Wave (MANDATORY — after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> Do NOT auto-proceed after verification.

- [ ] F1. Plan Compliance Audit — oracle
- [ ] F2. Code Quality Review — unspecified-high
- [ ] F3. Real Manual QA — unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check — deep

## Commit Strategy
- One snapshot commit only in this scope:
  - `chore(snapshot): preserve local openrouter/provider state before mainline reset`
- No commit on reset/verification/inventory tasks.
- Reintegration commits deferred to follow-up execution branch.

## Success Criteria
- Local `main` is fully aligned with `origin/main` and clean.
- Backup branch safely contains prior local provider work.
- Reintegration prep is concrete enough to execute without new judgment calls.
- Matrix-client and Anthropic-removal refactor are explicitly deferred, not mixed into sync phase.
