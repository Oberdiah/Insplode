package com.oberdiah.upgrades

enum class Upgrade(
    val title: String,
    val description: String,
    val price: Int,
    // If dependsOn is empty, then this upgrade depends on the one above it and nothing else.
    val dependsOn: List<Upgrade> = listOf(),
    val toggleable: Boolean = false,
) {
    Movement(
        "Movement",
        "Tap anywhere on the screen and drag left and right to move.",
        1,
    ),

    // ################ EARLY-GAME UPGRADES ################
    MediumTimedBomb(
        "Timed Bomb",
        "Occasionally drop a bomb that explodes after a short delay.",
        5
    ),
    ComboOrbs(
        "Combo Orbs",
        "When a bomb explodes another, orbs are spawned.",
        10,
        listOf(Movement),
    ),
    BombMagician(
        "Bomb Magician",
        "Touching a bomb while its fuse is in the last quarter of its life replenishes it.",
        5,
    ),
    Jump(
        "Jump",
        "Tap to jump.",
        5,
        listOf(ComboOrbs, MediumTimedBomb)
    ),

    // ################ JUMP UPGRADES ################


    FasterMovement(
        "Faster Movement",
        "Move faster.",
        15,
    ),
    MoreSkyOrbs(
        "More Orbs",
        "More orbs fall from the sky.",
        10,
    ),
    SmallTimedBomb(
        "Small Timed Bomb",
        "Extra tiny for easier combos",
        5
    ),
    LineBomb(
        "Line Bomb",
        "A harmless bomb that explodes in a line, collapsing large swathes of terrain.",
        10,
    ),
    Slam(
        "Slam",
        "Slam down to the ground to create an explosion.",
        15,
    ),

    // ################ SLAM UPGRADES ################

    BombSlam(
        "Bomb Slam",
        "Get propelled into the air after slamming into a bomb.",
        10,
    ),
    SlamOrbs(
        "Slam Orbs",
        "Orbs are spawned for each successful bomb slam.",
        10,
    ),
    ApexJetpack(
        "Apex Jetpack",
        "Hover slightly at the apex of your jump",
        15,
        toggleable = true,
    ),
    SneakySlam(
        "Sneaky Slam",
        "Slam when a fuse is half-depleted to get double-orbs without creating any explosion.",
        10,
    ),
    SlamAssist(
        "Slam Assist",
        "A line that indicates where the player is moving to and lights up when they reach it.",
        10,
        toggleable = true,
    ),
    Magnet(
        "Magnet",
        "Attract orbs to you.",
        15,
    ),
    GhostMode(
        "Ghost Mode",
        "While you're in the air bombs will phase through you harmlessly.",
        20,
    ),
    Multiplier(
        "Multiplier",
        "Get a multiplier after each successful bomb slam. Multiplies the number of slam orbs spawned.",
        10,
    ),
    MegaMagnet(
        "Mega Magnet",
        "Covers most of the screen.",
        20,
    ),

    // ################ POST-GAME UPGRADES ################

    EvenFasterMovement(
        "Even Faster Movement",
        "Move ridiculously fast.",
        25,
    ),
    SingularityMagnet(
        "Singularity Magnet",
        "The orbs simply cannot get away.",
        25,
    ),
    RainbowPlayer(
        "Rainbow Player",
        "Why not?",
        25,
        toggleable = true,
    ),
    ParticleBlackHole(
        "Particle Black Hole",
        "All particles are attracted to you.",
        25,
        toggleable = true,
    ),
}