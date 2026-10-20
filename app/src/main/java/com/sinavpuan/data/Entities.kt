package com.sinavpuan.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class SchoolClass(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val grade: String,
    val section: String,
    val size: Int,
    val q1Count: Int = 10,
    val q2Count: Int = 10,
)

fun SchoolClass.title(): String = "$grade-$section"

fun SchoolClass.questionCount(examNo: Int): Int = if (examNo == 1) q1Count else q2Count

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = SchoolClass::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("classId")],
)
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val number: String,
    val name: String,
    val parentPhone: String = "",
)

@Entity(
    tableName = "results",
    primaryKeys = ["studentId", "examNo"],
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("studentId")],
)
data class ExamResult(
    val studentId: Long,
    val examNo: Int,
    val scores: String,
)

fun ExamResult.scoreList(): List<Int> =
    if (scores.isBlank()) emptyList() else scores.split(",").map { it.trim().toIntOrNull() ?: 0 }

fun ExamResult.total(): Int = scoreList().sum()
