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

package com.stefansundin.sshremote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PendingVolumeRefreshState(
    val isAppActive: Boolean = false,
    val pendingHostId: String? = null,
    val pendingRefreshCounter: Long = 0,
    val handledRefreshCounter: Long = 0,
) {
    val hasPendingRefresh: Boolean
        get() = pendingHostId != null && pendingRefreshCounter > handledRefreshCounter
}

class PendingVolumeRefreshTracker {
    private val _state = MutableStateFlow(PendingVolumeRefreshState())
    val state: StateFlow<PendingVolumeRefreshState> = _state.asStateFlow()

    fun setAppActive(isAppActive: Boolean) {
        _state.update { current ->
            if (current.isAppActive == isAppActive) {
                current
            } else {
                current.copy(isAppActive = isAppActive)
            }
        }
    }

    fun markVolumeChanged(hostId: String) {
        _state.update { current ->
            current.copy(
                pendingHostId = hostId,
                pendingRefreshCounter = current.pendingRefreshCounter + 1,
            )
        }
    }

    fun markRefreshHandled(hostId: String, refreshCounter: Long) {
        _state.update { current ->
            if (
                current.pendingHostId != hostId ||
                refreshCounter <= current.handledRefreshCounter ||
                refreshCounter > current.pendingRefreshCounter
            ) {
                current
            } else {
                current.copy(
                    pendingHostId = if (refreshCounter == current.pendingRefreshCounter) null else current.pendingHostId,
                    handledRefreshCounter = refreshCounter,
                )
            }
        }
    }

    fun clearPending(hostId: String) {
        _state.update { current ->
            if (current.pendingHostId != hostId) {
                current
            } else {
                current.copy(
                    pendingHostId = null,
                    handledRefreshCounter = current.pendingRefreshCounter,
                )
            }
        }
    }
}
