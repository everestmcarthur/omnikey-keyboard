package com.omnkey.keyboard.core.engine

import android.content.Context
import android.graphics.PointF
import com.omnkey.keyboard.service.SwipePath
import kotlin.math.atan2
import kotlin.math.hypot

class SwipeEngine(private val context: Context) {

    private val keyPositions = mutableMapOf<String, PointF>()

    // QWERTY layout key positions (normalized 0-1)
    init {
        setupQwertyLayout()
    }

    private fun setupQwertyLayout() {
        // Row 1
        val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        row1.forEachIndexed { index, key ->
            keyPositions[key] = PointF(index / 10f, 0.25f)
        }

        // Row 2
        val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        row2.forEachIndexed { index, key ->
            keyPositions[key] = PointF((index + 0.5f) / 10f, 0.5f)
        }

        // Row 3
        val row3 = listOf("z", "x", "c", "v", "b", "n", "m")
        row3.forEachIndexed { index, key ->
            keyPositions[key] = PointF((index + 1.5f) / 10f, 0.75f)
        }
    }

    suspend fun recognizeGesture(path: SwipePath): String? {
        if (path.points.size < 3) return null

        // Convert path to key sequence
        val keySequence = pathToKeys(path)
        if (keySequence.isEmpty()) return null

        // Find best matching word
        return findBestMatch(keySequence)
    }

    private fun pathToKeys(path: SwipePath): List<String> {
        val keys = mutableListOf<String>()
        var lastKey: String? = null

        for (point in path.points) {
            val nearestKey = findNearestKey(point) ?: continue

            // Only add if different from last key (avoid duplicates)
            if (nearestKey != lastKey) {
                keys.add(nearestKey)
                lastKey = nearestKey
            }
        }

        return keys
    }

    private fun findNearestKey(point: PointF): String? {
        var nearestKey: String? = null
        var minDistance = Float.MAX_VALUE

        for ((key, position) in keyPositions) {
            val distance = hypot(point.x - position.x, point.y - position.y)
            if (distance < minDistance) {
                minDistance = distance
                nearestKey = key
            }
        }

        return nearestKey
    }

    private fun findBestMatch(keySequence: List<String>): String {
        // Simple algorithm: just return the key sequence as word
        // In production, use a dictionary lookup with fuzzy matching
        return keySequence.joinToString("")
    }

    fun updateKeyPositions(keys: Map<String, PointF>) {
        keyPositions.putAll(keys)
    }
}
