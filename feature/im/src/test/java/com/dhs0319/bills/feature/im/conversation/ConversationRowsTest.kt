package com.dhs0319.bills.feature.im.conversation

import com.dhs0319.bills.core.model.ImMessage
import com.dhs0319.bills.core.model.ImMsgType
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class ConversationRowsTest {
    private val now = ZonedDateTime.parse("2026-11-04T21:00:00+08:00[Asia/Shanghai]")

    @Test
    fun `messages in the same minute share a divider in reverse list order`() {
        val rows = ConversationRowCache().build(
            listOf(
                message(3, now.minusSeconds(5)),
                message(2, now.minusSeconds(30)),
                message(1, now.minusMinutes(2))
            ),
            now
        )

        assertEquals(listOf(3L, 2L, "time2", 1L, "time1"), rows.map { it.key })
        assertEquals(listOf("20:59", "20:58"), rows.filterIsInstance<TimeDividerRow>().map { it.text })
    }

    @Test
    fun `loading older messages and sending preserve unchanged rows and dividers`() {
        val cache = ConversationRowCache()
        val initial = listOf(message(2, now.minusMinutes(2)), message(1, now.minusMinutes(3)))
        val originalRows = cache.build(initial, now).associateBy { it.key }
        val older = message(0, now.minusMinutes(4))
        val historyRows = cache.build(initial.map { it.copy() } + older, now)
        originalRows.forEach { (key, row) -> assertSame(row, historyRows.single { it.key == key }) }

        val sent = message(3, now)
        val sentRows = cache.build(listOf(sent) + initial + older, now)
        historyRows.forEach { row -> assertSame(row, sentRows.single { it.key == row.key }) }
        assertEquals(3L, sentRows.first().key)
    }

    @Test
    fun `changed message metadata replaces its row without invalidating other messages`() {
        val cache = ConversationRowCache()
        val messages = listOf(message(2, now), message(1, now.minusMinutes(1)))
        val before = cache.build(messages, now)
        val changed = messages.first().copy(content = "已撤回的消息", isRecalled = true)
        val after = cache.build(listOf(changed, messages.last()), now)

        assertNotSame(before.first(), after.first())
        assertEquals(changed, (after.first() as MessageRow).item.message)
        assertEquals(changed.content, (after.first() as MessageRow).item.displayText)
        assertSame(before.single { it.key == 1L }, after.single { it.key == 1L })
        assertSame(before.single { it.key == "time2" }, after.single { it.key == "time2" })
    }

    @Test
    fun `cache drops rows removed from the loaded timeline`() {
        val cache = ConversationRowCache()
        val message = message(1, now)
        val before = cache.build(listOf(message), now)

        assertTrue(cache.build(emptyList(), now).isEmpty())
        val after = cache.build(listOf(message), now)
        assertNotSame(before.first(), after.first())
        assertNotSame(before.last(), after.last())
    }

    @Test
    fun `calendar and timezone changes invalidate cached time labels`() {
        val cache = ConversationRowCache()
        val messages = listOf(message(1, now))
        val today = cache.build(messages, now).first() as MessageRow
        val londonNow = now.withZoneSameInstant(ZoneId.of("Europe/London"))
        val london = cache.build(messages, londonNow).first() as MessageRow
        val tomorrow = cache.build(messages, londonNow.plusDays(1)).first() as MessageRow

        assertEquals("21:00", today.item.timeText)
        assertEquals("13:00", london.item.timeText)
        assertEquals("昨天 13:00", tomorrow.item.timeText)
        assertNotSame(today, london)
        assertNotSame(london, tomorrow)
    }

    @Test
    fun `older dates and absent timestamps keep their display behavior`() {
        val rows = ConversationRowCache().build(
            listOf(
                message(4, now.minusDays(1)),
                message(3, now.minusDays(2)),
                message(2, now.minusYears(1)),
                message(1, now).copy(timestampSec = 0)
            ),
            now
        )

        assertEquals(listOf("昨天 21:00", "11月2日 21:00", "2025年11月4日 21:00"),
            rows.filterIsInstance<TimeDividerRow>().map { it.text })
        assertEquals(1L, rows.last().key)
        assertEquals("", (rows.last() as MessageRow).item.timeText)
    }

    @Test
    fun `image dimensions and message kinds keep safe list content types`() {
        val base = message(1, now).copy(content = "")
        val messages = listOf(
            base.copy(key = 1, msgType = ImMsgType.IMAGE, imageUrl = "image", imageWidth = 100, imageHeight = 1000),
            base.copy(key = 2, msgType = ImMsgType.IMAGE, imageUrl = "image", imageWidth = 1000, imageHeight = 100),
            base.copy(key = 3, msgType = ImMsgType.IMAGE, imageUrl = "image"),
            base.copy(key = 4, msgType = 4, shareCoverUrl = "cover"),
            base.copy(key = 5, msgType = ImMsgType.NOTICE),
            base.copy(key = 6, msgType = ImMsgType.SYSTEM_NOTICE),
            base.copy(key = 7, msgType = 99)
        )
        val items = ConversationRowCache().build(messages, now).filterIsInstance<MessageRow>().map { it.item }

        assertEquals(listOf("image", "image", "image", "share", "notice", "system_notice", "text"),
            items.map { it.contentType })
        assertEquals(0.45f, items[0].imageRatio, 0f)
        assertEquals(1.8f, items[1].imageRatio, 0f)
        assertEquals(1f, items[2].imageRatio, 0f)
        assertEquals("[图片]", items[0].displayText)
        assertEquals("[分享]", items[3].displayText)
        assertEquals("[暂不支持的消息类型 99]", items[6].displayText)
    }

    private fun message(key: Long, time: ZonedDateTime) = ImMessage(
        key = key,
        seqNo = key,
        senderUid = 1,
        receiverId = 2,
        msgType = ImMsgType.TEXT,
        content = "消息 $key",
        timestampSec = time.toEpochSecond(),
        isSelf = false,
        isRecalled = false
    )
}
