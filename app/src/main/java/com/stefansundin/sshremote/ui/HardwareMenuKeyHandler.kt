/*
 * SSH Remote
 * Copyright (C) 2026  Stefan Sundin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.stefansundin.sshremote.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

interface HardwareMenuKeyHandlerHost {
    fun registerHardwareMenuKeyHandler(key: Any, handler: () -> Boolean)
    fun unregisterHardwareMenuKeyHandler(key: Any)
}

@Composable
fun HardwareMenuKeyHandler(onMenuPressed: () -> Boolean) {
    val context = LocalContext.current
    val handlerHost = remember(context) { context.findHardwareMenuKeyHandlerHost() }
    val currentHandler by rememberUpdatedState(onMenuPressed)
    val handlerKey = remember { Any() }

    DisposableEffect(handlerHost, handlerKey) {
        handlerHost?.registerHardwareMenuKeyHandler(handlerKey) { currentHandler() }
        onDispose {
            handlerHost?.unregisterHardwareMenuKeyHandler(handlerKey)
        }
    }
}

private tailrec fun Context.findHardwareMenuKeyHandlerHost(): HardwareMenuKeyHandlerHost? = when (this) {
    is HardwareMenuKeyHandlerHost -> this
    is ContextWrapper -> baseContext.findHardwareMenuKeyHandlerHost()
    else -> null
}
