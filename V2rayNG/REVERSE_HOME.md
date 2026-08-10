# VLESS 回家反代

此 fork 在“添加配置”菜单中提供两个 VLESS 配置类型，生成完整 Xray JSON，并保留原有的 Custom Config 导入方式。

## 家里手机 A

选择 **添加 [VLESS 回家反代隧道]**，填写：

- 普通 VLESS 的地址、端口、UUID、加密、流控与 TLS 传输参数；
- 反代隧道 UUID：VPS 入站中带 `reverse.tag` 的专用 UUID；
- 家里网段：例如 `192.168.5.0/24`，可填写多个，以逗号或换行分隔。

保存后会生成普通 `proxy` 出站、带 `reverse.tag: reverse-in` 的 `reverse` 出站，以及只允许所填家里网段的 `home-direct` 出站。普通代理流量仍走 `proxy`。

## 外出手机 B

选择 **添加 [VLESS 回家访问]**，填写普通客户端的 VLESS 参数和相同的家里网段。

生成的首条路由会将该网段送往 `proxy`，并且 Android VPN 模式会接管该私有网段，避免它在到达 Xray 前被系统按 LAN 直连绕过。

## VPS

VPS 保持现有配置：反代专用 VLESS 客户端需要配置 `reverse.tag`，并将目标家里网段路由到该 tag。应用不会修改 VPS 的 Xray 配置。

本功能对应 Xray 的 VLESS Reverse “Remote Return Home”模式；请使用支持 VLESS Reverse 的 Xray core。
