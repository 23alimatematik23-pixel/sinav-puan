package com.sinavpuan.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM classes ORDER BY grade, section")
    fun classes(): Flow<List<SchoolClass>>

    @Query("SELECT * FROM classes WHERE id = :id")
    fun classFlow(id: Long): Flow<SchoolClass?>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getClass(id: Long): SchoolClass?

    @Insert
    suspend fun insertClass(c: SchoolClass): Long

    @Update
    suspend fun updateClass(c: SchoolClass)

    @Delete
    suspend fun deleteClass(c: SchoolClass)

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY CAST(number AS INTEGER), name")
    fun students(classId: Long): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY CAST(number AS INTEGER), name")
    suspend fun studentsList(classId: Long): List<Student>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudent(id: Long): Student?

    @Insert
    suspend fun insertStudent(s: Student): Long

    @Insert
    suspend fun insertStudents(s: List<Student>)

    @Update
    suspend fun updateStudent(s: Student)

    @Delete
    suspend fun deleteStudent(s: Student)

    @Query("SELECT r.* FROM results r JOIN students s ON s.id = r.studentId WHERE s.classId = :classId")
    fun results(classId: Long): Flow<List<ExamResult>>

    @Query("SELECT r.* FROM results r JOIN students s ON s.id = r.studentId WHERE s.classId = :classId")
    suspend fun resultsList(classId: Long): List<ExamResult>

    @Query("SELECT * FROM results WHERE studentId = :studentId AND examNo = :examNo")
    suspend fun getResult(studentId: Long, examNo: Int): ExamResult?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResult(r: ExamResult)
}

@Database(entities = [SchoolClass::class, Student::class, ExamResult::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        fun build(context: Context): AppDb =
            Room.databaseBuilder(context, AppDb::class.java, "sinavpuan.db").build()
    }
}
