package com.v2ray.ang.feature.reverse

import com.tencent.mmkv.MMKV

/**
 * Global mark for the reverse node, independent of SELECTED_SERVER.
 * Kept in feature/reverse so upstream MmkvManager merges stay smaller.
 */
object ReverseServerMark {
    private const val ID_MAIN = "MAIN"
    private const val KEY_REVERSE_SERVER = "REVERSE_SERVER"

    private fun storageOrNull(): MMKV? =
        try {
            MMKV.mmkvWithID(ID_MAIN, MMKV.MULTI_PROCESS_MODE)
        } catch (_: Throwable) {
            // Unit tests / early process stages may not have MMKV initialized.
            null
        }

    fun get(): String? =
        try {
            storageOrNull()?.decodeString(KEY_REVERSE_SERVER)?.takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        }

    fun set(guid: String?) {
        val storage = storageOrNull() ?: return
        try {
            if (guid.isNullOrBlank()) {
                storage.remove(KEY_REVERSE_SERVER)
            } else {
                storage.encode(KEY_REVERSE_SERVER, guid)
            }
        } catch (_: Throwable) {
            // Ignore when MMKV is unavailable.
        }
    }

    fun clearIfMatches(guid: String) {
        if (get() == guid) set(null)
    }
}
