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

package com.stefansundin.sshremote.data.host

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Keep
@Parcelize
data class Command(
    val command: String?,
    val longPressCommand: String? = null,
    val name: String? = null,
    val showOutput: Boolean = false,
    val renderOutputAsMarkdown: Boolean = false,
    val repeat: Boolean = false,
    val downCommand: String? = null,
    val upCommand: String? = null,
    val id: String = UUID.randomUUID().toString(),
) : Parcelable {
    fun hasTapCommand(): Boolean = !command.isNullOrBlank()

    fun hasLongPressCommand(): Boolean = !longPressCommand.isNullOrBlank()

    fun hasDownCommand(): Boolean = !downCommand.isNullOrBlank()

    fun hasUpCommand(): Boolean = !upCommand.isNullOrBlank()

    fun usesPressReleaseCommands(): Boolean = hasDownCommand() || hasUpCommand()

    fun hasRemoteAction(): Boolean {
        return if (usesPressReleaseCommands()) {
            hasDownCommand() || hasUpCommand()
        } else {
            hasTapCommand()
        }
    }

    fun canAddRemoteShortcut(): Boolean = hasTapCommand() && !usesPressReleaseCommands()

    fun displayText(): String = name ?: command ?: downCommand ?: upCommand.orEmpty()

    fun normalized(): Command {
        val normalizedCommand = command?.takeIf { it.isNotBlank() }
        val normalizedLongPressCommand = longPressCommand?.takeIf { it.isNotBlank() }
        val normalizedDownCommand = downCommand?.takeIf { it.isNotBlank() }
        val normalizedUpCommand = upCommand?.takeIf { it.isNotBlank() }

        return if (!normalizedDownCommand.isNullOrEmpty() || !normalizedUpCommand.isNullOrEmpty()) {
            copy(
                command = null,
                longPressCommand = null,
                repeat = false,
                downCommand = normalizedDownCommand,
                upCommand = normalizedUpCommand,
            )
        } else {
            copy(
                command = normalizedCommand,
                longPressCommand = normalizedLongPressCommand,
                downCommand = null,
                upCommand = null,
            )
        }
    }

    fun formatCommand(text: String): String {
        // Escape single quotes in the text to avoid breaking the command.
        // This may not be foolproof for all shell injection cases, so you should still be careful about what you're feeding this app.
        val escapedText = text.replace("'", "'\\''")
        val commandTemplate = requireNotNull(command) { "Command template is missing." }
        return commandTemplate.format(escapedText)
    }
}
