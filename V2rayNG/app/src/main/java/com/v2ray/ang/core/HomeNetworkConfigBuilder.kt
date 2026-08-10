package com.v2ray.ang.core

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.util.JsonUtil

/**
 * Generates the full Xray configuration for the two VLESS "remote home" profiles.
 *
 * The home-tunnel profile matches the VLESS Reverse design: the normal VLESS
 * outbound keeps v2rayNG's proxy behaviour while a second VLESS outbound registers
 * the reverse connection. The home-access profile is for the roaming device and
 * sends only the selected home CIDRs to its normal proxy outbound.
 */
object HomeNetworkConfigBuilder {
    private const val TAG_DIRECT = "direct"
    private const val TAG_BLOCK = "block"
    private const val TAG_PROXY = "proxy"
    private const val TAG_HOME_DIRECT = "home-direct"
    private const val TAG_REVERSE = "reverse"
    private const val TAG_REVERSE_IN = "reverse-in"

    fun build(config: ProfileItem): String {
        require(config.configType == EConfigType.VLESS_REVERSE_HOME || config.configType == EConfigType.VLESS_ROAM_HOME) {
            "Home network configuration requires a VLESS home profile"
        }

        val cidrs = parseHomeCidrs(config.homeCidrs)
        require(cidrs.isNotEmpty()) { "At least one home network is required" }

        val root = JsonObject().apply {
            addProperty("remarks", config.remarks)
            add("log", JsonObject().apply { addProperty("loglevel", "warning") })
            add("inbounds", localInbounds())
            add("outbounds", outbounds(config, cidrs))
            add("routing", routing(config, cidrs))
        }
        return JsonUtil.toJsonPretty(root) ?: error("Failed to serialize home network configuration")
    }

    fun parseHomeCidrs(value: String?): List<String> =
        value.orEmpty()
            .split(',', '\n', '\r', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun areValidIpv4Cidrs(value: String?): Boolean {
        val cidrs = parseHomeCidrs(value)
        return cidrs.isNotEmpty() && cidrs.all(::isValidIpv4Cidr)
    }

    private fun isValidIpv4Cidr(value: String): Boolean {
        val parts = value.split('/')
        if (parts.size != 2) return false
        val prefix = parts[1].toIntOrNull() ?: return false
        if (prefix !in 0..32) return false
        val octets = parts[0].split('.')
        return octets.size == 4 && octets.all { octet -> octet.toIntOrNull()?.let { it in 0..255 } == true }
    }

    private fun localInbounds() = JsonArray().apply {
        add(JsonObject().apply {
            addProperty("listen", "127.0.0.1")
            addProperty("port", 10808)
            addProperty("protocol", "socks")
            add("settings", JsonObject().apply { addProperty("udp", true) })
            add("sniffing", sniffing("http", "tls", "quic"))
            addProperty("tag", "socks-in")
        })
        add(JsonObject().apply {
            addProperty("listen", "127.0.0.1")
            addProperty("port", 10809)
            addProperty("protocol", "http")
            add("settings", JsonObject())
            add("sniffing", sniffing("http", "tls"))
            addProperty("tag", "http-in")
        })
    }

    private fun outbounds(config: ProfileItem, cidrs: List<String>) = JsonArray().apply {
        add(JsonObject().apply {
            addProperty("protocol", "freedom")
            addProperty("tag", TAG_DIRECT)
        })
        add(JsonObject().apply {
            addProperty("protocol", "blackhole")
            addProperty("tag", TAG_BLOCK)
        })

        if (config.configType == EConfigType.VLESS_REVERSE_HOME) {
            add(JsonObject().apply {
                addProperty("protocol", "freedom")
                addProperty("tag", TAG_HOME_DIRECT)
                add("settings", JsonObject().apply {
                    add("finalRules", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("action", "allow")
                            addProperty("network", "tcp,udp")
                            add("ip", cidrArray(cidrs))
                        })
                    })
                })
            })
        }

        add(vlessOutbound(config, TAG_PROXY))

        if (config.configType == EConfigType.VLESS_REVERSE_HOME) {
            add(vlessOutbound(config.copy(password = config.reversePassword), TAG_REVERSE, TAG_REVERSE_IN))
        }
    }

    private fun vlessOutbound(config: ProfileItem, tag: String, reverseTag: String? = null): JsonObject {
        val vlessProfile = config.copy(configType = EConfigType.VLESS)
        val outbound = CoreOutboundBuilder.convert(vlessProfile)
            ?: error("Failed to create VLESS outbound")
        outbound.tag = tag
        if (reverseTag != null) {
            // VLESS Reverse already maintains its own multiplexed tunnel. Do not apply global Mux.
            outbound.mux = V2rayConfig.OutboundBean.MuxBean(enabled = false)
        }

        val json = JsonParser.parseString(JsonUtil.toJson(outbound)).asJsonObject
        if (reverseTag != null) {
            val settings = json.getAsJsonObject("settings") ?: JsonObject().also { json.add("settings", it) }
            settings.add("reverse", JsonObject().apply { addProperty("tag", reverseTag) })
        }
        return json
    }

    private fun routing(config: ProfileItem, cidrs: List<String>) = JsonObject().apply {
        addProperty("domainStrategy", "IPIfNonMatch")
        add("rules", JsonArray().apply {
            if (config.configType == EConfigType.VLESS_REVERSE_HOME) {
                add(rule(inboundTags = listOf(TAG_REVERSE_IN), outboundTag = TAG_HOME_DIRECT))
            } else {
                // This must precede geoip:private so the selected home LAN is proxied.
                add(rule(ips = cidrs, outboundTag = TAG_PROXY))
            }
            add(rule(ips = listOf("geoip:private", "geoip:cn"), outboundTag = TAG_DIRECT))
            add(rule(domains = listOf("geosite:cn", "geosite:private"), outboundTag = TAG_DIRECT))
            add(rule(network = "tcp,udp", outboundTag = TAG_PROXY))
        })
    }

    private fun rule(
        inboundTags: List<String> = emptyList(),
        ips: List<String> = emptyList(),
        domains: List<String> = emptyList(),
        network: String? = null,
        outboundTag: String,
    ) = JsonObject().apply {
        addProperty("type", "field")
        if (inboundTags.isNotEmpty()) add("inboundTag", stringArray(inboundTags))
        if (ips.isNotEmpty()) add("ip", stringArray(ips))
        if (domains.isNotEmpty()) add("domain", stringArray(domains))
        if (network != null) addProperty("network", network)
        addProperty("outboundTag", outboundTag)
    }

    private fun sniffing(vararg destinations: String) = JsonObject().apply {
        addProperty("enabled", true)
        add("destOverride", stringArray(destinations.asList()))
    }

    private fun cidrArray(cidrs: List<String>) = stringArray(cidrs)

    private fun stringArray(values: List<String>) = JsonArray().apply { values.forEach { add(it) } }
}
