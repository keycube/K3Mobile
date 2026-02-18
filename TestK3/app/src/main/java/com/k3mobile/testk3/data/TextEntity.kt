package com.k3mobile.testk3.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "texts")
data class TextEntity(
    @PrimaryKey(autoGenerate = true) val idText: Long = 0,
    val title: String,
    val content: String,
    val language: String,   // ex: "fr"
    val category: String,   // "phrases", "histoires", "textes personnalisées"
    val difficulty: Int     // de 1 à 5
)