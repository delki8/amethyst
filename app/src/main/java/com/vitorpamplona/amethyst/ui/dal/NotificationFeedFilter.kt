/**
 * Copyright (c) 2023 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.amethyst.ui.dal

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.GLOBAL_FOLLOWS
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.quartz.encoders.HexKey
import com.vitorpamplona.quartz.events.BadgeDefinitionEvent
import com.vitorpamplona.quartz.events.BadgeProfilesEvent
import com.vitorpamplona.quartz.events.BaseTextNoteEvent
import com.vitorpamplona.quartz.events.ChannelCreateEvent
import com.vitorpamplona.quartz.events.ChannelMetadataEvent
import com.vitorpamplona.quartz.events.GenericRepostEvent
import com.vitorpamplona.quartz.events.GiftWrapEvent
import com.vitorpamplona.quartz.events.LnZapEvent
import com.vitorpamplona.quartz.events.LnZapRequestEvent
import com.vitorpamplona.quartz.events.MuteListEvent
import com.vitorpamplona.quartz.events.PeopleListEvent
import com.vitorpamplona.quartz.events.ReactionEvent
import com.vitorpamplona.quartz.events.RepostEvent

class NotificationFeedFilter(val account: Account) : AdditiveFeedFilter<Note>() {
    override fun feedKey(): String {
        return account.userProfile().pubkeyHex + "-" + account.defaultNotificationFollowList.value
    }

    override fun showHiddenKey(): Boolean {
        return account.defaultNotificationFollowList.value ==
            PeopleListEvent.blockListFor(account.userProfile().pubkeyHex) ||
            account.defaultNotificationFollowList.value ==
            MuteListEvent.blockListFor(account.userProfile().pubkeyHex)
    }

    override fun feed(): List<Note> {
        return sort(innerApplyFilter(LocalCache.notes.values))
    }

    override fun applyFilter(collection: Set<Note>): Set<Note> {
        return innerApplyFilter(collection)
    }

    private fun innerApplyFilter(collection: Collection<Note>): Set<Note> {
        val isGlobal = account.defaultNotificationFollowList.value == GLOBAL_FOLLOWS
        val isHiddenList = showHiddenKey()

        val followingKeySet = account.liveNotificationFollowLists.value?.users ?: emptySet()

        val loggedInUser = account.userProfile()
        val loggedInUserHex = loggedInUser.pubkeyHex

        return collection
            .filter {
                it.event !is ChannelCreateEvent &&
                    it.event !is ChannelMetadataEvent &&
                    it.event !is LnZapRequestEvent &&
                    it.event !is BadgeDefinitionEvent &&
                    it.event !is BadgeProfilesEvent &&
                    it.event !is GiftWrapEvent &&
                    (it.event is LnZapEvent || it.author !== loggedInUser) &&
                    (isGlobal || it.author?.pubkeyHex in followingKeySet) &&
                    it.event?.isTaggedUser(loggedInUserHex) ?: false &&
                    (isHiddenList || it.author == null || !account.isHidden(it.author!!.pubkeyHex)) &&
                    tagsAnEventByUser(it, loggedInUserHex)
            }
            .toSet()
    }

    override fun sort(collection: Set<Note>): List<Note> {
        return collection.sortedWith(compareBy({ it.createdAt() }, { it.idHex })).reversed()
    }

    fun tagsAnEventByUser(
        note: Note,
        authorHex: HexKey,
    ): Boolean {
        val event = note.event

        if (event is BaseTextNoteEvent) {
            val isAuthoredPostCited =
                event.findCitations().any {
                    LocalCache.notes[it]?.author?.pubkeyHex == authorHex ||
                        LocalCache.addressables[it]?.author?.pubkeyHex == authorHex
                }

            return isAuthoredPostCited ||
                (
                    event.citedUsers().contains(authorHex) ||
                        note.replyTo?.any { it.author?.pubkeyHex == authorHex } == true
                )
        }

        if (event is ReactionEvent) {
            return note.replyTo?.lastOrNull()?.author?.pubkeyHex == authorHex
        }

        if (event is RepostEvent || event is GenericRepostEvent) {
            return note.replyTo?.lastOrNull()?.author?.pubkeyHex == authorHex
        }

        return true
    }
}
