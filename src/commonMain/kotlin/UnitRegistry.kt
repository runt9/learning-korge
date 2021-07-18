object UnitRegistry {
    val team1 = mutableSetOf<GameUnit>()
    val team2 = mutableSetOf<GameUnit>()
    val all
        get() = team1 + team2
}
