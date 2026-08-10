package com.v2ray.ang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNetworkConfigBuilderTest {
    @Test
    fun parsesDistinctCidrsAcrossSupportedSeparators() {
        assertEquals(
            listOf("192.168.5.0/24", "10.20.0.0/16"),
            HomeNetworkConfigBuilder.parseHomeCidrs("192.168.5.0/24, 10.20.0.0/16\n192.168.5.0/24")
        )
    }

    @Test
    fun validatesOnlyIpv4CidrNetworks() {
        assertTrue(HomeNetworkConfigBuilder.areValidIpv4Cidrs("192.168.5.0/24,10.0.0.0/8"))
        assertFalse(HomeNetworkConfigBuilder.areValidIpv4Cidrs("192.168.5.0/33"))
        assertFalse(HomeNetworkConfigBuilder.areValidIpv4Cidrs("192.168.5.0"))
        assertFalse(HomeNetworkConfigBuilder.areValidIpv4Cidrs(""))
    }
}
