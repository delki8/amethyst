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
package com.vitorpamplona.amethyst.service.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat
import com.vitorpamplona.amethyst.LocalPreferences
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.service.notifications.NotificationUtils.sendDMNotification
import com.vitorpamplona.amethyst.service.notifications.NotificationUtils.sendZapNotification
import com.vitorpamplona.amethyst.ui.note.showAmount
import com.vitorpamplona.quartz.encoders.toHexKey
import com.vitorpamplona.quartz.events.ChatMessageEvent
import com.vitorpamplona.quartz.events.ChatroomKey
import com.vitorpamplona.quartz.events.Event
import com.vitorpamplona.quartz.events.GiftWrapEvent
import com.vitorpamplona.quartz.events.LnZapEvent
import com.vitorpamplona.quartz.events.LnZapRequestEvent
import com.vitorpamplona.quartz.events.PrivateDmEvent
import com.vitorpamplona.quartz.events.SealedGossipEvent
import com.vitorpamplona.quartz.utils.TimeUtils
import kotlinx.collections.immutable.persistentSetOf
import java.math.BigDecimal

class EventNotificationConsumer(private val applicationContext: Context) {
    suspend fun consume(event: GiftWrapEvent) {
        if (!LocalCache.justVerify(event)) return
        if (!notificationManager().areNotificationsEnabled()) return

        // PushNotification Wraps don't include a receiver.
        // Test with all logged in accounts
        LocalPreferences.allSavedAccounts().forEach {
            if (it.hasPrivKey || it.loggedInWithExternalSigner) {
                LocalPreferences.loadCurrentAccountFromEncryptedStorage(it.npub)?.let { acc ->
                    consumeIfMatchesAccount(event, acc)
                }
            }
        }
    }

    private suspend fun consumeIfMatchesAccount(
        pushWrappedEvent: GiftWrapEvent,
        account: Account,
    ) {
        pushWrappedEvent.cachedGift(account.signer) { notificationEvent ->
            LocalCache.justConsume(notificationEvent, null)

            unwrapAndConsume(notificationEvent, account) { innerEvent ->
                if (innerEvent is PrivateDmEvent) {
                    notify(innerEvent, account)
                } else if (innerEvent is LnZapEvent) {
                    notify(innerEvent, account)
                } else if (innerEvent is ChatMessageEvent) {
                    notify(innerEvent, account)
                }
            }
        }
    }

    private fun unwrapAndConsume(
        event: Event,
        account: Account,
        onReady: (Event) -> Unit,
    ) {
        if (!LocalCache.justVerify(event)) return

        when (event) {
            is GiftWrapEvent -> {
                event.cachedGift(account.signer) { unwrapAndConsume(it, account, onReady) }
            }
            is SealedGossipEvent -> {
                event.cachedGossip(account.signer) {
                    // this is not verifiable
                    LocalCache.justConsume(it, null)
                    onReady(it)
                }
            }
            else -> {
                LocalCache.justConsume(event, null)
                onReady(event)
            }
        }
    }

    private fun notify(
        event: ChatMessageEvent,
        acc: Account,
    ) {
        if (
            event.createdAt > TimeUtils.fiveMinutesAgo() && // old event being re-broadcasted
            event.pubKey != acc.userProfile().pubkeyHex
        ) { // from the user

            val chatNote = LocalCache.notes[event.id] ?: return
            val chatRoom = event.chatroomKey(acc.keyPair.pubKey.toHexKey())

            val followingKeySet = acc.followingKeySet()

            val isKnownRoom =
                (
                    acc.userProfile().privateChatrooms[chatRoom]?.senderIntersects(followingKeySet) == true ||
                        acc.userProfile().hasSentMessagesTo(chatRoom)
                ) && !acc.isAllHidden(chatRoom.users)

            if (isKnownRoom) {
                val content = chatNote.event?.content() ?: ""
                val user = chatNote.author?.toBestDisplayName() ?: ""
                val userPicture = chatNote.author?.profilePicture()
                val noteUri = chatNote.toNEvent()
                notificationManager()
                    .sendDMNotification(
                        event.id,
                        content,
                        user,
                        userPicture,
                        noteUri,
                        applicationContext,
                    )
            }
        }
    }

    private fun notify(
        event: PrivateDmEvent,
        acc: Account,
    ) {
        val note = LocalCache.notes[event.id] ?: return

        // old event being re-broadcast
        if (event.createdAt < TimeUtils.fiveMinutesAgo()) return

        if (acc.userProfile().pubkeyHex == event.verifiedRecipientPubKey()) {
            val followingKeySet = acc.followingKeySet()

            val knownChatrooms =
                acc
                    .userProfile()
                    .privateChatrooms
                    .keys
                    .filter {
                        (
                            acc.userProfile().privateChatrooms[it]?.senderIntersects(followingKeySet) == true ||
                                acc.userProfile().hasSentMessagesTo(it)
                        ) && !acc.isAllHidden(it.users)
                    }
                    .toSet()

            note.author?.let {
                if (ChatroomKey(persistentSetOf(it.pubkeyHex)) in knownChatrooms) {
                    acc.decryptContent(note) { content ->
                        val user = note.author?.toBestDisplayName() ?: ""
                        val userPicture = note.author?.profilePicture()
                        val noteUri = note.toNEvent()
                        notificationManager()
                            .sendDMNotification(event.id, content, user, userPicture, noteUri, applicationContext)
                    }
                }
            }
        }
    }

    private fun notify(
        event: LnZapEvent,
        acc: Account,
    ) {
        val noteZapEvent = LocalCache.notes[event.id] ?: return

        // old event being re-broadcast
        if (event.createdAt < TimeUtils.fiveMinutesAgo()) return

        val noteZapRequest = event.zapRequest?.id?.let { LocalCache.checkGetOrCreateNote(it) } ?: return
        val noteZapped =
            event.zappedPost().firstOrNull()?.let { LocalCache.checkGetOrCreateNote(it) } ?: return

        if ((event.amount ?: BigDecimal.ZERO) < BigDecimal.TEN) return

        if (acc.userProfile().pubkeyHex == event.zappedAuthor().firstOrNull()) {
            val amount = showAmount(event.amount)
            (noteZapRequest.event as? LnZapRequestEvent)?.let { event ->
                acc.decryptZapContentAuthor(noteZapRequest) {
                    val author = LocalCache.getOrCreateUser(it.pubKey)
                    val senderInfo = Pair(author, it.content.ifBlank { null })

                    acc.decryptContent(noteZapped) {
                        val zappedContent = it.split("\n").get(0)

                        val user = senderInfo.first.toBestDisplayName()
                        var title =
                            applicationContext.getString(R.string.app_notification_zaps_channel_message, amount)
                        senderInfo.second?.ifBlank { null }?.let { title += " ($it)" }
                        var content =
                            applicationContext.getString(
                                R.string.app_notification_zaps_channel_message_from,
                                user,
                            )
                        zappedContent?.let {
                            content +=
                                " " +
                                applicationContext.getString(
                                    R.string.app_notification_zaps_channel_message_for,
                                    zappedContent,
                                )
                        }
                        val userPicture = senderInfo?.first?.profilePicture()
                        val noteUri = "nostr:Notifications"
                        notificationManager()
                            .sendZapNotification(
                                event.id,
                                content,
                                title,
                                userPicture,
                                noteUri,
                                applicationContext,
                            )
                    }
                }
            }
        }
    }

    fun notificationManager(): NotificationManager {
        return ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)
            as NotificationManager
    }
}
