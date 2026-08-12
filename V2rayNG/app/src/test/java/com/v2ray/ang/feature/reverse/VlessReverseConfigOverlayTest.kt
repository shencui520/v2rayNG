package com.v2ray.ang.feature.reverse

import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.util.JsonUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VlessReverseConfigOverlayTest {
    @Test
    fun disabledReverseLeavesGeneratedConfigUntouched() {
        val config = baseConfig()
        val profile = ProfileItem.create(EConfigType.VLESS)

        val result = VlessReverseConfigOverlay.apply(profile, config, null)
        val parsed = JsonUtil.parseString(result)!!.asJsonObject

        assertEquals(2, parsed.getAsJsonArray("outbounds").size())
        assertEquals(1, parsed.getAsJsonObject("routing").getAsJsonArray("rules").size())
    }

    @Test
    fun enabledReverseAppendsOnlyRequiredObjectsAndPreservesUserRoutes() {
        val profile = ProfileItem.create(EConfigType.VLESS).copy(
            vlessReverse = VlessReverseOptions(
                enabled = true,
                uuid = "reverse-uuid",
                targetIp = "192.168.5.0/24",
            )
        )
        val reverseOutbound = V2rayConfig.OutboundBean(
            protocol = "vless",
            settings = V2rayConfig.OutboundBean.OutSettingsBean(id = "reverse-uuid"),
        )

        val result = VlessReverseConfigOverlay.apply(profile, baseConfig(), reverseOutbound)
        val root = JsonUtil.parseString(result)!!.asJsonObject
        val outbounds = root.getAsJsonArray("outbounds")
        val rules = root.getAsJsonObject("routing").getAsJsonArray("rules")

        assertEquals(4, outbounds.size())
        assertEquals("proxy", outbounds[0].asJsonObject["tag"].asString)
        assertEquals("reverse", outbounds[2].asJsonObject["tag"].asString)
        assertFalse(outbounds[2].asJsonObject["mux"].asJsonObject["enabled"].asBoolean)
        assertEquals(
            "reverse-in",
            outbounds[2].asJsonObject["settings"].asJsonObject["reverse"].asJsonObject["tag"].asString,
        )
        assertEquals("home-direct", outbounds[3].asJsonObject["tag"].asString)
        assertEquals(
            "192.168.5.0/24",
            outbounds[3].asJsonObject["settings"].asJsonObject["finalRules"]
                .asJsonArray[0].asJsonObject["ip"].asJsonArray[0].asString,
        )
        assertEquals("home-direct", rules[0].asJsonObject["outboundTag"].asString)
        assertEquals("proxy", rules[1].asJsonObject["outboundTag"].asString)
    }

    @Test
    fun reservedTagCollisionFailsInsteadOfSilentlyDisablingReverse() {
        val profile = ProfileItem.create(EConfigType.VLESS).copy(
            vlessReverse = VlessReverseOptions(true, "reverse-uuid", "192.168.5.0/24")
        )
        val config = baseConfig().apply {
            outbounds.add(V2rayConfig.OutboundBean(tag = "reverse", protocol = "freedom"))
        }

        assertThrows(IllegalArgumentException::class.java) {
            VlessReverseConfigOverlay.apply(
                profile,
                config,
                V2rayConfig.OutboundBean(
                    protocol = "vless",
                    settings = V2rayConfig.OutboundBean.OutSettingsBean(id = "reverse-uuid"),
                ),
            )
        }
    }

    @Test
    fun legacyFlatFieldsRemainReadableDuringMigration() {
        @Suppress("DEPRECATION")
        val profile = ProfileItem.create(EConfigType.VLESS).copy(
            reverseEnabled = true,
            reversePassword = "legacy-uuid",
            reverseIp = "10.0.0.0/8",
        )

        val options = profile.resolvedVlessReverseOptions()
        assertTrue(options!!.enabled)
        assertEquals("legacy-uuid", options.uuid)
        assertEquals("10.0.0.0/8", options.targetIp)
    }

    private fun baseConfig(): V2rayConfig =
        V2rayConfig(
            log = V2rayConfig.LogBean(),
            inbounds = arrayListOf(),
            outbounds = arrayListOf(
                V2rayConfig.OutboundBean(tag = "proxy", protocol = "vless"),
                V2rayConfig.OutboundBean(tag = "direct", protocol = "freedom"),
            ),
            routing = V2rayConfig.RoutingBean(
                domainStrategy = "AsIs",
                rules = arrayListOf(
                    V2rayConfig.RoutingBean.RulesBean(outboundTag = "proxy")
                ),
            ),
        )
}
