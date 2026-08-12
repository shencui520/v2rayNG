package com.v2ray.ang.feature.reverse

import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.GEOIP_PRIVATE
import com.v2ray.ang.AppConfig.TAG_DIRECT
import com.v2ray.ang.dto.entities.RulesetItem
import com.v2ray.ang.util.Utils

/**
 * An explicit private CIDR -> non-direct rule must reach Xray before Android's
 * generic LAN bypass can consume it.
 */
object PrivateRouteCapturePolicy {
    fun shouldRoutePrivateTrafficThroughCore(rulesets: List<RulesetItem>?): Boolean =
        rulesets
            ?.asSequence()
            ?.filter { it.enabled && it.outboundTag != TAG_DIRECT }
            ?.flatMap { it.ip.orEmpty().asSequence() }
            ?.any(::isPrivateIpRoute)
            ?: false

    internal fun isPrivateIpRoute(value: String): Boolean {
        val route = value.trim()
        if (route == GEOIP_PRIVATE || route.endsWith(":private")) return true

        val address = route.substringBefore('/')
        if (!Utils.isIpAddress(address)) return false
        return AppConfig.PRIVATE_IP_LIST.any { privateCidr ->
            Utils.isIpInCidr(address, privateCidr)
        }
    }
}
