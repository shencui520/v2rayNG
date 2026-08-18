# ci-reverse (Action-only)

Branch purpose: **GitHub Actions only** — take upstream `2dust/v2rayNG` source, keep current Reverse (mark mode) features, validate, and compile.

Feature parity: same as `reverse-mark`
- Global reverse mark (`ReverseServerMark`)
- Browsing node freely selectable
- Node UUID used as reverse tunnel id
- App name: `v2rayNG Reverse`
- List badge: 反代 / REV

## Workflows (manual `workflow_dispatch` only)

| Workflow | Use |
|----------|-----|
| **Adapt upstream to Reverse and build** | Merge `2dust/v2rayNG@ref` into this branch’s Reverse code → tests → arm64 APK → draft PR to `ci-reverse` |
| **Build VLESS Reverse arm64 APK** | Build current tree only (no upstream merge) |

## Inputs

- `upstream_ref`: branch / tag / commit of 2dust/v2rayNG (default `master`)
- `build_type`: `debug` or `release` (release needs keystore secrets)

## After a successful adapt run

1. Download APK from Actions artifacts
2. Review draft PR `sync/upstream-*` → merge into `ci-reverse` (and optionally into `reverse-mark`)
