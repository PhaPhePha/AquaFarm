package com.phahoang.aquafarm.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phahoang.aquafarm.data.model.*
import com.phahoang.aquafarm.data.repository.AddFishResult
import com.phahoang.aquafarm.data.repository.BuyTankResult
import com.phahoang.aquafarm.data.repository.UnlockAreaResult
import com.phahoang.aquafarm.ui.components.theme.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.phahoang.aquafarm.data.model.MessageType
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.tween
import com.phahoang.aquafarm.ui.components.PlayerStatusBar
import androidx.compose.foundation.layout.PaddingValues


private val GradientBg = Brush.verticalGradient(listOf(DeepOcean, DarkWater, DeepOcean))

@Composable
fun AquaFarmApp(vm: MainViewModel) {
    val screen by vm.screen.collectAsState()

    BackHandler {
        vm.goBack()
    }

    when (screen) {
        "areas" -> AreasScreen(vm)
        "tanks" -> TanksScreen(vm)
        "tank_detail" -> TankDetailScreen(vm)
        "shop" -> ShopScreen(vm)
    }
}

// =====================================================================
// SCREEN 1: CHON KHU VUC
// =====================================================================

@Composable
fun AreasScreen(vm: MainViewModel) {
    val player by vm.player.collectAsState()
    val messages by vm.messages.collectAsState()
    val unlockedAreas by vm.unlockedAreas.collectAsState()
    val tick by vm.tick.collectAsState()

    var showUnlockForm by remember { mutableStateOf(false) }
    var unlockMsg by remember { mutableStateOf<String?>(null) }
    var showPlayerDetail by remember { mutableStateOf(false) }

    if (showUnlockForm) {
        BackHandler { showUnlockForm = false; unlockMsg = null }
    }
    if (showPlayerDetail) {
        BackHandler { showPlayerDetail = false }  // ← THÊM
    }

    Box(Modifier.fillMaxSize().background(GradientBg).statusBarsPadding()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showPlayerDetail = true },  // ← THÊM clickable
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        // ── Hàng 1: Tên ──
                        Text(player.name, color = Blue, fontSize = 20.sp)

                        // ── Hàng 2: Level + Tiền ──
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column {
                                Text("Lv.${player.level}", color = Gold, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                Text("${player.currentExp}/${player.expToNextLevel()} EXP", color = TextMuted, fontSize = 13.sp)
                            }
                            Text(
                                "${String.format("%,d", player.coins)} $",
                                color = Green,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = player.expProgress().coerceIn(0f, 1f),
                            Modifier.fillMaxWidth().height(10.dp),
                            color = Gold, trackColor = DeepOcean
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "Nhấn để xem chi tiết",
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(LightBlue900.copy(alpha = 0.3f), CardLight.copy(alpha = 0.3f) )),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🌊 AquaFarm 🐳",
                        color = Blue,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }


            item {
                Text("🏡 Chọn khu vực", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            items(unlockedAreas, key = { it }) { areaId ->
                val area = AreaDB.get(areaId) ?: return@items
                val tankCount = vm.tanksInAreaCount(areaId)
                Card(
                    Modifier.fillMaxWidth().clickable { vm.openArea(areaId) },
                    colors = CardDefaults.cardColors(containerColor = DarkWater),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${area.name}", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("${tankCount} Hồ / ${area.maxTanks} Hồ", color = TextMuted, fontSize = 13.sp)
                        }
                        Text(">", color = TextMuted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth().clickable { showUnlockForm = true },
                    colors = CardDefaults.cardColors(containerColor = Blue300.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("+ ", color = Green, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("Mở khóa khu vực 🔒", color = Blue, fontSize = 16.sp)
                    }
                }
            }
        }

        if (showUnlockForm) {
            Box(
                Modifier.fillMaxSize().background(DeepOcean.copy(alpha = 0.85f)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Mở khóa khu vực", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showUnlockForm = false; unlockMsg = null }) {
                                Text("❌", color = TextMuted, fontSize = 18.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        AreaDB.unlockable.forEach { area ->
                            val alreadyUnlocked = unlockedAreas.contains(area.id)
                            val canBuy = player.coins >= area.unlockPrice && !alreadyUnlocked

                            Card(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = CardLight),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("${area.name}", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("Tối đa ${area.maxTanks} hồ", color = TextMuted, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val result = vm.unlockArea(area.id)
                                            unlockMsg = when (result) {
                                                UnlockAreaResult.Success -> "Đã mở khóa ${area.name}!"
                                                UnlockAreaResult.NotEnoughCoins -> "Không đủ! Cần ${area.unlockPrice}"
                                                UnlockAreaResult.AlreadyUnlocked -> "Đã được mở khóa!"
                                            }
                                        },
                                        enabled = canBuy,
                                        colors = ButtonDefaults.buttonColors(containerColor = if (alreadyUnlocked) CardDark else if (canBuy) Blue else CardDark),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (alreadyUnlocked) "Đã mở" else "${area.unlockPrice}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        if (unlockMsg != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(unlockMsg!!, color = Orange, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        // ═══ PLAYER DETAIL OVERLAY ═══
        if (showPlayerDetail) {
            PlayerDetailOverlay(player = player, onDismiss = { showPlayerDetail = false }, vm = vm)
        }
    }

}

@Composable
fun PlayerDetailOverlay(player: Player, onDismiss: () -> Unit, vm: MainViewModel)  {
    // Tính thống kê
    val expNeeded = player.expToNextLevel()
    val expProgress = player.expProgress().coerceIn(0f, 1f)
    val expPercent = (expProgress * 100).toInt()

    var isEditing by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(player.name) }

    Box(
        Modifier
            .fillMaxSize()
            .background(DeepOcean.copy(alpha = 0.88f))
            .clickable { onDismiss() }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            Modifier
                .fillMaxWidth()
                .clickable { },  // chặn click xuyên qua
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Header ──
                Text(
                    "Thống kê người chơi",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                // ═══ TÊN NGƯỜI CHƠI ═══
                if (isEditing) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it.take(20) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Tên người chơi", color = TextMuted) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = TextWhite,
                                fontSize = 16.sp
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (nameInput.isNotBlank()) {
                                    vm.updatePlayerName(nameInput)
                                }
                                isEditing = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Green),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text("Lưu", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            nameInput = player.name
                            isEditing = true
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            player.name,
                            color = TextWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("✏️", fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Level badge ──
                Box(
                    Modifier
                        .size(100.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Gold.copy(alpha = 0.3f), CardDark)
                            ),
                            shape = RoundedCornerShape(50.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${player.level}",
                            color = Gold,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Stats rows ──
                StatRow(icon = "💰", label = "Số dư", value = "${String.format("%,d", player.coins)} $", valueColor = Green)
                Spacer(Modifier.height(12.dp))

                StatRow(icon = "📈", label = "EXP hiện tại", value = "${player.currentExp} / $expNeeded", valueColor = Gold)
                Spacer(Modifier.height(8.dp))

                // ── EXP progress bar ──
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tiến trình lên Level ${player.level + 1}", color = TextMuted, fontSize = 12.sp)
                        Text("$expPercent%", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = expProgress,
                        Modifier.fillMaxWidth().height(10.dp),
                        color = Gold, trackColor = DeepOcean
                    )
                }

                Spacer(Modifier.height(12.dp))

                StatRow(icon = "🐟", label = "Tổng cá đã cho ăn", value = "${player.totalFishFed}", valueColor = TextWhite)
                Spacer(Modifier.height(12.dp))

                StatRow(icon = "💰", label = "Tổng tiền đã kiếm", value = "${String.format("%,d", player.totalCoinsEarned)} $", valueColor = Green)

                Spacer(Modifier.height(24.dp))

                // ── Nút đóng ──
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Đóng", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatRow(icon: String, label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(label, color = TextMuted, fontSize = 15.sp)
        }
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// =====================================================================
// SCREEN 2: DANH SACH HO TRONG KHU VUC
// =====================================================================

@Composable
fun TanksScreen(vm: MainViewModel) {
    val player by vm.player.collectAsState()
    val selectedAreaId by vm.selectedAreaId.collectAsState()
    val tanks by vm.tanks.collectAsState()
    val tick by vm.tick.collectAsState()
    val areaId = selectedAreaId ?: "indoor"
    val area = AreaDB.get(areaId)
    val areaName = area?.name ?: areaId
    val areaTanks = tanks.filter { it.areaId == areaId }
    val maxTanks = area?.maxTanks ?: 3
    val canBuyMore = areaTanks.size < maxTanks

    var buyMsg by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(GradientBg).statusBarsPadding()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PlayerStatusBar(player)
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { vm.goBack() }) {
                        Text("← Quay lại", color = Blue, fontSize = 15.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${String.format("%,d", player.coins)} $", color = Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Text(areaName, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            if (areaTanks.isEmpty()) {
                item {
                    Text("Chưa có hồ. Nhấn \"+ Mua hồ\" để nuôi cá!", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 24.dp))
                }
            } else {
                items(areaTanks, key = { it.id }) { tank ->
                    val fishInTank = vm.fishInTank(tank.id)
                    val fishCount = fishInTank.size
                    val anyHungry = vm.hasAnyHungryFishInTank(tank.id)

                    // Read tick here to force recomposition every second
                    @Suppress("UNUSED_VARIABLE")
                    val currentTick = tick

                    val nextHungryText = if (fishInTank.isNotEmpty() && !anyHungry) {
                        val nextMs = fishInTank.minOfOrNull { fish -> vm.timeUntilHungryMs(fish).let { if (it > 0) it else Long.MAX_VALUE } } ?: Long.MAX_VALUE
                        if (nextMs < Long.MAX_VALUE) "Đói sau: ${vm.formatMs(nextMs)} ⏱️" else null
                    } else null

                    Card(
                        Modifier.fillMaxWidth().clickable { vm.openTank(tank.id) },
                        colors = CardDefaults.cardColors(containerColor = CardLight),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(tank.name, color = Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("${fishCount}/${MAX_FISH_PER_TANK} 🐟", color = TextMuted, fontSize = 13.sp)
                                if (fishInTank.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    if (anyHungry) {
                                        Text("Có cá đói! 🍽️", color = Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else if (nextHungryText != null) {
                                        Text(nextHungryText, color = TextMuted, fontSize = 12.sp)
                                    }
                                }
                            }
                            Text(">", color = TextMuted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (canBuyMore) {
                item {
                    Card(
                        Modifier.fillMaxWidth().clickable {
                            val result = vm.buyTank(areaId)
                            buyMsg = when (result) {
                                BuyTankResult.Success -> "Đã mua hồ mới!"
                                BuyTankResult.NotEnoughCoins -> "Không đủ tiền! Cần ${TANK_PRICE}$"
                                BuyTankResult.AreaFull -> "Đã đầy hồ!"
                                BuyTankResult.AreaNotUnlocked -> "Chưa mở khóa khu vực!"
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text("+ Mua hồ", color = Blue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("  (${TANK_PRICE})", color = TextMuted, fontSize = 14.sp)
                        }
                    }
                }
            }

            if (buyMsg != null) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Orange.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(buyMsg!!, Modifier.padding(14.dp), color = Orange, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// =====================================================================
// SCREEN 3: CHI TIET HO CA
// =====================================================================

@Composable
fun TankDetailScreen(vm: MainViewModel) {
    val player by vm.player.collectAsState()
    val selectedTankId by vm.selectedTankId.collectAsState()
    val tankId = selectedTankId ?: ""
    val tanks by vm.tanks.collectAsState()
    val tankName = tanks.find { it.id == tankId }?.name ?: "Hồ"
    val messages by vm.messages.collectAsState()
    val tick by vm.tick.collectAsState()

    val fishList by vm.fishList.collectAsState()
    val fishInTank = fishList.filter { it.tankId == tankId }

    Box(Modifier.fillMaxSize().background(GradientBg).statusBarsPadding()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                PlayerStatusBar(player)
            }
            // ── Header: nút quay lại + tiền ──
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { vm.goBack() }) {
                        Text("← Quay lại", color = Blue, fontSize = 15.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${String.format("%,d", player.coins)} $", color = Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Tên hồ + số cá ──
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(tankName, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${fishInTank.size}/$MAX_FISH_PER_TANK 🐟", color = TextMuted, fontSize = 14.sp)
                }
            }

            // ── Nút mua cá ──
            item {
                Button(
                    onClick = { vm.goToShop() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("+ Mua cá", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Danh sách cá ──
            if (fishInTank.isEmpty()) {
                item {
                    Text("Chưa có cá. Nhấn \"+ Mua cá\" để thêm!", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 24.dp))
                }
            } else {
                items(fishInTank, key = { it.uid }) { fish ->
                    val currentTick = tick

                    val sp = vm.speciesOf(fish) ?: return@items
                    val hungry = vm.isHungry(fish)
                    val countdown = vm.timeUntilHungry(fish)
                    val starsStr = "★".repeat(fish.stars) + "☆".repeat(sp.maxStars - fish.stars)

                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardLight),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(sp.icon, fontSize = 32.sp)
                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(sp.name, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(starsStr, fontSize = 11.sp)
                                Spacer(Modifier.height(2.dp))

                                if (hungry) {
                                    Text("Đói! 🍽️", color = Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Đói sau: $countdown ⏱️", color = TextMuted, fontSize = 12.sp)
                                }

                                Text("Ăn ${fish.feedCount}/${sp.feedsToGrow} lần để lên sao", color = TextMuted, fontSize = 10.sp)
                                LinearProgressIndicator(
                                    progress = fish.feedCount.toFloat() / sp.feedsToGrow,
                                    Modifier.fillMaxWidth().height(5.dp),
                                    color = Gold, trackColor = DeepOcean
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Button(
                                onClick = { vm.feed(fish.uid) },
                                enabled = hungry,
                                modifier = Modifier.height(44.dp).width(68.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hungry) Orange else CardDark,
                                    disabledContainerColor = CardDark
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Ăn", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // ═══ MESSAGE FLOAT STACK ═══
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            messages.forEach { msg ->
                // ═══ ANIMATED VISIBILITY CHO TỪNG MESSAGE ═══
                AnimatedVisibility(
                    visible = !msg.isDismissing,
                    enter = fadeIn(animationSpec = tween(300)) +
                            slideInVertically(
                                animationSpec = tween(300),
                                initialOffsetY = { -it }
                            ),
                    exit = fadeOut(animationSpec = tween(400)) +
                            slideOutVertically(
                                animationSpec = tween(400),
                                targetOffsetY = { -it / 2 }
                            )
                ) {
                    val bgColor = when (msg.type) {
                        MessageType.COIN_EXP -> Green.copy(alpha = 0.3f)
                        MessageType.LEVEL_UP -> Gold.copy(alpha = 0.4f)
                        MessageType.STAR_UP -> Color(0xFF9C27B0).copy(alpha = 0.35f)
                        MessageType.SHOP_ERROR -> RedAccent.copy(alpha = 0.35f)
                    }

                    val textColor = when (msg.type) {
                        MessageType.COIN_EXP -> Color(0xFFB9F6CA)
                        MessageType.LEVEL_UP -> Color(0xFFFFF9C4)
                        MessageType.STAR_UP -> Color(0xFFE1BEE7)
                        MessageType.SHOP_ERROR -> Color(0xFFFFCDD2)

                    }

                    val borderColor = when (msg.type) {
                        MessageType.COIN_EXP -> Green.copy(alpha = 0.6f)
                        MessageType.LEVEL_UP -> Gold.copy(alpha = 0.7f)
                        MessageType.STAR_UP -> Color(0xFF9C27B0).copy(alpha = 0.6f)
                        MessageType.SHOP_ERROR -> RedAccent.copy(alpha = 0.7f)
                    }

                    val icon = when (msg.type) {
                        MessageType.COIN_EXP -> "💰"
                        MessageType.LEVEL_UP -> "⭐"
                        MessageType.STAR_UP -> "🌟"
                        MessageType.SHOP_ERROR -> "❌"
                    }

                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(icon, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                msg.text,
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


// =====================================================================
// SCREEN 4: SHOP
// =====================================================================

@Composable
fun ShopScreen(vm: MainViewModel) {
    val player by vm.player.collectAsState()
    val messages by vm.messages.collectAsState()  // ← THÊM
    val tick by vm.tick.collectAsState()           // ← THÊM

    Box(Modifier.fillMaxSize().background(GradientBg).statusBarsPadding()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {

            PlayerStatusBar(player)

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { vm.goBack() }) {
                    Text("← Quay lại", color = Blue, fontSize = 15.sp)
                }
                Text("Mua cá", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("${String.format("%,d", player.coins)} $", color = Green, fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))

            // XOÁ đoạn snackbarMessage ở đây

            FishDB.species.forEach { sp ->
                val canBuy = player.coins >= sp.price

                Card(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = CardLight),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sp.icon, fontSize = 44.sp)
                        Spacer(Modifier.width(14.dp))

                        Column(Modifier.weight(1f)) {
                            Text(sp.name, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when (sp.size) {
                                    FishSize.SMALL -> "Nhỏ"
                                    FishSize.MEDIUM -> "Vừa"
                                    FishSize.LARGE -> "Lớn"
                                } + " ${sp.hungerMinutes}p/Chu kỳ\n${sp.coinPerFeed} $ + ${sp.expPerFeed} EXP",
                                color = TextMuted, fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { vm.buyFish(sp.id) },
                            enabled = canBuy,
                            colors = ButtonDefaults.buttonColors(containerColor = if (canBuy) Green else CardDark),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("${sp.price} $", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // ═══ MESSAGE FLOAT (giống các screen khác) ═══
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            messages.forEach { msg ->
                AnimatedVisibility(
                    visible = !msg.isDismissing,
                    enter = fadeIn(animationSpec = tween(300)) +
                            slideInVertically(animationSpec = tween(300), initialOffsetY = { -it }),
                    exit = fadeOut(animationSpec = tween(400)) +
                            slideOutVertically(animationSpec = tween(400), targetOffsetY = { -it / 2 })
                ) {
                    val bgColor = when (msg.type) {
                        MessageType.COIN_EXP -> Green.copy(alpha = 0.3f)
                        MessageType.LEVEL_UP -> Gold.copy(alpha = 0.4f)
                        MessageType.STAR_UP -> Color(0xFF9C27B0).copy(alpha = 0.35f)
                        MessageType.SHOP_ERROR -> RedAccent.copy(alpha = 0.35f)
                    }

                    val textColor = when (msg.type) {
                        MessageType.COIN_EXP -> Color(0xFFB9F6CA)
                        MessageType.LEVEL_UP -> Color(0xFFFFF9C4)
                        MessageType.STAR_UP -> Color(0xFFE1BEE7)
                        MessageType.SHOP_ERROR -> Color(0xFFFFCDD2)

                    }

                    val borderColor = when (msg.type) {
                        MessageType.COIN_EXP -> Green.copy(alpha = 0.6f)
                        MessageType.LEVEL_UP -> Gold.copy(alpha = 0.7f)
                        MessageType.STAR_UP -> Color(0xFF9C27B0).copy(alpha = 0.6f)
                        MessageType.SHOP_ERROR -> RedAccent.copy(alpha = 0.7f)
                    }

                    val icon = when (msg.type) {
                        MessageType.COIN_EXP -> "💰"
                        MessageType.LEVEL_UP -> "⭐"
                        MessageType.STAR_UP -> "🌟"
                        MessageType.SHOP_ERROR -> "❌"
                    }

                    Card(
                        Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(icon, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                msg.text,
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}