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

package com.stefansundin.sshremote.ui.components

import android.view.SoundEffectConstants
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.stefansundin.sshremote.R
import com.stefansundin.sshremote.data.host.Command
import com.stefansundin.sshremote.data.host.RemoteControlKey
import com.stefansundin.sshremote.data.host.findPhysicalKeyBindingOwner
import com.stefansundin.sshremote.ui.portraitImePadding
import android.view.KeyEvent as AndroidKeyEvent

@Composable
fun PhysicalKeyBindingsDialog(
    remoteControlKey: RemoteControlKey,
    initialKeyCodes: List<Int>,
    existingRemoteCommands: Map<RemoteControlKey, Command>,
    onDismiss: () -> Unit,
    onSave: (List<Int>) -> Unit,
) {
    var keyCodes by rememberSaveable { mutableStateOf(initialKeyCodes) }
    var isCapturingKey by rememberSaveable { mutableStateOf(false) }
    var pendingRebindKeyCode by rememberSaveable { mutableIntStateOf(-1) }
    var pendingRebindOwnerName by rememberSaveable { mutableStateOf<String?>(null) }
    val captureFocusRequester = remember { FocusRequester() }
    val view = LocalView.current
    val pendingRebind = pendingRebindKeyCode >= 0 && pendingRebindOwnerName != null

    LaunchedEffect(isCapturingKey) {
        if (isCapturingKey) {
            captureFocusRequester.requestFocus()
        }
    }

    fun addKeyCode(keyCode: Int) {
        keyCodes = (keyCodes + keyCode).distinct().sorted()
    }

    AlertDialog(
        title = { Text(stringResource(R.string.configure_physical_keys)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.physical_key_bindings_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    onClick = {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        isCapturingKey = !isCapturingKey
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(captureFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (!isCapturingKey) {
                                return@onPreviewKeyEvent false
                            }

                            val nativeKeyEvent = event.nativeKeyEvent
                            if (nativeKeyEvent.action != AndroidKeyEvent.ACTION_DOWN) {
                                return@onPreviewKeyEvent true
                            }

                            if (nativeKeyEvent.repeatCount > 0) {
                                return@onPreviewKeyEvent true
                            }

                            if (nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_BACK) {
                                isCapturingKey = false
                                return@onPreviewKeyEvent false
                            }

                            if (nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_UNKNOWN) {
                                return@onPreviewKeyEvent true
                            }

                            val keyCode = nativeKeyEvent.keyCode
                            val currentOwner = findPhysicalKeyBindingOwner(
                                remoteCommands = existingRemoteCommands,
                                keyCode = keyCode,
                                excludingRemoteControlKey = remoteControlKey,
                            )

                            when {
                                keyCodes.contains(keyCode) -> {
                                    isCapturingKey = false
                                }

                                currentOwner != null -> {
                                    pendingRebindKeyCode = keyCode
                                    pendingRebindOwnerName = view.context.getString(currentOwner.titleRes)
                                    isCapturingKey = false
                                }

                                else -> {
                                    addKeyCode(keyCode)
                                    isCapturingKey = false
                                }
                            }
                            true
                        },
                ) {
                    Text(
                        if (isCapturingKey) {
                            stringResource(R.string.press_physical_key_now)
                        } else {
                            stringResource(R.string.bind_physical_key)
                        },
                    )
                }
                if (keyCodes.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.tap_bound_key_to_remove),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        keyCodes.forEach { keyCode ->
                            OutlinedButton(
                                onClick = {
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    keyCodes = keyCodes - keyCode
                                },
                            ) {
                                Text(formatPhysicalKeyLabel(keyCode))
                            }
                        }
                    }
                }
            }
        },
        properties = DialogProperties(dismissOnClickOutside = false, decorFitsSystemWindows = false),
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onSave(keyCodes)
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = Modifier.portraitImePadding(),
    )

    if (pendingRebind) {
        AlertDialog(
            title = { Text(stringResource(R.string.rebind_physical_key_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.rebind_physical_key_message,
                        pendingRebindOwnerName!!,
                        stringResource(remoteControlKey.titleRes),
                    ),
                )
            },
            onDismissRequest = {
                pendingRebindKeyCode = -1
                pendingRebindOwnerName = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        addKeyCode(pendingRebindKeyCode)
                        pendingRebindKeyCode = -1
                        pendingRebindOwnerName = null
                    },
                ) {
                    Text(stringResource(R.string.rebind))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        pendingRebindKeyCode = -1
                        pendingRebindOwnerName = null
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun formatPhysicalKeyLabel(keyCode: Int): String {
    val keyName = AndroidKeyEvent.keyCodeToString(keyCode)
        .removePrefix("KEYCODE_")
        .split('_')
        .joinToString(" ") { part ->
            part.lowercase().replaceFirstChar { char -> char.titlecase() }
        }

    return keyName.ifBlank {
        keyCode.toString()
    }
}
