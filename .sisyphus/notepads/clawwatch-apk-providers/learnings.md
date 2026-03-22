
## Multi-Provider Implementation (2025-03-06)

### Changes Made to ClawRunner.kt

1. **Added LLMProvider enum** (lines 19-24):
   - ANTHROPIC: Original provider (requires sk-ant-xxx key)
   - OPENCODE_ZEN: Free tier with "public" placeholder (default)
   - NVIDIA: Requires nvapi-xxx key, uses moonshotai/kimi-k2.5 model
   - MOONSHOT: Requires API key, uses kimi-k2.5 model

2. **Added ProviderConfig sealed class** (lines 29-184):
   - Abstract class with apiKey, endpoint, model properties
   - Abstract methods: buildRequestBody(), extractResponseText(), getHeaders()
   - Four data class implementations:
     - AnthropicConfig: Anthropic API format (top-level "system" field)
     - OpenCodeZenConfig: OpenAI-compatible format
     - NvidiaConfig: OpenAI-compatible format
     - MoonshotConfig: OpenAI-compatible format

3. **Added provider preference constants** (lines 207-228):
   - PREF_PROVIDER: Stores selected provider enum name
   - PREF_ANTHROPIC_KEY, PREF_NVIDIA_KEY, PREF_MOONSHOT_KEY: Provider-specific keys
   - DEFAULT_PROVIDER = OPENCODE_ZEN (zero-config default)
   - Provider-specific default models

4. **Added provider accessor methods** (lines 278-362):
   - saveProvider()/getProvider(): Provider selection
   - saveAnthropicKey()/saveNvidiaKey()/saveMoonshotKey(): Provider-specific key storage
   - hasProviderApiKey(): Check if provider has valid key
   - getProviderApiKey()/getApiKeyForProvider(): Get key for specific provider
   - createProviderConfig(): Create ProviderConfig instance for current provider

5. **Refactored HTTP layer** (lines 859-925):
   - callProviderRaw(): Generic HTTP call using ProviderConfig
   - callProviderMessages(): Generic message sending using ProviderConfig
   - Legacy callAnthropicRaw()/callAnthropicMessages(): Backward compatibility wrappers

6. **Updated query methods** (lines 657-693):
   - queryDirect(): Now uses createProviderConfig() + callProviderMessages()
   - queryWithKotlinRag(): Now uses createProviderConfig() + callProviderMessages()
   - queryWithOpusTool(): Kept Anthropic-specific (uses Claude tool_use)

### Backward Compatibility

- Legacy saveApiKey() saves to both old and new Anthropic key preferences
- Legacy getApiKey() delegates to getProviderApiKey()
- Legacy hasApiKey() checks provider-specific key based on current provider
- Existing Anthropic users continue to work without changes

### Build Verification

- ./gradlew :app:compileDebugKotlin: SUCCESS
- Warnings: Unused apiKey parameters in query methods (expected after refactoring)

### API Endpoints

- Anthropic: https://api.anthropic.com/v1/messages
- OpenCode Zen: https://opencode.ai/zen/v1/responses
- NVIDIA: https://integrate.api.nvidia.com/v1/chat/completions
- Moonshot: https://api.moonshot.cn/v1/chat/completions

### Default Models

- Anthropic: claude-opus-4-6
- OpenCode Zen: gpt-5-nano
- NVIDIA: moonshotai/kimi-k2.5
- Moonshot: kimi-k2.5

## Admin Panel Updates (2025-03-06)

### Changes to admin/index.html
1. **Added provider selection dropdown**:
   - Options: OpenCode Zen (default), Anthropic, NVIDIA, Moonshot
   - onchange handler shows/hides appropriate API key inputs

2. **Added conditional API key sections**:
   - anthropicKeySection: shown when ANTHROPIC selected
   - nvidiaKeySection: shown when NVIDIA selected
   - moonshotKeySection: shown when MOONSHOT selected
   - OpenCode Zen requires no key (free)

3. **Updated pushAll() function**:
   - Sends llm_provider to server
   - Sends nvidia_api_key and moonshot_api_key
   - Sends avatar_type

4. **Updated loadFromWatch() function**:
   - Reads llm_provider and updates dropdown
   - Reads all provider-specific API keys
   - Calls updateProviderFields() to show correct inputs

### Changes to admin/server.js
1. **Updated /api/push/settings endpoint**:
   - Accepts llm_provider, nvidia_api_key, moonshot_api_key
   - Validates provider is one of validProviders array
   - Validates each API key format
   - Saves provider-specific keys to watch prefs

2. **Updated /api/prefs endpoint**:
   - Returns llm_provider, nvidia_api_key, moonshot_api_key

### Changes to ConfigSyncService.kt
1. **Updated PATH_CONFIG handler**:
   - Parses llm_provider from JSON → LLMProvider.valueOf()
   - Calls runner.saveProvider() with error handling
   - Handles anthropic_api_key → saveAnthropicKey()
   - Handles nvidia_api_key → saveNvidiaKey()
   - Handles moonshot_api_key → saveMoonshotKey()
   - Updated log message to include provider info

## Summary of All Changes

### Files Modified:
1. app/build.gradle.kts - SDK versions (35, 33, 35)
2. app/src/main/java/com/thinkoff/clawwatch/ClawRunner.kt - Multi-provider support
3. app/src/main/java/com/thinkoff/clawwatch/MainActivity.kt - Provider UI
4. app/src/main/java/com/thinkoff/clawwatch/ConfigSyncService.kt - Provider sync
5. app/src/main/res/layout/activity_main.xml - Provider spinner layout
6. admin/index.html - Admin panel provider selection
7. admin/server.js - Admin API provider support

### Features Implemented:
- OpenCode Zen as default (free, no API key)
- Anthropic support (sk-ant-xxx key)
- NVIDIA support (nvapi-xxx key)
- Moonshot support (API key)
- Provider selection in watch UI
- Provider selection in admin panel
- Provider sync via Wearable Data Layer
- Backward compatibility with existing Anthropic users
