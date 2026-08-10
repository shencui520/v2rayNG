package com.v2ray.ang.ui.server

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.v2ray.ang.R
import com.v2ray.ang.core.HomeNetworkConfigBuilder
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.isHomeNetworkProfile
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.compose.FormDropdownField
import com.v2ray.ang.ui.compose.FormTextField

/**
 * Form editor for the two mobile roles used by VLESS Reverse:
 *
 * - VLESS_REVERSE_HOME runs on the phone at home and establishes the reverse tunnel.
 * - VLESS_ROAM_HOME runs on the phone outside and routes just the home CIDRs to the VPS.
 */
class ServerHomeNetworkActivity : BaseServerActivity() {
    override val serverConfigType: EConfigType by lazy {
        EConfigType.fromInt(intent.getIntExtra("createConfigType", EConfigType.VLESS_REVERSE_HOME.value))
            ?.takeIf { it.isHomeNetworkProfile() }
            ?: EConfigType.VLESS_REVERSE_HOME
    }

    @Composable
    override fun ScreenContent() {
        val options = rememberFieldOptions()
        val scope = rememberCoroutineScope()
        val isReverseHome = serverConfigType == EConfigType.VLESS_REVERSE_HOME
        val uiState = rememberSaveable(saver = ServerUiState.Saver) {
            ServerUiState.from(initialConfig)
        }.apply {
            configType = serverConfigType
        }
        val flowOptions = stringArrayResource(R.array.flows).toList()
        var reverseId by rememberSaveable { mutableStateOf(initialConfig.reversePassword.orEmpty()) }
        var homeCidrs by rememberSaveable {
            mutableStateOf(initialConfig.homeCidrs?.ifBlank { null } ?: "192.168.5.0/24")
        }

        ServerEditorScaffold(
            title = stringResource(
                if (isReverseHome) R.string.title_vless_reverse_home else R.string.title_vless_roam_home
            ),
            onSaveClick = {
                saveHomeProfile(
                    state = uiState,
                    reverseId = reverseId,
                    homeCidrs = homeCidrs,
                    isReverseHome = isReverseHome,
                )
            }
        ) {
            CommonBasicFields(uiState)
            VlessProtocolFields(uiState, flowOptions)
            if (isReverseHome) {
                FormTextField(
                    label = stringResource(R.string.server_lab_reverse_id),
                    value = reverseId,
                    onValueChange = { reverseId = it }
                )
            }
            FormTextField(
                label = stringResource(R.string.server_lab_home_cidrs),
                value = homeCidrs,
                onValueChange = { homeCidrs = it },
                placeholder = stringResource(R.string.server_lab_home_cidrs_hint),
            )
            CommonNetworkFields(uiState, options)
            CommonStreamSecurityFields(
                state = uiState,
                options = options,
                scope = scope,
                buildProfileItem = { uiState.toProfileItem(initialConfig).copy(configType = EConfigType.VLESS) }
            )
        }
    }

    private fun saveHomeProfile(
        state: ServerUiState,
        reverseId: String,
        homeCidrs: String,
        isReverseHome: Boolean,
    ): Boolean {
        if (!validateBasicConfig(state)) return false
        if (state.password.isBlank()) {
            toast(R.string.server_lab_id)
            return false
        }
        if (isReverseHome && reverseId.isBlank()) {
            toast(R.string.server_lab_reverse_id)
            return false
        }
        if (!HomeNetworkConfigBuilder.areValidIpv4Cidrs(homeCidrs)) {
            toast(R.string.toast_home_cidrs_required)
            return false
        }

        val config = state.toProfileItem(initialConfig).copy(
            configType = serverConfigType,
            reversePassword = reverseId.takeIf { isReverseHome },
            homeCidrs = HomeNetworkConfigBuilder.parseHomeCidrs(homeCidrs).joinToString(","),
        )
        if (!validateCommonConfig(config)) return false

        val rawConfig = try {
            HomeNetworkConfigBuilder.build(config)
        } catch (_: Exception) {
            toast(R.string.toast_malformed_json)
            return false
        }

        config.description = AngConfigManager.generateDescription(config)
        if (config.subscriptionId.isEmpty() && !subscriptionId.isNullOrEmpty()) {
            config.subscriptionId = subscriptionId.orEmpty()
        }
        val savedGuid = MmkvManager.encodeServerConfig(editGuid, config)
        MmkvManager.encodeServerRaw(savedGuid, rawConfig)
        toastSuccess(R.string.toast_success)
        ProfileEditorResult.run {
            finishSaved(savedGuid, isRunning)
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
