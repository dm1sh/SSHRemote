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

fun buildPhysicalKeyBindingMap(remoteCommands: Map<RemoteControlKey, Command>?): Map<Int, RemoteControlKey> {
    if (remoteCommands.isNullOrEmpty()) {
        return emptyMap()
    }

    val bindings = linkedMapOf<Int, RemoteControlKey>()
    remoteCommands.forEach { (remoteControlKey, command) ->
        command.physicalKeyCodes.orEmpty().forEach { keyCode ->
            bindings[keyCode] = remoteControlKey
        }
    }
    return bindings
}

fun findPhysicalKeyBindingOwner(
    remoteCommands: Map<RemoteControlKey, Command>,
    keyCode: Int,
    excludingRemoteControlKey: RemoteControlKey? = null,
): RemoteControlKey? {
    return remoteCommands.entries.firstOrNull { (remoteControlKey, command) ->
        remoteControlKey != excludingRemoteControlKey && command.isBoundToPhysicalKey(keyCode)
    }?.key
}

fun upsertRemoteCommandWithExclusivePhysicalKeys(
    remoteCommands: Map<RemoteControlKey, Command>,
    remoteControlKey: RemoteControlKey,
    command: Command,
): Map<RemoteControlKey, Command> {
    val normalizedCommand = command.normalized()
    val physicalKeyCodes = normalizedCommand.physicalKeyCodes.orEmpty().toSet()

    return (remoteCommands + (remoteControlKey to normalizedCommand)).mapValues { (key, existingCommand) ->
        if (key == remoteControlKey || physicalKeyCodes.isEmpty()) {
            existingCommand.normalized()
        } else {
            existingCommand.copy(
                physicalKeyCodes = existingCommand.physicalKeyCodes?.filterNot(physicalKeyCodes::contains),
            ).normalized()
        }
    }
}
