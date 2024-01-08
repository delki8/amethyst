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
package com.vitorpamplona.amethyst.model

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vitorpamplona.amethyst.R

fun checkForHashtagWithIcon(
    tag: String,
    primary: Color,
): HashtagIcon? {
    return when (tag.lowercase()) {
        "bitcoin",
        "btc",
        "timechain",
        "bitcoiner",
        "bitcoiners",
        ->
            HashtagIcon(
                R.drawable.ht_btc,
                "Bitcoin",
                Color.Unspecified,
                Modifier.padding(2.dp, 2.dp, 0.dp, 0.dp),
            )
        "nostr",
        "nostrich",
        "nostriches",
        "thenostr",
        ->
            HashtagIcon(
                R.drawable.ht_nostr,
                "Nostr",
                Color.Unspecified,
                Modifier.padding(1.dp, 2.dp, 0.dp, 0.dp),
            )
        "lightning",
        "lightningnetwork",
        ->
            HashtagIcon(
                R.drawable.ht_lightning,
                "Lightning",
                Color.Unspecified,
                Modifier.padding(1.dp, 3.dp, 0.dp, 0.dp),
            )
        "zap",
        "zaps",
        "zapper",
        "zappers",
        "zapping",
        "zapped",
        "zapathon",
        "zapraiser",
        "zaplife",
        "zapchain",
        ->
            HashtagIcon(
                R.drawable.zap,
                "Zap",
                Color.Unspecified,
                Modifier.padding(1.dp, 3.dp, 0.dp, 0.dp),
            )
        "amethyst" ->
            HashtagIcon(
                R.drawable.amethyst,
                "Amethyst",
                Color.Unspecified,
                Modifier.padding(3.dp, 2.dp, 0.dp, 0.dp),
            )
        "onyx" ->
            HashtagIcon(
                R.drawable.black_heart,
                "Onyx",
                Color.Unspecified,
                Modifier.padding(1.dp, 3.dp, 0.dp, 0.dp),
            )
        "cashu",
        "ecash",
        "nut",
        "nuts",
        "deeznuts",
        ->
            HashtagIcon(
                R.drawable.cashu,
                "Cashu",
                Color.Unspecified,
                Modifier.padding(1.dp, 3.dp, 0.dp, 0.dp),
            )
        "plebs",
        "pleb",
        "plebchain",
        ->
            HashtagIcon(
                R.drawable.plebs,
                "Pleb",
                Color.Unspecified,
                Modifier.padding(2.dp, 2.dp, 0.dp, 1.dp),
            )
        "coffee",
        "coffeechain",
        "cafe",
        ->
            HashtagIcon(
                R.drawable.coffee,
                "Coffee",
                Color.Unspecified,
                Modifier.padding(2.dp, 2.dp, 0.dp, 0.dp),
            )
        "skullofsatoshi" ->
            HashtagIcon(
                R.drawable.skull,
                "SkullofSatoshi",
                Color.Unspecified,
                Modifier.padding(2.dp, 1.dp, 0.dp, 0.dp),
            )
        "grownostr",
        "gardening",
        "garden",
        ->
            HashtagIcon(
                R.drawable.grownostr,
                "GrowNostr",
                Color.Unspecified,
                Modifier.padding(0.dp, 1.dp, 0.dp, 1.dp),
            )
        "footstr" ->
            HashtagIcon(
                R.drawable.footstr,
                "Footstr",
                Color.Unspecified,
                Modifier.padding(1.dp, 1.dp, 0.dp, 0.dp),
            )
        "tunestr",
        "music",
        "nowplaying",
        ->
            HashtagIcon(R.drawable.tunestr, "Tunestr", primary, Modifier.padding(0.dp, 3.dp, 0.dp, 1.dp))
        "weed",
        "weedstr",
        "420",
        "cannabis",
        "marijuana",
        ->
            HashtagIcon(
                R.drawable.weed,
                "Weed",
                Color.Unspecified,
                Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp),
            )
        else -> null
    }
}

@Immutable
class HashtagIcon(
    val icon: Int,
    val description: String,
    val color: Color,
    val modifier: Modifier,
)
