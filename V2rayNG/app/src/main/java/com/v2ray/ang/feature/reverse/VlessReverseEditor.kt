package com.v2ray.ang.feature.reverse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.ui.compose.FormTextField
import com.v2ray.ang.ui.compose.SettingsSwitchItem
import com.v2ray.ang.util.JsonUtil

class VlessReverseEditorState(
    enabled: Boolean = false,
    targetIp: String = VlessReverseOptions.DEFAULT_TARGET_IP,
) {
    var enabled by mutableStateOf(enabled)
    var targetIp by mutableStateOf(targetIp)

    fun applyTo(profile: ProfileItem): ProfileItem =
        profile.copy(
            vlessReverse = if (enabled) {
                VlessReverseOptions(
                    enabled = true,
                    // Empty uuid => runtime uses the node's own VLESS UUID (password).
                    uuid = "",
                    targetIp = targetIp.trim(),
                )
            } else {
                null
            },
            reverseEnabled = null,
            reversePassword = null,
            reverseIp = null,
        )

    private fun snapshot(): VlessReverseOptions =
        VlessReverseOptions(enabled = enabled, uuid = "", targetIp = targetIp)

    companion object {
        fun from(profile: ProfileItem): VlessReverseEditorState {
            val options = profile.resolvedVlessReverseOptions()
            return VlessReverseEditorState(
                enabled = options?.enabled == true,
                targetIp = options?.targetIp?.takeIf { it.isNotBlank() }
                    ?: VlessReverseOptions.DEFAULT_TARGET_IP,
            )
        }

        val Saver: Saver<VlessReverseEditorState, String> = Saver(
            save = { JsonUtil.toJson(it.snapshot()) },
            restore = { saved ->
                JsonUtil.fromJsonSafe(saved, VlessReverseOptions::class.java)?.let {
                    VlessReverseEditorState(it.enabled, it.targetIp)
                }
            }
        )
    }
}

@Composable
fun VlessReverseFields(state: VlessReverseEditorState) {
    SettingsSwitchItem(
        title = stringResource(R.string.server_lab_reverse_enable),
        summary = stringResource(R.string.server_lab_reverse_summary),
        checked = state.enabled,
        onCheckedChange = { state.enabled = it },
    )
    if (state.enabled) {
        FormTextField(
            stringResource(R.string.server_lab_reverse_ip),
            state.targetIp,
            { state.targetIp = it },
        )
    }
}
