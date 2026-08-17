package com.v2ray.ang.feature.reverse

import com.v2ray.ang.util.Utils

object VlessReverseValidator {
    /** UUID may be blank in options; the node's password is used at runtime. */
    fun hasValidUuid(options: VlessReverseOptions?): Boolean = true

    fun hasValidTarget(options: VlessReverseOptions?): Boolean =
        options?.enabled != true || isValidIpOrCidr(options.targetIp)

    fun requireValid(options: VlessReverseOptions, tunnelUuid: String = options.uuid) {
        require(tunnelUuid.isNotBlank()) { "VLESS Reverse UUID is empty" }
        require(isValidIpOrCidr(options.targetIp)) {
            "Invalid VLESS Reverse target IP/CIDR: ${options.targetIp}"
        }
    }

    fun isValidIpOrCidr(value: String?): Boolean {
        val target = value?.trim().orEmpty()
        val parts = target.split('/')
        if (target.isEmpty() || parts.size !in 1..2 || !Utils.isIpAddress(parts.first())) return false
        if (parts.size == 1) return true

        val prefix = parts[1].toIntOrNull() ?: return false
        val maximumPrefix = if (parts.first().contains(':')) 128 else 32
        return prefix in 0..maximumPrefix
    }
}
