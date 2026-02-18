package com.k3mobile.testk3.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_table",
    foreignKeys = [ForeignKey(
        entity = TextEntity::class,
        parentColumns = ["idText"],
        childColumns = ["textId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val idSession: Long = 0,
    val textId: Long,
    val timeStamp: Long = System.currentTimeMillis(),
    val duration: Long,
    val wpm: Double,
    val accuracy: Double
)