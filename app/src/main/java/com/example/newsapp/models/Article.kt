package com.example.newsapp.models
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val author: String = "Unknown",
    val content: String = "No content",
    val description: String = "No description",
    val publishedAt: String = "Unknown date",
    val source: Source? = Source(),
    val title: String = "No title",
    val url: String = "",
    val urlToImage: String = ""
) : Serializable