package com.omnkey.keyboard.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.omnkey.keyboard.core.model.*

@Database(
    entities = [
        KeyboardTheme::class,
        TextReplacement::class,
        LearnedWord::class,
        WordPrediction::class,
        ClipboardItem::class,
        EmojiFrequency::class,
        Template::class,
        GestureAction::class,
        MacroDefinition::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class OmniKeyDatabase : RoomDatabase() {

    abstract fun themeDao(): ThemeDao
    abstract fun textReplacementDao(): TextReplacementDao
    abstract fun learnedWordDao(): LearnedWordDao
    abstract fun wordPredictionDao(): WordPredictionDao
    abstract fun clipboardDao(): ClipboardDao
    abstract fun emojiDao(): EmojiDao
    abstract fun templateDao(): TemplateDao
    abstract fun gestureDao(): GestureDao
    abstract fun macroDao(): MacroDao

    companion object {
        @Volatile
        private var INSTANCE: OmniKeyDatabase? = null

        fun getDatabase(context: Context): OmniKeyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OmniKeyDatabase::class.java,
                    "omnikey_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
