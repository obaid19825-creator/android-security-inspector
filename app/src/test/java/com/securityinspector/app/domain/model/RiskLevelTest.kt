package com.securityinspector.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RiskLevelTest {

    @Test
    fun `score of 100 maps to SAFE`() {
        assertThat(RiskLevel.fromScore(100)).isEqualTo(RiskLevel.SAFE)
    }

    @Test
    fun `score of 0 maps to CRITICAL`() {
        assertThat(RiskLevel.fromScore(0)).isEqualTo(RiskLevel.CRITICAL)
    }

    @Test
    fun `score of 80 maps to LOW`() {
        assertThat(RiskLevel.fromScore(80)).isEqualTo(RiskLevel.LOW)
    }

    @Test
    fun `score of 60 maps to MEDIUM`() {
        assertThat(RiskLevel.fromScore(60)).isEqualTo(RiskLevel.MEDIUM)
    }

    @Test
    fun `score of 30 maps to HIGH`() {
        assertThat(RiskLevel.fromScore(30)).isEqualTo(RiskLevel.HIGH)
    }

    @Test
    fun `out of range score is coerced before mapping`() {
        assertThat(RiskLevel.fromScore(150)).isEqualTo(RiskLevel.SAFE)
        assertThat(RiskLevel.fromScore(-20)).isEqualTo(RiskLevel.CRITICAL)
    }
}
