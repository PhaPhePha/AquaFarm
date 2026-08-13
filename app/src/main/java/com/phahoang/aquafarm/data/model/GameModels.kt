package com.phahoang.aquafarm.data.model

//  PLAYER

data class Player(
    val name: String = "FishFramer00",
    val level: Int = 1,
    val currentExp: Int = 0,
    val coins: Long = 500,
    val gems: Int = 0,
    val totalFishFed: Long = 0,
    val totalCoinsEarned: Long = 0
) {
    fun expToNextLevel(): Int = (100 * Math.pow(1.15, level.toDouble())).toInt()
    fun expProgress(): Float = currentExp.toFloat() / expToNextLevel().toFloat()
}

//  FISH
enum class FishSize { SMALL, MEDIUM, LARGE }

data class FishSpecies(
    val id: String,
    val name: String,
    val icon: String,
    val size: FishSize,
    val price: Int,
    val coinPerFeed: Int,
    val expPerFeed: Int,
    val hungerMinutes: Int,
    val feedsToGrow: Int,
    val maxStars: Int = 5
)

data class FishInstance(
    val uid: String,
    val speciesId: String,
    val tankId: String = DEFAULT_TANK_ID,
    val stars: Int = 0,
    val feedCount: Int = 0,
    val lastFed: Long = System.currentTimeMillis()
)

//  KHU VUC & HO
const val MAX_FISH_PER_TANK = 5
const val DEFAULT_TANK_ID = "tank1"
const val TANK_PRICE = 300

// Mot slot ho ca nam trong 1 khu vuc
data class Tank(
    val id: String,
    val name: String,
    val areaId: String = "indoor"
)

// Mot khu vuc chua nhieu slot ho
data class GameArea(
    val id: String,
    val name: String,
    val icon: String,
    val maxTanks: Int,
    val unlockPrice: Long = 0
)

// Khu vuc co san (Trong Nha) va kho duoc mo khoa
object AreaDB {
    val indoor = GameArea(
        id = "indoor", name = "Trong Nhà", icon = "🏠",
        maxTanks = 3, unlockPrice = 0
    )

    // Khu vuc co the mo khoa
    val unlockable = listOf(
        GameArea(
            id = "yard", name = "Ngoài Sân", icon = "🌤",
            maxTanks = 3, unlockPrice = 1000
        ),
        GameArea(
            id = "garden", name = "Sau Vườn", icon = "🌿",
            maxTanks = 5, unlockPrice = 2000
        )
    )

    val allAreas = listOf(indoor) + unlockable

    fun get(id: String): GameArea? = allAreas.find { it.id == id }

    fun defaultTanks(): List<Tank> = listOf(
        Tank(id = "tank1", name = "Hồ 1", areaId = "indoor")
    )

    fun newTankId(): String = "tank_${System.currentTimeMillis()}"
}


// 3 LOAI CA
object FishDB {
    val species = listOf(
        FishSpecies(
            id = "goldfish", name = "Cá Vàng", icon = "🐟",
            size = FishSize.SMALL, price = 50,
            coinPerFeed = 15, expPerFeed = 10,
            hungerMinutes = 2, feedsToGrow = 10
        ),
        FishSpecies(
            id = "discus", name = "Cá Dĩa", icon = "🐠",
            size = FishSize.MEDIUM, price = 200,
            coinPerFeed = 40, expPerFeed = 25,
            hungerMinutes = 5, feedsToGrow = 8
        ),
        FishSpecies(
            id = "puffer", name = "Cá Nóc", icon = "🐡",
            size = FishSize.LARGE, price = 500,
            coinPerFeed = 100, expPerFeed = 60,
            hungerMinutes = 10, feedsToGrow = 5
        )
    )

    val map = species.associateBy { it.id }

    fun get(id: String): FishSpecies? = map[id]
}

enum class MessageType {
    COIN_EXP,      // Thu thập tiền + EXP
    LEVEL_UP,      // Player lên cấp
    STAR_UP,         // Cá lên sao

    SHOP_ERROR
}

data class GameMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val type: MessageType = MessageType.COIN_EXP,
    val timestamp: Long = System.currentTimeMillis(),
    val isDismissing: Boolean = false
)

