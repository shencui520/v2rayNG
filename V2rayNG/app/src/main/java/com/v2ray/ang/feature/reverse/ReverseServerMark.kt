package com.v2ray.ang.feature.reverse

import com.tencent.mmkv.MMKV

/**
 * Global mark for the reverse node, independent of SELECTED_SERVER.
 * Kept in feature/reverse so upstream MmkvManager merges stay smaller.
 */
object ReverseServerMark {
    private const val ID_MAIN = "MAIN"
    private const val KEY_REVERSE_SERVER = "REVERSE_SERVER"

    private val mainStorage by lazy { MMKV.mmkvWithID(ID_MAIN, MMKV.MULTI_PROCESS_MODE) }

    fun get(): String? =
        mainStorage.decodeString(KEY_REVERSE_SERVER)?.takeIf { it.isNotBlank() }

    fun set(guid: String?) {
        if (guid.isNullOrBlank()) {
            mainStorage.remove(KEY_REVERSE_SERVER)
        } else {
            mainStorage.encode(KEY_REVERSE_SERVER, guid)
        }
    }

    fun clearIfMatches(guid: String) {
        if (get() == guid) set(null)
    }
}
