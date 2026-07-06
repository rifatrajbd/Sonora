package com.sonora.music.ui.screens.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.sonora.music.data.settings.SettingsStore
import com.sonora.music.data.source.JioSaavnSource
import com.sonora.music.data.source.SpotifySource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistPick(val name: String, val imageUrl: String?)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val store: SettingsStore,
    private val spotify: SpotifySource,
    private val jioSaavn: JioSaavnSource,
) : ViewModel() {

    private val _artists = MutableStateFlow<List<ArtistPick>>(emptyList())
    val artists: StateFlow<List<ArtistPick>> = _artists.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            // Prefer Spotify (real artist photos); fall back to JioSaavn covers if unavailable.
            val fromSpotify = runCatching { spotify.popularArtists() }.getOrDefault(emptyList())
                .map { ArtistPick(it.first, it.second) }
            val picks = fromSpotify.ifEmpty { jioSaavnFallback() }
            _artists.value = picks
            _loading.value = false
        }
    }

    private suspend fun jioSaavnFallback(): List<ArtistPick> {
        val seeds = listOf("arijit singh", "the weeknd", "taylor swift", "diljit dosanjh", "ap dhillon",
            "atif aslam", "dua lipa", "badshah", "ed sheeran", "shreya ghoshal", "eminem", "coldplay",
            "honey singh", "neha kakkar", "sidhu moose wala", "billie eilish", "drake", "ariana grande")
        val out = LinkedHashMap<String, String?>()
        seeds.forEach { s ->
            // Real artist portrait from the artists endpoint; fall back to a song cover.
            val pick = runCatching { jioSaavn.searchArtistPick(s) }.getOrNull()
                ?: runCatching { jioSaavn.search(s).tracks.firstOrNull()?.let { it.artistName to it.thumbnailUrl } }.getOrNull()
            if (pick != null && !out.containsKey(pick.first)) out[pick.first] = pick.second
        }
        return out.entries.map { ArtistPick(it.key, it.value) }
    }

    fun finish(selected: Set<String>) = store.completeOnboarding(selected)
}

@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    val artists by vm.artists.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    val chosen = selected.filterValues { it }.keys

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp)) {
        Text("Choose artists you like", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp))
        Text(
            "We'll tailor your home around them.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(artists, key = { it.name }) { artist ->
                        val isSel = selected[artist.name] == true
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selected[artist.name] = !isSel },
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AsyncImage(
                                    model = artist.imageUrl,
                                    contentDescription = artist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .then(if (isSel) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                                )
                                if (isSel) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                            }
                            Text(artist.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }

        Button(
            onClick = { vm.finish(chosen); onDone() },
            enabled = chosen.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) { Text(if (chosen.isEmpty()) "Pick at least one" else "Continue (${chosen.size})") }
        TextButton(onClick = { vm.finish(emptySet()); onDone() }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("Skip for now") }
    }
}
