package com.omnkey.keyboard.core.engine

import android.content.ClipData
import android.content.ClipboardManager as SystemClipboardManager
import android.content.Context
import com.omnkey.keyboard.core.database.OmniKeyDatabase
import com.omnkey.keyboard.core.model.ClipboardItem
import com.omnkey.keyboard.core.model.ClipboardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ClipboardManager(
    private val context: Context,
    private val database: OmniKeyDatabase
) {
    private val clipboardDao = database.clipboardDao()
    private val systemClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as SystemClipboardManager

    init {
        // Monitor system clipboard
        systemClipboard.addPrimaryClipChangedListener {
            onClipboardChanged()
        }
    }

    private fun onClipboardChanged() {
        val clip = systemClipboard.primaryClip ?: return
        if (clip.itemCount == 0) return

        val item = clip.getItemAt(0)
        val text = item.text?.toString() ?: return

        if (text.isNotBlank()) {
            kotlinx.coroutines.GlobalScope.launch {
                saveClipboardItem(text)
            }
        }
    }

    suspend fun saveClipboardItem(content: String) = withContext(Dispatchers.IO) {
        val type = detectClipboardType(content)

        val item = ClipboardItem(
            content = content,
            contentType = type,
            copiedAt = System.currentTimeMillis()
        )

        clipboardDao.insertClipboardItem(item)

        // Clean up old items
        clipboardDao.deleteOldItems()
        clipboardDao.deleteExpiredItems()
    }

    private fun detectClipboardType(content: String): ClipboardType {
        return when {
            content.startsWith("http://") || content.startsWith("https://") -> ClipboardType.URL
            content.contains("@") && content.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) -> ClipboardType.EMAIL
            content.matches(Regex("^[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}$")) -> ClipboardType.PHONE
            else -> ClipboardType.TEXT
        }
    }

    fun getAllItems(): Flow<List<ClipboardItem>> {
        return clipboardDao.getAllClipboardItems()
    }

    fun getPinnedItems(): Flow<List<ClipboardItem>> {
        return clipboardDao.getPinnedItems()
    }

    suspend fun pinItem(item: ClipboardItem) {
        clipboardDao.updateClipboardItem(item.copy(isPinned = true))
    }

    suspend fun unpinItem(item: ClipboardItem) {
        clipboardDao.updateClipboardItem(item.copy(isPinned = false))
    }

    suspend fun deleteItem(item: ClipboardItem) {
        clipboardDao.deleteClipboardItem(item)
    }

    suspend fun pasteItem(item: ClipboardItem) {
        // Copy to system clipboard
        val clip = ClipData.newPlainText("OmniKey", item.content)
        systemClipboard.setPrimaryClip(clip)

        // Increment use count
        clipboardDao.incrementUseCount(item.id)
    }

    suspend fun clearHistory() {
        val items = clipboardDao.getAllClipboardItems().first()
        items.filter { !it.isPinned }.forEach { clipboardDao.deleteClipboardItem(it) }
    }

    suspend fun getClipboardSuggestions(contextBefore: String): List<ClipboardItem> {
        val allItems = clipboardDao.getAllClipboardItems().first()

        // Simple relevance scoring
        return allItems
            .filter { item ->
                // Suggest URLs in URL fields, emails in email fields, etc.
                item.contentType == detectClipboardType(contextBefore) ||
                item.content.contains(contextBefore, ignoreCase = true)
            }
            .sortedByDescending { it.useCount }
            .take(3)
    }

    fun showClipboardPanel() {
        // This would show a UI panel with clipboard history
        // Implementation depends on keyboard view
    }
}

suspend fun <T> Flow<T>.first(): T {
    var result: T? = null
    this.collect {
        result = it
        return@collect
    }
    return result!!
}
