package com.v2ray.ang.feature.reverse

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.util.JsonUtil

/**
 * Applies the Reverse feature after v2rayNG has finished its normal dynamic
 * config generation. Existing outbounds, DNS and user routing stay untouched.
 */
object VlessReverseConfigOverlay {
    private const val TAG_REVERSE = "reverse"
    private const val TAG_REVERSE_IN = "reverse-in"
    private const val TAG_HOME_DIRECT = "home-direct"

    fun buildReverseProfile(profile: ProfileItem?): ProfileItem? {
        val options = profile?.resolvedVlessReverseOptions() ?: return null
        if (profile.configType != EConfigType.VLESS || !options.enabled) return null
        VlessReverseValidator.requireValid(options)
        return profile.copy(
            password = options.uuid.trim(),
            vlessReverse = null,
            reverseEnabled = null,
            reversePassword = null,
            reverseIp = null,
        )
    }

    fun apply(
        profile: ProfileItem?,
        config: V2rayConfig,
        reverseOutbound: V2rayConfig.OutboundBean?,
    ): String {
        val options = profile?.resolvedVlessReverseOptions()
        if (profile?.configType != EConfigType.VLESS || options?.enabled != true) {
            return JsonUtil.toJsonPretty(config).orEmpty()
        }
        VlessReverseValidator.requireValid(options)
        requireNotNull(reverseOutbound) { "Failed to create VLESS Reverse outbound" }

        val root = JsonUtil.parseString(JsonUtil.toJson(config))
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: error("Failed to serialize generated Xray configuration")
        val outbounds = root.getAsJsonArray("outbounds")
            ?: JsonArray().also { root.add("outbounds", it) }

        val existingTags = outbounds.mapNotNull { element ->
            element.takeIf { it.isJsonObject }?.asJsonObject
                ?.get("tag")?.takeIf { it.isJsonPrimitive }?.asString
        }
        val collision = listOf(TAG_REVERSE, TAG_HOME_DIRECT).firstOrNull(existingTags::contains)
        require(collision == null) { "Reserved VLESS Reverse tag is already used: $collision" }

        val reverseJson = JsonUtil.parseString(JsonUtil.toJson(reverseOutbound))
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: error("Failed to serialize VLESS Reverse outbound")
        reverseJson.addProperty("tag", TAG_REVERSE)
        reverseJson.add("mux", JsonObject().apply {
            addProperty("enabled", false)
            addProperty("concurrency", -1)
        })
        val reverseSettings = reverseJson.getAsJsonObject("settings")
            ?: JsonObject().also { reverseJson.add("settings", it) }
        reverseSettings.add("reverse", JsonObject().apply {
            addProperty("tag", TAG_REVERSE_IN)
        })
        outbounds.add(reverseJson)
        outbounds.add(homeDirect(options.targetIp.trim()))

        val routing = root.getAsJsonObject("routing")
            ?: JsonObject().also { root.add("routing", it) }
        val existingRules = routing.getAsJsonArray("rules") ?: JsonArray()
        val rules = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("type", "field")
                add("inboundTag", JsonArray().apply { add(TAG_REVERSE_IN) })
                addProperty("outboundTag", TAG_HOME_DIRECT)
            })
            existingRules.forEach(::add)
        }
        routing.add("rules", rules)
        return JsonUtil.toJsonPretty(root).orEmpty()
    }

    private fun homeDirect(targetIp: String): JsonObject =
        JsonObject().apply {
            addProperty("tag", TAG_HOME_DIRECT)
            addProperty("protocol", "freedom")
            add("settings", JsonObject().apply {
                add("finalRules", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("action", "allow")
                        addProperty("network", "tcp,udp")
                        add("ip", JsonArray().apply { add(targetIp) })
                    })
                })
            })
        }
}
