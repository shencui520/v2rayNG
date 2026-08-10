package com.v2ray.ang.core

import com.v2ray.ang.dto.V2rayConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VlessReverseConfigTest {
    @Test
    fun `reverse injection preserves existing outbounds and user routes`() {
        val config = V2rayConfig(
            log = V2rayConfig.LogBean(),
            inbounds = arrayListOf(),
            outbounds = arrayListOf(
                V2rayConfig.OutboundBean(tag = "proxy", protocol = "vless"),
                V2rayConfig.OutboundBean(tag = "direct", protocol = "freedom"),
            ),
            routing = V2rayConfig.RoutingBean(
                domainStrategy = "AsIs",
                rules = arrayListOf(V2rayConfig.RoutingBean.RulesBean(outboundTag = "proxy")),
            ),
        )
        val reverse = V2rayConfig.OutboundBean(
            protocol = "vless",
            settings = V2rayConfig.OutboundBean.OutSettingsBean(id = "reverse-uuid"),
        )

        assertTrue(CoreConfigManager.appendVlessReverse(config, reverse, "192.168.5.0/24"))

        assertEquals(4, config.outbounds.size)
        assertEquals("proxy", config.outbounds[0].tag)
        assertEquals("reverse", reverse.tag)
        assertEquals(false, reverse.mux?.enabled)
        assertEquals(-1, reverse.mux?.concurrency)
        assertEquals("reverse-in", reverse.settings?.reverse?.tag)
        assertEquals("home-direct", config.outbounds[3].tag)
        assertEquals("freedom", config.outbounds[3].protocol)
        assertEquals("allow", config.outbounds[3].settings?.finalRules?.single()?.action)
        assertEquals("tcp,udp", config.outbounds[3].settings?.finalRules?.single()?.network)
        assertEquals(listOf("192.168.5.0/24"), config.outbounds[3].settings?.finalRules?.single()?.ip)
        assertEquals(listOf("reverse-in"), config.routing.rules.first().inboundTag)
        assertEquals("home-direct", config.routing.rules.first().outboundTag)
        assertEquals("proxy", config.routing.rules[1].outboundTag)
    }

    @Test
    fun `reverse injection never replaces an existing reverse outbound`() {
        val config = V2rayConfig(
            log = V2rayConfig.LogBean(),
            inbounds = arrayListOf(),
            outbounds = arrayListOf(V2rayConfig.OutboundBean(tag = "reverse", protocol = "vless")),
            routing = V2rayConfig.RoutingBean(domainStrategy = "AsIs", rules = arrayListOf()),
        )

        assertFalse(CoreConfigManager.appendVlessReverse(config, V2rayConfig.OutboundBean(protocol = "vless")))
        assertEquals(1, config.outbounds.size)
        assertTrue(config.routing.rules.isEmpty())
        assertNotNull(config.outbounds.first())
    }
}
