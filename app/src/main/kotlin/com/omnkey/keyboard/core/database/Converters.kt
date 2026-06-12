package com.omnkey.keyboard.core.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.omnkey.keyboard.core.model.MacroAction

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromMacroActionList(value: String?): List<MacroAction> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<MacroAction>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toMacroActionList(list: List<MacroAction>): String {
        return gson.toJson(list)
    }
}
