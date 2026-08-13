package com.phahoang.aquafarm.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phahoang.aquafarm.data.model.*
import com.phahoang.aquafarm.data.repository.AddFishResult
import com.phahoang.aquafarm.data.repository.BuyTankResult
import com.phahoang.aquafarm.data.repository.GameRepository
import com.phahoang.aquafarm.data.repository.UnlockAreaResult
import com.phahoang.aquafarm.service.GameEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GameRepository(app)
    private val engine = GameEngine()

    val player: StateFlow<Player> = repo.player
    val fishList: StateFlow<List<FishInstance>> = repo.fishList
    val tanks: StateFlow<List<Tank>> = repo.tanks
    val unlockedAreas: StateFlow<List<String>> = repo.unlockedAreas
    val selectedTankId: StateFlow<String?> = repo.selectedTankId

    private val _screen = MutableStateFlow("areas")
    val screen: StateFlow<String> = _screen.asStateFlow()

    private val _selectedAreaId = MutableStateFlow<String?>(null)
    val selectedAreaId: StateFlow<String?> = _selectedAreaId.asStateFlow()

    private val backStack = mutableListOf<String>()

    val messages: StateFlow<List<GameMessage>> = repo.messages

    // Tick counter - used to force UI recomposition for realtime timers
    private val _tick = MutableStateFlow(0L)
    val tick: StateFlow<Long> = _tick.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            repo.load()

            while (true) {
                delay(1000)
                repo.tick()
                _tick.value++
                // Auto save mỗi 5 giây
                if (_tick.value % 5 == 0L) {
                    repo.save()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.Default) { repo.save() }
    }

    // Save immediately (called from Activity lifecycle)
    fun saveNow() {
        viewModelScope.launch(Dispatchers.Default) { repo.save() }
    }

    // Navigation

    private fun navigateTo(screen: String) {
        backStack.add(_screen.value)
        _screen.value = screen
    }

    fun goBack(): Boolean {
        if (backStack.isEmpty()) return false
        _screen.value = backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun openArea(areaId: String) {
        _selectedAreaId.value = areaId
        navigateTo("tanks")
    }

    fun openTank(tankId: String) {
        repo.selectTank(tankId)
        navigateTo("tank_detail")
    }

    fun goToShop() {
        navigateTo("shop")
    }

    fun goHome() {
        backStack.clear()
        _screen.value = "areas"
        _selectedAreaId.value = null
    }

    // Actions

    fun feed(uid: String) {
        // Lưu số message trước khi cho ăn
        val beforeCount = repo.messages.value.size

        repo.feed(uid)
        _tick.value = _tick.value + 1

        // Lấy các message MỚI vừa thêm (sau khi feed)
        val afterMessages = repo.messages.value
        val newMessages = afterMessages.drop(beforeCount)

        // Set timer riêng cho từng message mới
        newMessages.forEachIndexed { index, msg ->
            viewModelScope.launch(Dispatchers.Default) {
                delay(2000L + index * 300L)    // hiện 2s

                // Bước 1: Bắt đầu animation biến mất
                repo.dismissMessage(msg.id)
                _tick.value = _tick.value + 1

                // Bước 2: Đợi animation xong rồi xoá khỏi list
                delay(400)
                repo.removeMessage(msg.id)
                _tick.value = _tick.value + 1
            }
        }
    }

    fun buyFish(speciesId: String): AddFishResult {
        val result = repo.addFish(speciesId)
        _tick.value = _tick.value + 1

        val sp = FishDB.get(speciesId)
        val text = when (result) {
            AddFishResult.Success -> "Đã thêm ${sp?.name} vào hồ!"
            AddFishResult.NotEnoughCoins -> "Không đủ tiền! Cần ${sp?.price}"
            AddFishResult.TankFull -> "Hồ đã đầy!"
            AddFishResult.InvalidSpecies -> "Loại cá không hợp lệ"
        }

        // Thêm message
        // ═══ XANH LÁ nếu thành công, ĐỎ nếu lỗi ═══
        val type = when (result) {
            AddFishResult.Success -> MessageType.COIN_EXP
            else -> MessageType.SHOP_ERROR
        }

        repo.addMessage(text, type)

        // Timer xoá
        val latestMsg = repo.messages.value.maxByOrNull { it.id }
        if (latestMsg != null) {
            viewModelScope.launch(Dispatchers.Default) {
                delay(2000)
                repo.dismissMessage(latestMsg.id)
                _tick.value = _tick.value + 1
                delay(400)
                repo.removeMessage(latestMsg.id)
                _tick.value = _tick.value + 1
            }
        }

        return result
    }

    fun buyTank(areaId: String): BuyTankResult {
        val result = repo.buyTank(areaId)
        _tick.value = _tick.value + 1
        return result
    }

    fun unlockArea(areaId: String): UnlockAreaResult {
        val result = repo.unlockArea(areaId)
        _tick.value = _tick.value + 1
        return result
    }

    // Helpers

    fun speciesOf(fish: FishInstance): FishSpecies? = FishDB.get(fish.speciesId)

    fun isHungry(fish: FishInstance): Boolean {
        val sp = FishDB.get(fish.speciesId) ?: return false
        return engine.isHungry(fish, sp)
    }

    fun timeUntilHungry(fish: FishInstance): String {
        val sp = FishDB.get(fish.speciesId) ?: return ""
        val ms = engine.timeUntilHungry(fish, sp)
        if (ms <= 0) return "Doi!"
        val sec = ms / 1000
        val m = sec / 60
        val s = sec % 60
        return "${m}:${String.format("%02d", s)}"
    }

    fun fishInTank(tankId: String): List<FishInstance> {
        return fishList.value.filter { it.tankId == tankId }
    }

    fun hasAnyHungryFishInTank(tankId: String): Boolean {
        return fishInTank(tankId).any { isHungry(it) }
    }

    fun tanksForArea(areaId: String): List<Tank> {
        return tanks.value.filter { it.areaId == areaId }
    }

    fun fishCountInTank(tankId: String): Int {
        return repo.fishCountInTank(tankId)
    }

    fun timeUntilHungryMs(fish: FishInstance): Long {
        val sp = FishDB.get(fish.speciesId) ?: return 0L
        return engine.timeUntilHungry(fish, sp)
    }

    fun formatMs(ms: Long): String {
        val sec = ms / 1000
        val m = sec / 60
        val s = sec % 60
        return "${m}:${String.format("%02d", s)}"
    }

    fun tanksInAreaCount(areaId: String): Int {
        return repo.tanksInAreaCount(areaId)
    }

    fun updatePlayerName(newName: String) = repo.updatePlayerName(newName)

}