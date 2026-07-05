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

import com.stefansundin.sshremote.data.settings.ExportedCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhysicalKeyBindingsTest {
    @Test
    fun normalized_removes_invalid_duplicate_physical_key_codes() {
        val command = Command(
            command = "echo hello",
            longPressCommand = "echo long",
            physicalKeyCodes = listOf(42, 0, 42, -1, 9),
        ).normalized()

        assertEquals(listOf(9, 42), command.physicalKeyCodes)
        assertEquals("echo hello", command.command)
        assertEquals("echo long", command.longPressCommand)
    }

    @Test
    fun exportedCommand_toCommand_preserves_physical_key_bindings() {
        val command = ExportedCommand(
            name = "Select",
            command = "input keyevent KEYCODE_DPAD_CENTER",
            physicalKeyCodes = listOf(100, 100, 0, 101),
        ).toCommand()

        assertEquals(listOf(100, 101), command.physicalKeyCodes)
    }

    @Test
    fun upsertRemoteCommandWithExclusivePhysicalKeys_moves_existing_bindings_to_new_command() {
        val updatedCommands = upsertRemoteCommandWithExclusivePhysicalKeys(
            remoteCommands = mapOf(
                RemoteControlKey.UP to Command("up", physicalKeyCodes = listOf(10, 11)),
                RemoteControlKey.DOWN to Command("down", physicalKeyCodes = listOf(12)),
            ),
            remoteControlKey = RemoteControlKey.SELECT,
            command = Command("select", physicalKeyCodes = listOf(11, 12, 13)),
        )

        assertEquals(listOf(10), updatedCommands[RemoteControlKey.UP]?.physicalKeyCodes)
        assertNull(updatedCommands[RemoteControlKey.DOWN]?.physicalKeyCodes)
        assertEquals(listOf(11, 12, 13), updatedCommands[RemoteControlKey.SELECT]?.physicalKeyCodes)
    }

    @Test
    fun buildPhysicalKeyBindingMap_returns_latest_binding_for_duplicate_keys() {
        val bindings = buildPhysicalKeyBindingMap(
            mapOf(
                RemoteControlKey.UP to Command("up", physicalKeyCodes = listOf(10)),
                RemoteControlKey.SELECT to Command("select", physicalKeyCodes = listOf(10, 11)),
            ),
        )

        assertEquals(RemoteControlKey.SELECT, bindings[10])
        assertEquals(RemoteControlKey.SELECT, bindings[11])
    }

    @Test
    fun findPhysicalKeyBindingOwner_ignores_the_current_remote_button() {
        val remoteCommands = mapOf(
            RemoteControlKey.UP to Command("up", physicalKeyCodes = listOf(10)),
            RemoteControlKey.SELECT to Command("select", physicalKeyCodes = listOf(11)),
        )

        assertEquals(
            RemoteControlKey.UP,
            findPhysicalKeyBindingOwner(
                remoteCommands,
                keyCode = 10,
                excludingRemoteControlKey = RemoteControlKey.SELECT,
            ),
        )
        assertNull(
            findPhysicalKeyBindingOwner(
                remoteCommands,
                keyCode = 11,
                excludingRemoteControlKey = RemoteControlKey.SELECT,
            ),
        )
    }
}
