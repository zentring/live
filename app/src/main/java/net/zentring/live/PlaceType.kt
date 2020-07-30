package net.zentring.live

enum class PlaceType(var placeTypeID: Int) {
    fairway(1),
    rough(2),
    fairway_bunker(3),
    green_bunker(4),
    green(5),
    in_hole(6),
    out_of_bounds(7),
    lost(8),
    immovable_obstacles(9),
    water(10),
    long_grass(11);

    fun getPlaceString(): String {
        return when (this) {
            fairway -> "球道"
            rough -> "长草"
            fairway_bunker -> "球道沙坑"
            green_bunker -> "果岭沙坑"
            green -> "果岭"
            in_hole -> "进洞"
            out_of_bounds -> "界外"
            lost -> "遗失球"
            immovable_obstacles -> "不可移动障碍物"
            water -> "水障碍"
            long_grass -> "深长草"
        }
    }
}