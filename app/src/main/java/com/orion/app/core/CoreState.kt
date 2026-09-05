package com.orion.app.core

/**
 * Drives every AI Core animation. Only one of these is active at a time;
 * IDLE breathing is the resting state everything else returns to.
 */
enum class CoreState {
    IDLE,       // slow breathing
    LISTENING,  // sound-wave rings travel INWARD toward the nucleus
    THINKING,   // constellation + particles orbit faster
    SEARCHING,  // a radar-style scan ring sweeps around the core
    EXECUTING,  // energy lines radiate OUTWARD from the nucleus
    COMPLETE,   // one strong outward light pulse, then decays back to IDLE
    ALERT       // disaster notice — core flashes red until acknowledged
}

data class ChatMessage(
    val role: Role,
    val text: String
) {
    enum class Role { USER, ORION }
}
