package com.phahoang.aquafarm.service

import com.phahoang.aquafarm.data.model.*

class GameEngine {

    //Kiểm tra cá đói
    fun isHungry(fish: FishInstance, species: FishSpecies): Boolean {
        val elapsed = System.currentTimeMillis() - fish.lastFed
        return elapsed >= species.hungerMinutes * 60_000L
    }

    //thời gian còn lại trước khi đói
    fun timeUntilHungry(fish: FishInstance, species: FishSpecies): Long {
        val elapsed = System.currentTimeMillis() - fish.lastFed
        val total = species.hungerMinutes * 60_000L
        return (total - elapsed).coerceAtLeast(0)
    }

    //Cho ăn
    fun feed(fish: FishInstance, species: FishSpecies): FeedResult {
        if (!isHungry(fish, species)) return FeedResult.NotHungry

        //Bonus: mỗi sao +15%
        val starBonus = 1f + fish.stars * 0.15f
        val coins = (species.coinPerFeed * starBonus).toInt()
        val exp = (species.expPerFeed * starBonus).toInt()

        val newCount = fish.feedCount + 1
        val grewUp = newCount >= species.feedsToGrow && fish.stars < species.maxStars

        return FeedResult.Ok(
            coins = coins,
            exp = exp,
            newFeedCount = if (grewUp) 0 else newCount,
            newStars = if (grewUp) fish.stars + 1 else fish.stars,
            grewUp = grewUp
        )
    }

    //Level up
    fun addExp(player: Player, amount: Int): Pair<Player, Int> {
        var exp = player.currentExp + amount
        var lvl = player.level
        var gained = 0
        while (exp >= player.expToNextLevel()) {
            exp -= player.expToNextLevel()
            lvl++
            gained++
        }
        return player.copy(currentExp = exp, level = lvl) to gained
    }

    sealed class FeedResult {
        data class Ok(val coins: Int, val exp: Int, val newFeedCount: Int, val newStars: Int, val grewUp: Boolean) : FeedResult()
        data object NotHungry : FeedResult()
    }
}
