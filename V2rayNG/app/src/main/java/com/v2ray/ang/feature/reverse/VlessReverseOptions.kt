package com.v2ray.ang.feature.reverse

import com.v2ray.ang.dto.entities.ProfileItem

data class VlessReverseOptions(
    var enabled: Boolean = false,
    /** Optional override; blank means use the node's own VLESS UUID (password). */
    var uuid: String = "",
    var targetIp: String = DEFAULT_TARGET_IP,
) {
    companion object {
        const val DEFAULT_TARGET_IP = "192.168.5.0/24"
    }
}

/**
 * Read the new nested options first, then transparently fall back to profiles
 * written by the first Reverse build.
 */
fun ProfileItem.resolvedVlessReverseOptions(): VlessReverseOptions? {
    vlessReverse?.let { return it }
    if (reverseEnabled != true) return null
    return VlessReverseOptions(
        enabled = true,
        uuid = reversePassword.orEmpty(),
        targetIp = reverseIp?.takeIf { it.isNotBlank() }
            ?: VlessReverseOptions.DEFAULT_TARGET_IP,
    )
}

/**
 * UUID used for the reverse tunnel. Prefer an explicit reverse uuid when set;
 * otherwise the node's own VLESS id (password).
 */
fun ProfileItem.reverseTunnelUuid(): String {
    val options = resolvedVlessReverseOptions() ?: return ""
    if (!options.enabled) return ""
    return options.uuid.trim().ifBlank { password?.trim().orEmpty() }
}
