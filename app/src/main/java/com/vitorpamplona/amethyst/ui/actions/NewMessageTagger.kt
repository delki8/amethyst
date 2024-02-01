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
package com.vitorpamplona.amethyst.ui.actions

import androidx.compose.runtime.Immutable
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.quartz.crypto.KeyPair
import com.vitorpamplona.quartz.encoders.Bech32
import com.vitorpamplona.quartz.encoders.HexKey
import com.vitorpamplona.quartz.encoders.Nip19
import com.vitorpamplona.quartz.encoders.bechToBytes
import com.vitorpamplona.quartz.encoders.toNpub
import kotlinx.coroutines.CancellationException

class NewMessageTagger(
    var message: String,
    var pTags: List<User>? = null,
    var eTags: List<Note>? = null,
    var channelHex: String? = null,
    var dao: Dao,
) {
    val directMentions = mutableSetOf<HexKey>()

    fun addUserToMentions(user: User) {
        directMentions.add(user.pubkeyHex)
        pTags = if (pTags?.contains(user) == true) pTags else pTags?.plus(user) ?: listOf(user)
    }

    fun addNoteToReplyTos(note: Note) {
        directMentions.add(note.idHex)

        note.author?.let { addUserToMentions(it) }
        eTags = if (eTags?.contains(note) == true) eTags else eTags?.plus(note) ?: listOf(note)
    }

    fun tagIndex(user: User): Int {
        // Postr Events assembles replies before mentions in the tag order
        return (if (channelHex != null) 1 else 0) + (eTags?.size ?: 0) + (pTags?.indexOf(user) ?: 0)
    }

    fun tagIndex(note: Note): Int {
        // Postr Events assembles replies before mentions in the tag order
        return (if (channelHex != null) 1 else 0) + (eTags?.indexOf(note) ?: 0)
    }

    suspend fun run() {
        // adds all references to mentions and reply tos
        message.split('\n').forEach { paragraph: String ->
            paragraph.split(' ').forEach { word: String ->
                val results = parseDirtyWordForKey(word)

                if (results?.key?.type == Nip19.Type.USER) {
                    addUserToMentions(dao.getOrCreateUser(results.key.hex))
                } else if (results?.key?.type == Nip19.Type.NOTE) {
                    addNoteToReplyTos(dao.getOrCreateNote(results.key.hex))
                } else if (results?.key?.type == Nip19.Type.EVENT) {
                    addNoteToReplyTos(dao.getOrCreateNote(results.key.hex))
                } else if (results?.key?.type == Nip19.Type.ADDRESS) {
                    val note = dao.checkGetOrCreateAddressableNote(results.key.hex)
                    if (note != null) {
                        addNoteToReplyTos(note)
                    }
                }
            }
        }

        // Tags the text in the correct order.
        message =
            message
                .split('\n')
                .map { paragraph: String ->
                    paragraph
                        .split(' ')
                        .map { word: String ->
                            val results = parseDirtyWordForKey(word)
                            if (results?.key?.type == Nip19.Type.USER) {
                                val user = dao.getOrCreateUser(results.key.hex)

                                getNostrAddress(user.pubkeyNpub(), results.restOfWord)
                            } else if (results?.key?.type == Nip19.Type.NOTE) {
                                val note = dao.getOrCreateNote(results.key.hex)

                                getNostrAddress(note.toNEvent(), results.restOfWord)
                            } else if (results?.key?.type == Nip19.Type.EVENT) {
                                val note = dao.getOrCreateNote(results.key.hex)

                                getNostrAddress(note.toNEvent(), results.restOfWord)
                            } else if (results?.key?.type == Nip19.Type.ADDRESS) {
                                val note = dao.checkGetOrCreateAddressableNote(results.key.hex)
                                if (note != null) {
                                    getNostrAddress(note.idNote(), results.restOfWord)
                                } else {
                                    word
                                }
                            } else {
                                word
                            }
                        }
                        .joinToString(" ")
                }
                .joinToString("\n")
    }

    fun getNostrAddress(
        bechAddress: String,
        restOfTheWord: String,
    ): String {
        return if (restOfTheWord.isEmpty()) {
            "nostr:$bechAddress"
        } else {
            if (Bech32.ALPHABET.contains(restOfTheWord.get(0), true)) {
                "nostr:$bechAddress $restOfTheWord"
            } else {
                "nostr:${bechAddress}$restOfTheWord"
            }
        }
    }

    @Immutable data class DirtyKeyInfo(val key: Nip19.Return, val restOfWord: String)

    fun parseDirtyWordForKey(mightBeAKey: String): DirtyKeyInfo? {
        var key = mightBeAKey
        if (key.startsWith("nostr:", true)) {
            key = key.substring("nostr:".length)
        }

        key = key.removePrefix("@")

        try {
            if (key.startsWith("nsec1", true)) {
                if (key.length < 63) {
                    return null
                }

                val keyB32 = key.substring(0, 63)
                val restOfWord = key.substring(63)
                // Converts to npub
                val pubkey =
                    Nip19.uriToRoute(KeyPair(privKey = keyB32.bechToBytes()).pubKey.toNpub()) ?: return null

                return DirtyKeyInfo(pubkey, restOfWord)
            } else if (key.startsWith("npub1", true)) {
                if (key.length < 63) {
                    return null
                }

                val keyB32 = key.substring(0, 63)
                val restOfWord = key.substring(63)

                val pubkey = Nip19.uriToRoute(keyB32) ?: return null

                return DirtyKeyInfo(pubkey, restOfWord)
            } else if (key.startsWith("note1", true)) {
                if (key.length < 63) {
                    return null
                }

                val keyB32 = key.substring(0, 63)
                val restOfWord = key.substring(63)

                val noteId = Nip19.uriToRoute(keyB32) ?: return null

                return DirtyKeyInfo(noteId, restOfWord)
            } else if (key.startsWith("nprofile", true)) {
                val pubkeyRelay = Nip19.uriToRoute(key) ?: return null

                return DirtyKeyInfo(pubkeyRelay, pubkeyRelay.additionalChars)
            } else if (key.startsWith("nevent1", true)) {
                val noteRelayId = Nip19.uriToRoute(key) ?: return null

                return DirtyKeyInfo(noteRelayId, noteRelayId.additionalChars)
            } else if (key.startsWith("naddr1", true)) {
                val address = Nip19.uriToRoute(key) ?: return null

                return DirtyKeyInfo(
                    address,
                    address.additionalChars,
                ) // no way to know when they address ends and dirt begins
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
        }

        return null
    }
}

interface Dao {
    suspend fun getOrCreateUser(hex: String): User

    suspend fun getOrCreateNote(hex: String): Note

    suspend fun checkGetOrCreateAddressableNote(hex: String): Note?
}
