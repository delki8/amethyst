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
package com.vitorpamplona.amethyst.service.relays

import com.vitorpamplona.amethyst.model.RelaySetupInfo

object Constants {
    val activeTypes = setOf(FeedType.FOLLOWS, FeedType.PRIVATE_DMS)
    val activeTypesChats = setOf(FeedType.FOLLOWS, FeedType.PUBLIC_CHATS, FeedType.PRIVATE_DMS)
    val activeTypesGlobalChats =
        setOf(FeedType.FOLLOWS, FeedType.PUBLIC_CHATS, FeedType.PRIVATE_DMS, FeedType.GLOBAL)
    val activeTypesSearch = setOf(FeedType.SEARCH)

    fun convertDefaultRelays(): Array<Relay> {
        return defaultRelays.map { Relay(it.url, it.read, it.write, it.feedTypes) }.toTypedArray()
    }

    val defaultRelays =
        arrayOf(
            // Free relays for only DMs and Follows due to the amount of spam
            RelaySetupInfo("wss://relay.damus.io", read = true, write = true, feedTypes = activeTypes),
            // Chats
            RelaySetupInfo(
                "wss://nostr.bitcoiner.social",
                read = true,
                write = true,
                feedTypes = activeTypesChats,
            ),
            RelaySetupInfo(
                "wss://relay.nostr.bg",
                read = true,
                write = true,
                feedTypes = activeTypesChats,
            ),
            RelaySetupInfo(
                "wss://nostr.oxtr.dev",
                read = true,
                write = true,
                feedTypes = activeTypesChats,
            ),
            RelaySetupInfo(
                "wss://nostr-pub.wellorder.net",
                read = true,
                write = true,
                feedTypes = activeTypesChats,
            ),
            RelaySetupInfo("wss://nostr.mom", read = true, write = true, feedTypes = activeTypesChats),
            RelaySetupInfo("wss://nos.lol", read = true, write = true, feedTypes = activeTypesChats),
            // Less Reliable
            // NewRelayListViewModel.Relay("wss://nostr.orangepill.dev", read = true, write = true,
            // feedTypes = activeTypes),
            // NewRelayListViewModel.Relay("wss://nostr.onsats.org", read = true, write = true, feedTypes
            // = activeTypes),
            // NewRelayListViewModel.Relay("wss://nostr.sandwich.farm", read = true, write = true,
            // feedTypes = activeTypes),
            // NewRelayListViewModel.Relay("wss://relay.nostr.ch", read = true, write = true, feedTypes =
            // activeTypes),
            // NewRelayListViewModel.Relay("wss://nostr.zebedee.cloud", read = true, write = true,
            // feedTypes = activeTypes),
            // NewRelayListViewModel.Relay("wss://nostr.rocks", read = true, write = true, feedTypes =
            // activeTypes),
            // NewRelayListViewModel.Relay("wss://nostr.fmt.wiz.biz", read = true, write = true, feedTypes
            // = activeTypes),
            // NewRelayListViewModel.Relay("wss://brb.io", read = true, write = true, feedTypes =
            // activeTypes),
            // Paid relays
            RelaySetupInfo(
                "wss://relay.snort.social",
                read = true,
                write = false,
                feedTypes = activeTypesGlobalChats,
            ),
            RelaySetupInfo(
                "wss://relay.nostr.com.au",
                read = true,
                write = false,
                feedTypes = activeTypesGlobalChats,
            ),
            RelaySetupInfo(
                "wss://eden.nostr.land",
                read = true,
                write = false,
                feedTypes = activeTypesGlobalChats,
            ),
            RelaySetupInfo(
                "wss://nostr.milou.lol",
                read = true,
                write = false,
                feedTypes = activeTypesGlobalChats,
            ),
            RelaySetupInfo(
                "wss://puravida.nostr.land",
                read = true,
                write = false,
                feedTypes = activeTypesGlobalChats,
            ),
            RelaySetupInfo(
                "wss://nostr.wine",
                read = true,
                write = false,
                feedTypes = activeTypesGlobalChats,
            ),
            RelaySetupInfo(
                "wss://nostr.inosta.cc",
                read = true,
                write = false,
                feedTypes = activeTypesGlobalChats,
            ),
            RelaySetupInfo(
                "wss://atlas.nostr.land",
                read = true,
                write = false,
                feedTypes = activeTypesGlobalChats,
            ),
            RelaySetupInfo(
                "wss://relay.orangepill.dev",
                read = true,
                write = false,
                feedTypes = activeTypesGlobalChats,
            ),
            RelaySetupInfo(
                "wss://relay.nostrati.com",
                read = true,
                write = false,
                feedTypes = activeTypesGlobalChats,
            ),
            // Supporting NIP-50
            RelaySetupInfo(
                "wss://relay.nostr.band",
                read = true,
                write = false,
                feedTypes = activeTypesSearch,
            ),
            RelaySetupInfo("wss://nostr.wine", read = true, write = false, feedTypes = activeTypesSearch),
            RelaySetupInfo(
                "wss://relay.noswhere.com",
                read = true,
                write = false,
                feedTypes = activeTypesSearch,
            ),
        )

    val forcedRelayForSearch =
        arrayOf(
            RelaySetupInfo(
                "wss://relay.nostr.band",
                read = true,
                write = false,
                feedTypes = activeTypesSearch,
            ),
            RelaySetupInfo("wss://nostr.wine", read = true, write = false, feedTypes = activeTypesSearch),
            RelaySetupInfo(
                "wss://relay.noswhere.com",
                read = true,
                write = false,
                feedTypes = activeTypesSearch,
            ),
        )
    val forcedRelaysForSearchSet = forcedRelayForSearch.map { it.url }
}
