package pl.put.airbeats.utils.game

import android.util.Log

const val PERFECT_MARGIN = 0.1f // relative to tile center
const val GREAT_MARGIN = 0.5f // relative to tile center
const val PERFECT_POINTS = 100f
const val GREAT_POINTS = 50f
const val GOOD_POINTS = 10f
const val PERFECT_COMBO_MULT = 1.1f

class LevelStatistics() {
    var points = 0f
    var maxCombo = 0
    var combo = 0
    var comboMultiplier = 1f
    var perfect = 0
    var great = 0
    var good = 0
    var missed = 0

    fun addTile() {
        missed++
    }

    fun score(relativeOffset: Float) {
        if( relativeOffset < PERFECT_MARGIN ) {
            Log.d("Game event", "Perfect hit")
            perfect++
            missed--
            points += PERFECT_POINTS * comboMultiplier
            combo++
            maxCombo = if (combo > maxCombo) combo else maxCombo
            comboMultiplier *= PERFECT_COMBO_MULT
        } else {
            combo = 0
            comboMultiplier = 1f
            if( relativeOffset < GREAT_MARGIN ) {
                Log.d("Game event", "Great hit")
                great++
                missed--
                points += GREAT_POINTS
            } else {
                Log.d("Game event", "Good hit")
                good++
                missed--
                points += GOOD_POINTS
            }
        }
    }
}