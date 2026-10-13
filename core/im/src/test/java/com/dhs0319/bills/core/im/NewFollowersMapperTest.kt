package com.dhs0319.bills.core.im

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFollowersMapperTest {
    @Test
    fun recognizesFollowingAndMutualFollowingRelations() {
        assertTrue(isFollowedRelation(1))
        assertTrue(isFollowedRelation(2))
        assertTrue(isFollowedRelation(6))
    }

    @Test
    fun nonFollowingAndBlockedUsersCanBeDistinguishedFromFollowedUsers() {
        listOf(0, 4, 128, 130, -1).forEach { assertFalse(isFollowedRelation(it)) }
    }
}
