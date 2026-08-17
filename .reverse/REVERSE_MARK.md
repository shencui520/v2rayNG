# reverse-mark branch

Based on `reverse-next` with one behavior change:

- **Browsing node**: still the selected server (`SELECTED_SERVER`).
- **Reverse node**: a separate global mark (`REVERSE_SERVER`).

## Usage

1. Add VLESS nodes as usual (manual / subscription / QR).
2. Open the home-side reverse VLESS node → enable **Mark as reverse node** → fill reverse UUID and target IP/CIDR → save.
3. Select any other node as the normal proxy; reverse tunnel stays on the marked node.

## Code touch points

- `MmkvManager.getReverseServer` / `setReverseServer`
- `CoreConfigManager.toConfigResult` uses marked reverse profile for overlay
- `ServerVlessActivity` updates the mark on save
