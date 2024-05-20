package com.example.newsapp.db

import androidx.room.TypeConverter
import com.example.newsapp.models.Source

class Converters {
//    converting Object to String which help database to store value
//    because database can store only string not object
    @TypeConverter
    fun fromSourse(source: Source): String?{
        return source.name
    }

    @TypeConverter
    fun toSource(name: String): Source{
        return Source(name, name)
    }

}