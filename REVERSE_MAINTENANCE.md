# v2rayNG Reverse maintenance

This branch keeps the Reverse feature as a small overlay on top of upstream
[2dust/v2rayNG](https://github.com/2dust/v2rayNG).

## Branches

- `upstream-master`: clean upstream mirror.
- `reverse-next`: integration branch for verified upstream updates.
- `master`: stable Reverse release.
- `reverse-legacy-2.3.3`: backup of the first working implementation.

## Preserved behavior

When Reverse is disabled, v2rayNG produces its normal runtime configuration.
When enabled on an existing VLESS profile, the overlay:

1. preserves the original proxy outbound, DNS, subscriptions and user routes;
2. creates a second VLESS outbound using the Reverse UUID while reusing the
   node address, transport, TLS, fingerprint and flow;
3. sets `settings.reverse.tag` to `reverse-in` and disables mux;
4. adds a constrained `home-direct` Freedom outbound for the configured IP/CIDR;
5. prepends only `reverse-in -> home-direct` to the generated routing rules.

A normal routing rule such as `192.168.5.0/24 -> proxy` also prevents Android's
generic LAN bypass from consuming that route before it reaches Xray.

## Updating from upstream

Run **Sync upstream and validate Reverse** manually from the Actions page and
select the `reverse-next` branch. The workflow merges the requested upstream
ref, preserves this repository's manual-only workflows, runs the Reverse
contract tests, compiles arm64-v8a, and opens a draft PR. It never modifies
`master` automatically.

## Building

Run **Build VLESS Reverse arm64 APK** manually.

- `debug` is for installation testing.
- `release` requires these repository secrets:
  - `REVERSE_KEYSTORE_BASE64`
  - `REVERSE_KEYSTORE_PASSWORD`
  - `REVERSE_KEY_ALIAS`
  - `REVERSE_KEY_PASSWORD`

Keep the same release key for every build so Android can install updates without
removing the existing Reverse app and its data.
