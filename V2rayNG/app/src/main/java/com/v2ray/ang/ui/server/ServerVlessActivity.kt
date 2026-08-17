package com.v2ray.ang.ui.server

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.feature.reverse.VlessReverseEditorState
import com.v2ray.ang.feature.reverse.VlessReverseFields
import com.v2ray.ang.feature.reverse.VlessReverseValidator
import com.v2ray.ang.feature.reverse.resolvedVlessReverseOptions
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.compose.FormDropdownField
import com.v2ray.ang.ui.compose.FormTextField

class ServerVlessActivity : BaseServerActivity() {

    override val serverConfigType: EConfigType = EConfigType.VLESS

    @Composable
    override fun ScreenContent() {
        val options = rememberFieldOptions()
        val scope = rememberCoroutineScope()
        val uiState = rememberSaveable(saver = ServerUiState.Saver) {
            ServerUiState.from(
                initialConfig = initialConfig
            )
        }.apply {
            configType = EConfigType.VLESS
        }
        val reverseState = rememberSaveable(saver = VlessReverseEditorState.Saver) {
            VlessReverseEditorState.from(initialConfig).also { state ->
                // Reflect global mark when opening editor
                if (editGuid.isNotEmpty() && editGuid == MmkvManager.getReverseServer()) {
                    state.enabled = true
                }
            }
        }
        val flowOptions = stringArrayResource(R.array.flows).toList()

        ServerEditorScaffold(
            title = serverConfigType.toString(),
            onSaveClick = {
                val savedGuid = saveServer(uiState, reverseState::applyTo) ?: return@ServerEditorScaffold
                // Global reverse mark is independent of the selected browsing node.
                if (reverseState.enabled) {
                    MmkvManager.setReverseServer(savedGuid)
                } else if (MmkvManager.getReverseServer() == savedGuid) {
                    MmkvManager.setReverseServer(null)
                }
            }
        ) {
            CommonBasicFields(uiState)
            VlessProtocolFields(uiState, flowOptions)
            VlessReverseFields(reverseState)
            CommonNetworkFields(uiState, options)
            CommonStreamSecurityFields(
                state = uiState,
                options = options,
                scope = scope,
                buildProfileItem = {
                    reverseState.applyTo(uiState.toProfileItem(initialConfig))
                }
            )
        }
    }

    override fun validateProtocolConfig(config: ProfileItem): Boolean {
        if (config.password.isNullOrBlank()) {
            toast(R.string.server_lab_id)
            return false
        }
        val reverse = config.resolvedVlessReverseOptions()
        // Only validate reverse fields when this profile is marked as reverse.
        if (reverse?.enabled == true) {
            if (!VlessReverseValidator.hasValidUuid(reverse)) {
                toast(R.string.server_lab_reverse_id)
                return false
            }
            if (!VlessReverseValidator.hasValidTarget(reverse)) {
                toast(R.string.server_lab_reverse_ip)
                return false
            }
        }
        return true
    }

    @Composable
    private fun VlessProtocolFields(
        state: ServerUiState,
        flowOptions: List<String>
    ) {
        FormTextField(
            stringResource(R.string.server_lab_id),
            state.password,
            { state.password = it }
        )
        FormTextField(
            stringResource(R.string.server_lab_encryption),
            state.encryption,
            { state.encryption = it }
        )
        FormDropdownField(
            stringResource(R.string.server_lab_flow),
            state.flow,
            flowOptions,
            { state.flow = it }
        )
    }
}
