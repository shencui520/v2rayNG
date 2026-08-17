# reverse-mark

- **Browsing node**: selected server (`SELECTED_SERVER`).
- **Reverse node**: global mark (`REVERSE_SERVER` via `ReverseServerMark`).

Marking a VLESS node as reverse uses **that node's own UUID** (the ID field).
Only the home LAN IP/CIDR is extra input.

## Usage

1. Add VLESS nodes as usual.
2. Open the home reverse VLESS → enable **Mark as reverse node** → set home LAN CIDR → save.
3. Select any other node for normal browsing; reverse stays on the marked node.
