package com.securityinspector.app.data.source

import com.google.common.truth.Truth.assertThat
import com.securityinspector.app.domain.model.FdCategory
import org.junit.Before
import org.junit.Test

class FdListingParserTest {

    private lateinit var parser: FdListingParser

    @Before
    fun setUp() {
        parser = FdListingParser()
    }

    @Test
    fun `classifies binder references`() {
        val result = parser.parse("test", "62 -> /dev/binder")
        assertThat(result.entries.single().category).isEqualTo(FdCategory.BINDER)
    }

    @Test
    fun `classifies hwbinder references distinct from binder`() {
        val result = parser.parse("test", "63 -> /dev/hwbinder")
        assertThat(result.entries.single().category).isEqualTo(FdCategory.HWBINDER)
    }

    @Test
    fun `classifies apk file references`() {
        val result = parser.parse("test", "12 -> /data/app/com.example-1/base.apk")
        assertThat(result.entries.single().category).isEqualTo(FdCategory.APK_FILES)
    }

    @Test
    fun `classifies socket references`() {
        val result = parser.parse("test", "8 -> socket:[12345]")
        assertThat(result.entries.single().category).isEqualTo(FdCategory.SOCKETS)
    }

    @Test
    fun `classifies pipe references`() {
        val result = parser.parse("test", "4 -> pipe:[9876]")
        assertThat(result.entries.single().category).isEqualTo(FdCategory.PIPES)
    }

    @Test
    fun `classifies eventfd and eventpoll distinctly`() {
        val eventFd = parser.parse("test", "5 -> anon_inode:[eventfd]")
        val eventPoll = parser.parse("test", "6 -> anon_inode:[eventpoll]")

        assertThat(eventFd.entries.single().category).isEqualTo(FdCategory.EVENTFD)
        assertThat(eventPoll.entries.single().category).isEqualTo(FdCategory.EVENTPOLL)
    }

    @Test
    fun `unrecognized line falls back to UNKNOWN`() {
        val result = parser.parse("test", "totally not a real fd line !!")
        assertThat(result.entries.single().category).isEqualTo(FdCategory.UNKNOWN)
    }

    @Test
    fun `blank lines are ignored`() {
        val result = parser.parse("test", "\n\n   \n")
        assertThat(result.entries).isEmpty()
    }

    @Test
    fun `counts by category aggregate correctly across multiple lines`() {
        val text = """
            62 -> /dev/binder
            63 -> /dev/binder
            8 -> socket:[1]
        """.trimIndent()

        val result = parser.parse("test", text)

        assertThat(result.countsByCategory[FdCategory.BINDER]).isEqualTo(2)
        assertThat(result.countsByCategory[FdCategory.SOCKETS]).isEqualTo(1)
    }
}
