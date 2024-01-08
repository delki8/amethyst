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
import com.vitorpamplona.quartz.events.AudioHeaderEvent
import com.vitorpamplona.quartz.events.AudioTrackEvent
import com.vitorpamplona.quartz.events.ClassifiedsEvent
import com.vitorpamplona.quartz.events.GenericRepostEvent
import com.vitorpamplona.quartz.events.HighlightEvent
import com.vitorpamplona.quartz.events.LongTextNoteEvent
import com.vitorpamplona.quartz.events.MuteListEvent
import com.vitorpamplona.quartz.events.PeopleListEvent
import com.vitorpamplona.quartz.events.PollNoteEvent
import com.vitorpamplona.quartz.events.RepostEvent
import com.vitorpamplona.quartz.events.TextNoteEvent
import com.vitorpamplona.quartz.utils.TimeUtils

class HomeNewThreadFeedFilter(val account: Account) : AdditiveFeedFilter<Note>() {
    override fun feedKey(): String {
        return account.userProfile().pubkeyHex + "-" + account.defaultHomeFollowList.value
    }

    override fun showHiddenKey(): Boolean {
        return account.defaultHomeFollowList.value ==
            PeopleListEvent.blockListFor(account.userProfile().pubkeyHex) ||
            account.defaultHomeFollowList.value ==
            MuteListEvent.blockListFor(account.userProfile().pubkeyHex)
    }

    override fun feed(): List<Note> {
        val notes = innerApplyFilter(LocalCache.notes.values, true)
        val longFormNotes = innerApplyFilter(LocalCache.addressables.values, false)

        return sort(notes + longFormNotes)
    }

    override fun applyFilter(collection: Set<Note>): Set<Note> {
        return innerApplyFilter(collection, false)
    }

    private fun innerApplyFilter(
        collection: Collection<Note>,
        ignoreAddressables: Boolean,
    ): Set<Note> {
        val isGlobal = account.defaultHomeFollowList.value == GLOBAL_FOLLOWS
        val gRelays = account.activeGlobalRelays()
        val isHiddenList = showHiddenKey()

        val followingKeySet = account.liveHomeFollowLists.value?.users ?: emptySet()
        val followingTagSet = account.liveHomeFollowLists.value?.hashtags ?: emptySet()
        val followingGeohashSet = account.liveHomeFollowLists.value?.geotags ?: emptySet()
        val followingCommunities = account.liveHomeFollowLists.value?.communities ?: emptySet()

        val oneMinuteInTheFuture = TimeUtils.now() + (1 * 60) // one minute in the future.
        val oneHr = 60 * 60

        return collection
            .asSequence()
            .filter { it ->
                val noteEvent = it.event
                val isGlobalRelay = it.relays.any { gRelays.contains(it.url) }
                (
                    noteEvent is TextNoteEvent ||
                        noteEvent is ClassifiedsEvent ||
                        noteEvent is RepostEvent ||
                        noteEvent is GenericRepostEvent ||
                        noteEvent is LongTextNoteEvent ||
                        noteEvent is PollNoteEvent ||
                        noteEvent is HighlightEvent ||
                        noteEvent is AudioTrackEvent ||
                        noteEvent is AudioHeaderEvent
                ) &&
                    (!ignoreAddressables || noteEvent.kind() < 10000) &&
                    (
                        (isGlobal && isGlobalRelay) ||
                            it.author?.pubkeyHex in followingKeySet ||
                            noteEvent.isTaggedHashes(followingTagSet) ||
                            noteEvent.isTaggedGeoHashes(followingGeohashSet) ||
                            noteEvent.isTaggedAddressableNotes(followingCommunities)
                    ) &&
                    // && account.isAcceptable(it)  // This filter follows only. No need to check if
                    // acceptable
                    (isHiddenList || it.author?.let { !account.isHidden(it.pubkeyHex) } ?: true) &&
                    ((it.event?.createdAt() ?: 0) < oneMinuteInTheFuture) &&
                    it.isNewThread() &&
                    (
                        (noteEvent !is RepostEvent && noteEvent !is GenericRepostEvent) || // not a repost
                            (
                                it.replyTo?.lastOrNull()?.author?.pubkeyHex !in followingKeySet ||
                                    (
                                        noteEvent.createdAt() >
                                            (
                                                it.replyTo?.lastOrNull()?.createdAt()
                                                    ?: 0
                                            ) + oneHr
                                    )
                            ) // or a repost of by a non-follower's post (likely not seen yet)
                    )
            }
            .toSet()
    }

    override fun sort(collection: Set<Note>): List<Note> {
        return collection.sortedWith(compareBy({ it.createdAt() }, { it.idHex })).reversed()
    }
}
