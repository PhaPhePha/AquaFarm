package com.phahoang.aquafarm.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.phahoang.aquafarm.data.model.*
import com.phahoang.aquafarm.service.GameEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.phahoang.aquafarm.data.model.MessageType

enum class AddFishResult {
    Success, NotEnoughCoins, InvalidSpecies, TankFull
}

enum class BuyTankResult {
    Success, NotEnoughCoins, AreaFull, AreaNotUnlocked
}

enum class UnlockAreaResult {
    Success, NotEnoughCoins, AlreadyUnlocked
}

class GameRepository(context: Context) {

    private val prefs = context.getSharedPreferences("aquafarm", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val engine = GameEngine()

    private val _player = MutableStateFlow(Player())
    val player: StateFlow<Player> = _player.asStateFlow()

    private val _fishList = MutableStateFlow<List<FishInstance>>(emptyList())
    val fishList: StateFlow<List<FishInstance>> = _fishList.asStateFlow()

    private val _tanks = MutableStateFlow<List<Tank>>(emptyList())
    val tanks: StateFlow<List<Tank>> = _tanks.asStateFlow()

    private val _unlockedAreas = MutableStateFlow<List<String>>(listOf("indoor"))
    val unlockedAreas: StateFlow<List<String>> = _unlockedAreas.asStateFlow()

    private val _selectedTankId = MutableStateFlow<String?>(null)
    val selectedTankId: StateFlow<String?> = _selectedTankId.asStateFlow()

    private val _messages = MutableStateFlow<List<GameMessage>>(emptyList())
    val messages: StateFlow<List<GameMessage>> = _messages.asStateFlow()

    // Load
    fun load() {
        val playerJson = prefs.getString("player", null)
        if (playerJson == null) {
            // Lan dau: 1 ho trong nha, 1 ca vang free
            _player.value = Player()
            _tanks.value = AreaDB.defaultTanks()
            _unlockedAreas.value = listOf("indoor")
            _player.value = Player(coins = 100000)
            addFish("goldfish", tankId = "tank1", free = true)
        } else {
            _player.value = gson.fromJson(playerJson, Player::class.java)

            val fishJson = prefs.getString("fish", "[]")
            val fishType = object : TypeToken<List<FishInstance>>() {}.type
            _fishList.value = gson.fromJson(fishJson, fishType)

            val tankJson = prefs.getString("tanks", null)
            if (tankJson != null) {
                val tankType = object : TypeToken<List<Tank>>() {}.type
                _tanks.value = gson.fromJson(tankJson, tankType)
            } else {
                _tanks.value = AreaDB.defaultTanks()
                _fishList.value = _fishList.value.map {
                    if (it.tankId.isBlank()) it.copy(tankId = DEFAULT_TANK_ID) else it
                }
            }

            val areasJson = prefs.getString("unlockedAreas", null)
            _unlockedAreas.value = if (areasJson != null) {
                val areaType = object : TypeToken<List<String>>() {}.type
                gson.fromJson(areasJson, areaType)
            } else {
                listOf("indoor")
            }
        }
        if (_selectedTankId.value == null) {
            _selectedTankId.value = _tanks.value.firstOrNull()?.id
        }
    }

    // Save
    fun save() {
        prefs.edit()
            .putString("player", gson.toJson(_player.value))
            .putString("fish", gson.toJson(_fishList.value))
            .putString("tanks", gson.toJson(_tanks.value))
            .putString("unlockedAreas", gson.toJson(_unlockedAreas.value))
            .apply()
    }

    // Chon ho
    fun selectTank(tankId: String?) {
        _selectedTankId.value = tankId
    }

    // So ca trong ho
    fun fishCountInTank(tankId: String): Int {
        return _fishList.value.count { it.tankId == tankId }
    }

    fun tankIsFull(tankId: String): Boolean {
        return fishCountInTank(tankId) >= MAX_FISH_PER_TANK
    }

    // Dem so ho trong khu vuc
    fun tanksInAreaCount(areaId: String): Int {
        return _tanks.value.count { it.areaId == areaId }
    }

    // Mua ca vao ho duoc chon
    fun addFish(speciesId: String, tankId: String? = null, free: Boolean = false): AddFishResult {
        val sp = FishDB.get(speciesId) ?: return AddFishResult.InvalidSpecies
        val targetTank = tankId ?: _selectedTankId.value ?: return AddFishResult.InvalidSpecies

        if (_tanks.value.none { it.id == targetTank }) return AddFishResult.InvalidSpecies
        if (tankIsFull(targetTank)) return AddFishResult.TankFull
        if (!free && _player.value.coins < sp.price) return AddFishResult.NotEnoughCoins

        if (!free) {
            _player.value = _player.value.copy(coins = _player.value.coins - sp.price)
        }

        val fish = FishInstance(
            uid = "f_${System.currentTimeMillis()}",
            speciesId = speciesId,
            tankId = targetTank
        )
        _fishList.value = _fishList.value + fish
        return AddFishResult.Success
    }

    // Mua them ho trong khu vuc
    fun buyTank(areaId: String): BuyTankResult {
        if (_unlockedAreas.value.none { it == areaId }) return BuyTankResult.AreaNotUnlocked

        val area = AreaDB.get(areaId) ?: return BuyTankResult.AreaNotUnlocked
        val currentCount = tanksInAreaCount(areaId)
        if (currentCount >= area.maxTanks) return BuyTankResult.AreaFull
        if (_player.value.coins < TANK_PRICE) return BuyTankResult.NotEnoughCoins

        _player.value = _player.value.copy(coins = _player.value.coins - TANK_PRICE)

        val tankNum = currentCount + 1
        val tank = Tank(
            id = AreaDB.newTankId(),
            name = "Hồ $tankNum",
            areaId = areaId
        )
        _tanks.value = _tanks.value + tank
        return BuyTankResult.Success
    }

    // Mo khoa khu vuc
    fun unlockArea(areaId: String): UnlockAreaResult {
        if (_unlockedAreas.value.contains(areaId)) return UnlockAreaResult.AlreadyUnlocked

        val area = AreaDB.get(areaId) ?: return UnlockAreaResult.AlreadyUnlocked
        if (_player.value.coins < area.unlockPrice) return UnlockAreaResult.NotEnoughCoins

        _player.value = _player.value.copy(coins = _player.value.coins - area.unlockPrice)
        _unlockedAreas.value = _unlockedAreas.value + areaId
        return UnlockAreaResult.Success
    }

    // Cho an
    fun feed(uid: String): GameEngine.FeedResult {
        val fish = _fishList.value.find { it.uid == uid } ?: return GameEngine.FeedResult.NotHungry
        val sp = FishDB.get(fish.speciesId) ?: return GameEngine.FeedResult.NotHungry

        val result = engine.feed(fish, sp)
        if (result is GameEngine.FeedResult.Ok) {
            val updated = fish.copy(
                lastFed = System.currentTimeMillis(),
                feedCount = result.newFeedCount,
                stars = result.newStars
            )
            _fishList.value = _fishList.value.map { if (it.uid == uid) updated else it }

            _player.value = _player.value.copy(
                coins = _player.value.coins + result.coins,
                totalCoinsEarned = _player.value.totalCoinsEarned + result.coins,
                totalFishFed = _player.value.totalFishFed + 1
            )
            val (newPlayer, lvlGained) = engine.addExp(_player.value, result.exp)
            _player.value = newPlayer.copy(
                totalCoinsEarned = _player.value.totalCoinsEarned,
                totalFishFed = _player.value.totalFishFed
            )

            // ═══ THÊM MESSAGE VÀO LIST ═══
            val text: String
            val type: MessageType

            // 1. Luôn hiện tiền + EXP
            addMessage("+${result.coins}💰  +${result.exp}⭐", MessageType.COIN_EXP)

            // 2. Nếu cá lên sao
            if (result.grewUp) {
                addMessage("🌟 ${sp.name} lên ${updated.stars} sao!", MessageType.STAR_UP)
            }

            // 3. Nếu player lên level
            if (lvlGained > 0) {
                addMessage("⭐ Lên Level ${newPlayer.level}!", MessageType.LEVEL_UP)
            }
        }
        return result
    }

    fun addMessage(text: String, type: MessageType = MessageType.COIN_EXP) {
        _messages.value = _messages.value + GameMessage(text = text, type = type)
    }


    fun tick() {
        _fishList.value = _fishList.value.toList()
    }

    fun dismissMessage(id: Long) {
        _messages.value = _messages.value.map {
            if (it.id == id) it.copy(isDismissing = true) else it
        }
    }

    fun removeMessage(id: Long) {
        _messages.value = _messages.value.filter { it.id != id }
    }

    fun updatePlayerName(newName: String) {
        _player.value = _player.value.copy(name = newName.trim())
    }
}