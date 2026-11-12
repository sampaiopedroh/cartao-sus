package br.edu.cartaosus.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

// Documentos anexados: só o caminho (URI) fica no aparelho; nada disso vai para a API.
@Entity(tableName = "documento_local")
data class DocumentoLocal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idCidadao: Long,
    val tipo: String,
    val titulo: String,
    val uri: String,
    val adicionadoEm: Long,
)

@Dao
interface DocumentoDao {
    @Insert
    suspend fun inserir(documento: DocumentoLocal)

    @Update
    suspend fun atualizar(documento: DocumentoLocal)

    @Query("SELECT * FROM documento_local WHERE idCidadao = :idCidadao ORDER BY adicionadoEm DESC")
    fun observar(idCidadao: Long): Flow<List<DocumentoLocal>>

    @Delete
    suspend fun remover(documento: DocumentoLocal)
}

@Database(entities = [MedicaoPendente::class, DocumentoLocal::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicoes(): MedicaoDao
    abstract fun documentos(): DocumentoDao

    companion object {
        fun criar(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "cartao_sus.db")
                .addMigrations(MIGRACAO_1_2)
                .build()

        // Mantém a fila de medições pendentes e só acrescenta a tabela de documentos.
        val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `documento_local` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`idCidadao` INTEGER NOT NULL, `tipo` TEXT NOT NULL, `titulo` TEXT NOT NULL, " +
                        "`uri` TEXT NOT NULL, `adicionadoEm` INTEGER NOT NULL)",
                )
            }
        }
    }
}
