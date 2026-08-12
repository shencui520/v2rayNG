package com.v2ray.ang.feature.reverse

import com.v2ray.ang.dto.entities.RulesetItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateRouteCapturePolicyTest {
    @Test
    fun privateProxyRuleForcesTrafficIntoCore() {
        val rules = listOf(
            RulesetItem(
                ip = listOf("192.168.5.0/24"),
                outboundTag = "proxy",
                enabled = true,
            )
        )
        assertTrue(PrivateRouteCapturePolicy.shouldRoutePrivateTrafficThroughCore(rules))
    }

    @Test
    fun directOrDisabledPrivateRulesDoNotOverrideLanBypass() {
        assertFalse(
            PrivateRouteCapturePolicy.shouldRoutePrivateTrafficThroughCore(
                listOf(RulesetItem(ip = listOf("10.0.0.0/8"), outboundTag = "direct"))
            )
        )
        assertFalse(
            PrivateRouteCapturePolicy.shouldRoutePrivateTrafficThroughCore(
                listOf(
                    RulesetItem(
                        ip = listOf("192.168.5.0/24"),
                        outboundTag = "proxy",
                        enabled = false,
                    )
                )
            )
        )
    }

    @Test
    fun publicProxyRuleDoesNotAffectLanBypass() {
        assertFalse(
            PrivateRouteCapturePolicy.shouldRoutePrivateTrafficThroughCore(
                listOf(RulesetItem(ip = listOf("8.8.8.8/32"), outboundTag = "proxy"))
            )
        )
    }
}
