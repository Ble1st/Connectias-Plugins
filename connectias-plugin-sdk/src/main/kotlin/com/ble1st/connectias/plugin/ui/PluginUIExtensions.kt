// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2025 Connectias

package com.ble1st.connectias.plugin.ui

/**
 * Extension functions for converting between SDK data classes and AIDL Parcels.
 *
 * Three-Process Architecture requires IPC-compatible Parcel types for communication
 * between Sandbox Process and UI Process. These extension functions provide
 * seamless conversion between the SDK-side data classes and AIDL Parcelables.
 */

/**
 * Converts UIStateData (SDK-side) to UIStateParcel (AIDL-compatible).
 *
 * This conversion is necessary for sending UI state from the Sandbox Process
 * to the UI Process via IPC.
 */
fun UIStateData.toParcel(): UIStateParcel {
    val parcel = UIStateParcel()
    parcel.screenId = this.screenId
    parcel.title = this.title
    parcel.data = this.data
    parcel.components = this.components.map { it.toParcel() }.toTypedArray()
    parcel.timestamp = this.timestamp
    return parcel
}

/**
 * Converts UIComponentData (SDK-side) to UIComponentParcel (AIDL-compatible).
 *
 * Recursively converts nested components (children).
 */
fun UIComponentData.toParcel(): UIComponentParcel {
    val parcel = UIComponentParcel()
    parcel.id = this.id
    parcel.type = this.type
    parcel.properties = this.properties
    parcel.children = this.children.map { it.toParcel() }.toTypedArray()
    return parcel
}

/**
 * Converts UserActionParcel (AIDL-compatible) to UserAction (SDK-side).
 *
 * This conversion is necessary for plugins to receive user actions from the
 * UI Process in a type-safe manner.
 */
fun UserActionParcel.toUserAction(): UserAction {
    return UserAction(
        actionType = this.actionType,
        targetId = this.targetId,
        data = this.data,
        timestamp = this.timestamp
    )
}

/**
 * Converts UserAction (SDK-side) to UserActionParcel (AIDL-compatible).
 *
 * This conversion is used when the plugin needs to send user actions via IPC.
 */
fun UserAction.toParcel(): UserActionParcel {
    val parcel = UserActionParcel()
    parcel.actionType = this.actionType
    parcel.targetId = this.targetId
    parcel.data = this.data
    parcel.timestamp = this.timestamp
    return parcel
}

/**
 * Converts UIStateParcel (AIDL-compatible) to UIStateData (SDK-side).
 *
 * This is useful for testing or when plugins need to work with SDK data classes.
 */
fun UIStateParcel.toUIStateData(): UIStateData {
    return UIStateData(
        screenId = this.screenId,
        title = this.title,
        data = this.data,
        components = this.components.map { it.toUIComponentData() },
        timestamp = this.timestamp
    )
}

/**
 * Converts UIComponentParcel (AIDL-compatible) to UIComponentData (SDK-side).
 *
 * Recursively converts nested components (children).
 */
fun UIComponentParcel.toUIComponentData(): UIComponentData {
    return UIComponentData(
        id = this.id,
        type = this.type,
        properties = this.properties,
        children = this.children.map { it.toUIComponentData() }
    )
}
