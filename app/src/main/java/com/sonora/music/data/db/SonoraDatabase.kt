package com.sonora.music.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonora.music.core.model.AudioQuality
import com.sonora.music.core.model.SourceType
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter fun sourceToString(s: SourceType) = s.name
    @TypeConverter fun stringToSource(s: String) = SourceType.valueOf(s)
    @TypeConverter fun qualityToString(q: AudioQuality) = q.name
    @TypeConverter fun stringToQuality(s: String) = AudioQuality.valueOf(s)
}

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: SongEntity)

    @Query("SELECT * FROM songs WHERE liked = 1 ORDER BY likedAt DESC")
    fun likedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE downloaded = 1 ORDER BY addedAt DESC")
    fun downloadedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): SongEntity?

    @Query("UPDATE songs SET liked = :liked, likedAt = :ts WHERE id = :id")
    suspend fun setLiked(id: String, liked: Boolean, ts: Long?)

    @Query("SELECT EXISTS(SELECT 1 FROM songs WHERE id = :id AND liked = 1)")
    fun isLiked(id: String): Flow<Boolean>

    @Query("UPDATE songs SET downloaded = :downloaded, localPath = :path WHERE id = :id")
    suspend fun setDownloaded(id: String, downloaded: Boolean, path: String?)

    @Query("SELECT EXISTS(SELECT 1 FROM songs WHERE id = :id AND downloaded = 1)")
    fun isDownloaded(id: String): Flow<Boolean>

    @Query("SELECT localPath FROM songs WHERE id = :id AND downloaded = 1 LIMIT 1")
    suspend fun localPath(id: String): String?
}

@Dao
interface HistoryDao {
    @Insert
    suspend fun record(event: PlayEventEntity)

    /** Most-recently-played, one row per song. */
    @Query(
        "SELECT * FROM play_events WHERE id IN " +
            "(SELECT MAX(id) FROM play_events GROUP BY songId) ORDER BY playedAt DESC LIMIT 100"
    )
    fun recentlyPlayed(): Flow<List<PlayEventEntity>>

    /** Top songs by play count. */
    @Query(
        "SELECT * FROM play_events WHERE id IN " +
            "(SELECT MAX(id) FROM play_events GROUP BY songId) " +
            "ORDER BY (SELECT COUNT(*) FROM play_events e WHERE e.songId = play_events.songId) DESC LIMIT 50"
    )
    fun topSongs(): Flow<List<PlayEventEntity>>

    @Query("SELECT artistName, COUNT(*) AS plays FROM play_events GROUP BY artistName ORDER BY plays DESC LIMIT 50")
    fun topArtists(): Flow<List<ArtistPlays>>

    @Query("SELECT COUNT(*) FROM play_events")
    fun totalPlays(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM play_events")
    fun totalListenedMs(): Flow<Long>

    @Query("DELETE FROM play_events")
    suspend fun clear()
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun create(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun playlists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSong(ref: PlaylistSongCrossRef)

    @Query(
        "SELECT s.* FROM songs s JOIN playlist_songs ps ON s.id = ps.songId " +
            "WHERE ps.playlistId = :playlistId ORDER BY ps.position"
    )
    fun songsIn(playlistId: Long): Flow<List<SongEntity>>
}

@Database(
    entities = [SongEntity::class, PlayEventEntity::class, PlaylistEntity::class, PlaylistSongCrossRef::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class SonoraDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        fun build(context: Context): SonoraDatabase =
            Room.databaseBuilder(context, SonoraDatabase::class.java, "sonora.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
