package com.apps.bubblepopping.stage

/** A themed group of stages. Every world after the first unlocks with total stars. */
enum class World(val displayName: String, val emoji: String, val starsToUnlock: Int) {
    SKY("Sky", "☁️", 0),
    OCEAN("Ocean", "🌊", 20),
    SPACE("Space", "🌌", 45),
    CANDY("Candy", "🍬", 75),
}
