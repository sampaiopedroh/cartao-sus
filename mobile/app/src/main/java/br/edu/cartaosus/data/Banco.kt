package br.edu.cartaosus.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

// Fila offline de medições (SDD 7.6): o item só sai após o servidor confirmar (201).
@Entity(tableName = "medicao_pendente")
data class MedicaoPendente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,
    val valor: Double,
    val dataHora: String,
)

@Dao
interface MedicaoDao {
    @Insert
    suspend fun inserir(medicao: MedicaoPendente)

    @Query("SELECT * FROM medicao_pendente ORDER BY id")
    suspend fun todas(): List<MedicaoPendente>

    @Query("SELECT * FROM medicao_pendente ORDER BY id")
    fun observar(): Flow<List<MedicaoPendente>>

    @Delete
    suspend fun remover(medicao: MedicaoPendente)
}

@Database(entities = [MedicaoPendente::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicoes(): MedicaoDao

    companion object {
        fun criar(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "cartao_sus.db").build()
    }
}
