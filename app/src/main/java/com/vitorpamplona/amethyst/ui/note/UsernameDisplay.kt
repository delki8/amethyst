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
package com.vitorpamplona.amethyst.ui.note

import android.content.Context
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.LifecycleOwner
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.tts.TextToSpeechHelper
import com.vitorpamplona.amethyst.ui.components.CreateTextWithEmoji
import com.vitorpamplona.amethyst.ui.theme.StdButtonSizeModifier
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import com.vitorpamplona.quartz.events.ImmutableListOfLists

@Composable
fun NoteUsernameDisplay(
    baseNote: Note,
    weight: Modifier = Modifier,
    showPlayButton: Boolean = true,
    textColor: Color = Color.Unspecified,
) {
    val authorState by baseNote.live().authorChanges.observeAsState(baseNote.author)

    Crossfade(targetState = authorState, modifier = weight, label = "NoteUsernameDisplay") {
        it?.let { UsernameDisplay(it, weight, showPlayButton, textColor = textColor) }
    }
}

@Composable
fun UsernameDisplay(
    baseUser: User,
    weight: Modifier = Modifier,
    showPlayButton: Boolean = true,
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color.Unspecified,
) {
    val npubDisplay by remember { derivedStateOf { baseUser.pubkeyDisplayHex() } }

    val userMetadata by baseUser.live().userMetadataInfo.observeAsState(baseUser.info)

    Crossfade(targetState = userMetadata, modifier = weight, label = "UsernameDisplay") {
        if (it != null) {
            UserNameDisplay(
                it.bestUsername(),
                it.bestDisplayName(),
                npubDisplay,
                it.tags,
                weight,
                showPlayButton,
                fontWeight,
                textColor,
            )
        } else {
            NPubDisplay(npubDisplay, weight, fontWeight, textColor)
        }
    }
}

@Composable
private fun UserNameDisplay(
    bestUserName: String?,
    bestDisplayName: String?,
    npubDisplay: String,
    tags: ImmutableListOfLists<String>?,
    modifier: Modifier,
    showPlayButton: Boolean = true,
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color.Unspecified,
) {
    if (bestUserName != null && bestDisplayName != null && bestDisplayName != bestUserName) {
        UserAndUsernameDisplay(
            bestDisplayName.trim(),
            tags,
            bestUserName.trim(),
            modifier,
            showPlayButton,
            fontWeight,
            textColor,
        )
    } else if (bestDisplayName != null) {
        UserDisplay(bestDisplayName.trim(), tags, modifier, showPlayButton, fontWeight, textColor)
    } else if (bestUserName != null) {
        UserDisplay(bestUserName.trim(), tags, modifier, showPlayButton, fontWeight, textColor)
    } else {
        NPubDisplay(npubDisplay, modifier, fontWeight, textColor)
    }
}

@Composable
fun NPubDisplay(
    npubDisplay: String,
    modifier: Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color.Unspecified,
) {
    Text(
        text = npubDisplay,
        fontWeight = fontWeight,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = textColor,
    )
}

@Composable
private fun UserDisplay(
    bestDisplayName: String,
    tags: ImmutableListOfLists<String>?,
    modifier: Modifier,
    showPlayButton: Boolean = true,
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color.Unspecified,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CreateTextWithEmoji(
            text = bestDisplayName,
            tags = tags,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
            color = textColor,
        )
//        if (showPlayButton) {
//            Spacer(StdHorzSpacer)
//            DrawPlayName(bestDisplayName)
//        }
    }
}

@Composable
private fun UserAndUsernameDisplay(
    bestDisplayName: String,
    tags: ImmutableListOfLists<String>?,
    bestUserName: String,
    modifier: Modifier,
    showPlayButton: Boolean = true,
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color.Unspecified,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CreateTextWithEmoji(
            text = bestDisplayName,
            tags = tags,
            fontWeight = fontWeight,
            maxLines = 1,
            modifier = modifier,
            color = textColor,
        )
    /*
    CreateTextWithEmoji(
        text = remember { "@$bestUserName" },
        tags = tags,
        color = MaterialTheme.colorScheme.placeholderText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,

    )*/
//        if (showPlayButton) {
//            Spacer(StdHorzSpacer)
//            DrawPlayName(bestDisplayName)
//        }
    }
}

@Composable
fun DrawPlayName(name: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DrawPlayNameIcon { speak(name, context, lifecycleOwner) }
}

@Composable
fun DrawPlayNameIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = StdButtonSizeModifier) {
        PlayIcon(modifier = StdButtonSizeModifier, tint = MaterialTheme.colorScheme.placeholderText)
    }
}

private fun speak(
    message: String,
    context: Context,
    owner: LifecycleOwner,
) {
    TextToSpeechHelper.getInstance(context)
        .registerLifecycle(owner)
        .speak(message)
        .highlight()
        .onDone { Log.d("TextToSpeak", "speak: done") }
        .onError { Log.d("TextToSpeak", "speak error: $it") }
}
