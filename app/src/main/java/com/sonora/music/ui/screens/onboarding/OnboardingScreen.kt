package com.sonora.music.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sonora.music.data.settings.SettingsStore
import com.sonora.music.data.source.JioSaavnSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val store: SettingsStore,
    private val jioSaavn: JioSaavnSource,
) : ViewModel() {

    /** Instant, network-free seed list — the picker opens with zero loading time. */
    val seedArtists: List<String> = listOf(
        "Arijit Singh", "The Weeknd", "Taylor Swift", "Atif Aslam", "Ed Sheeran", "Dua Lipa",
        "Shreya Ghoshal", "Coldplay", "Billie Eilish", "Eminem", "Drake", "Ariana Grande",
        "Diljit Dosanjh", "AP Dhillon", "Sidhu Moose Wala", "Badshah", "Neha Kakkar",
        "Honey Singh", "Karan Aujla", "Pritam", "A.R. Rahman", "Tanveer Evan", "Habib Wahid",
        "Imagine Dragons", "Adele", "Bruno Mars", "Post Malone", "Justin Bieber", "BTS", "Rihanna",
    )

    private val _searchResults = MutableStateFlow<List<String>>(emptyList())
    val searchResults: StateFlow<List<String>> = _searchResults.asStateFlow()

    private var searchJob: Job? = null

    @OptIn(FlowPreview::class)
    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(350) // debounce typing
            _searchResults.value =
                runCatching { jioSaavn.searchArtistNames(query) }.getOrDefault(emptyList())
        }
    }

    fun finish(selected: Set<String>) = store.completeOnboarding(selected)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    // Selected names; search picks not in the seed list are appended so they stay visible.
    val selected = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
    val extras = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val chosen = selected.filterValues { it }.keys

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp)) {
        Text(
            "Choose artists you like",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            "We'll tailor your home around them.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it; vm.search(it) },
            singleLine = true,
            placeholder = { Text("Search any artist…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (searchResults.isNotEmpty()) {
                Text(
                    "Search results",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    searchResults.forEach { name ->
                        val isSel = selected[name] == true
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selected[name] = !isSel
                                if (!isSel && name !in vm.seedArtists && name !in extras) extras += name
                            },
                            label = { Text(name) },
                            leadingIcon = {
                                Icon(
                                    if (isSel) Icons.Rounded.Check else Icons.Rounded.Add,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }
                Text(
                    "Popular",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (extras + vm.seedArtists).forEach { name ->
                    val isSel = selected[name] == true
                    FilterChip(
                        selected = isSel,
                        onClick = { selected[name] = !isSel },
                        label = { Text(name) },
                        leadingIcon = if (isSel) {
                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                        } else null,
                    )
                }
            }
        }

        Button(
            onClick = { vm.finish(chosen); onDone() },
            enabled = chosen.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) { Text(if (chosen.isEmpty()) "Pick at least one" else "Continue (${chosen.size})") }
        TextButton(
            onClick = { vm.finish(emptySet()); onDone() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) { Text("Skip for now") }
    }
}
