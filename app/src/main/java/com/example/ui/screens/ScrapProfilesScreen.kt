package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.scrapprofiles.*
import com.example.ui.components.FloatingBottomBar
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrapProfilesScreen(viewModel: MainViewModel, siteId: String? = null, onBack: () -> Unit = { viewModel.navigateBack() }) {
    val context = LocalContext.current
    val store = remember { ScrapProfileStore(context) }
    val scope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf(store.profiles()) }
    var selectedSite by remember(siteId) { mutableStateOf(siteId) }
    var showAdd by remember { mutableStateOf(false) }
    var urls by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var customImage by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val sites = ScrapSite.supported

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedSite == null) "Scrap Profiles" else sites.firstOrNull { it.id == selectedSite }?.name ?: "Profiles", color = CyanPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { if (selectedSite != null) IconButton(onClick = { onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CyanPrimary) } },
                actions = { IconButton(onClick = { profiles = store.profiles(); message = "Profile list refreshed" }) { Icon(Icons.Default.Refresh, "Refresh", tint = CyanPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = { FloatingBottomBar(currentScreen = Screen.ScrapProfiles(selectedSite), onNavigate = { viewModel.setScreen(it) }) },
        containerColor = DarkBackground
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(if (selectedSite == null) "SUPPORTED SITES" else "SAVED CREATOR PROFILES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Text(if (selectedSite == null) "Choose a platform" else "${profiles.count { it.siteId == selectedSite }} saved profiles", color = TextPrimary, fontSize = 13.sp)
                }
                Button(onClick = { showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            if (selectedSite == null) {
                LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(sites, key = { it.id }) { site ->
                        val count = profiles.count { it.siteId == site.id }
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DarkSurfaceVariant).border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp)).clickable { viewModel.setScreen(Screen.ScrapProfiles(site.id)) }.padding(16.dp)) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0x3300E5FF)), contentAlignment = Alignment.Center) { Text(site.name.take(1), color = CyanPrimary, fontSize = 23.sp, fontWeight = FontWeight.Black) }
                            Spacer(Modifier.height(14.dp)); Text(site.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$count profiles", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                val siteProfiles = profiles.filter { it.siteId == selectedSite }
                if (siteProfiles.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("No saved profiles", color = TextPrimary, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text("Add one or more public profile URLs.", color = TextSecondary, fontSize = 12.sp) } }
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(siteProfiles, key = { it.id }) { profile ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DarkSurfaceVariant).border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))) {
                                AsyncImage(model = profile.imageUrl.ifBlank { null }, contentDescription = profile.name, modifier = Modifier.fillMaxWidth().height(110.dp).background(Color(0xFF10151A)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                Column(Modifier.padding(11.dp)) {
                                    Text(profile.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${store.media(profile.id).size} media cached", color = TextSecondary, fontSize = 11.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { viewModel.setScreen(Screen.ScrapProfileMedia(profile.id)) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)), contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp)) { Text("Open profile", color = CyanPrimary, fontSize = 12.sp) }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        TextButton(onClick = { customName = profile.name; customImage = profile.imageUrl; urls = profile.id; showAdd = true }) { Text("Edit", color = CyanPrimary, fontSize = 11.sp) }
                                        IconButton(onClick = { store.remove(profile); profiles = store.profiles() }) { Icon(Icons.Default.DeleteOutline, "Remove profile", tint = Color(0xFFFF6B6B)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            message?.let { Text(it, color = CyanPrimary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }
        }
    }
    if (showAdd) {
        val isEditing = urls.isNotBlank() && profiles.any { it.id == urls }
        AlertDialog(
            onDismissRequest = { showAdd = false; urls = ""; customName = ""; customImage = "" },
            title = { Text(if (isEditing) "Edit profile card" else "Add scrap profiles", color = CyanPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isEditing) OutlinedTextField(value = urls, onValueChange = { urls = it }, label = { Text("Profile URL(s)") }, placeholder = { Text("Paste one URL per line") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = customName, onValueChange = { customName = it }, label = { Text("Display name (optional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = customImage, onValueChange = { customImage = it }, label = { Text("Custom profile image URL (optional)") }, modifier = Modifier.fillMaxWidth())
                    Text("Only publicly accessible metadata is inspected. Sites may restrict automated extraction.", color = TextSecondary, fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    if (isEditing) {
                        profiles.firstOrNull { it.id == urls }?.let { store.update(it.copy(name = customName.ifBlank { it.name }, imageUrl = customImage)) }
                        message = "Profile updated"
                    } else {
                        var added = 0; var duplicates = 0; var invalid = 0
                        urls.lines().map { it.trim().trim(',', ';', ' ') }.filter { it.isNotBlank() }.forEach { url ->
                            val p = store.add(url, customName.takeIf { urls.lines().size == 1 }, customImage)
                            if (p != null) added++ else if (ScrapSite.fromUrl(url) != null) duplicates++ else invalid++
                        }
                        message = "Added $added · duplicates $duplicates · invalid/unsupported $invalid"
                    }
                    profiles = store.profiles(); busy = false; showAdd = false; urls = ""; customName = ""; customImage = ""
                }
            }) { Text(if (busy) "Saving…" else "Save", color = CyanPrimary) } },
            dismissButton = { TextButton(onClick = { showAdd = false; urls = ""; customName = ""; customImage = "" }) { Text("Cancel", color = TextSecondary) } },
            containerColor = DarkSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrapProfileMediaScreen(profileId: String, viewModel: MainViewModel, onBack: () -> Unit = { viewModel.navigateBack() }) {
    val context = LocalContext.current
    val store = remember { ScrapProfileStore(context) }
    val scraper = remember { ScrapProfileScraper() }
    var profile by remember(profileId) { mutableStateOf(store.profiles().firstOrNull { it.id == profileId }) }
    var media by remember(profileId) { mutableStateOf(store.media(profileId)) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun refresh() {
        val p = profile ?: return
        scope.launch {
            busy = true; error = null
            val result = scraper.scrape(p)
            result.onSuccess { items -> media = items; store.saveMedia(p.id, items) }.onFailure { error = it.message ?: "Unable to extract public media" }
            busy = false
        }
    }
    LaunchedEffect(profileId) { if (media.isEmpty()) refresh() }
    Scaffold(topBar = {
        TopAppBar(title = { Text(profile?.name ?: "Profile media", color = CyanPrimary, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CyanPrimary) } }, actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "Refresh", tint = CyanPrimary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface))
    }, containerColor = DarkBackground) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = CyanPrimary)
            error?.let { Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp)) }
            Text("${media.size} media items", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            if (media.isEmpty() && !busy) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No public playable media found. Try refresh, or open the source profile in a browser.", color = TextSecondary, fontSize = 13.sp) }
            else LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(media, key = { it.id }) { item ->
                    Column(Modifier.clip(RoundedCornerShape(14.dp)).background(DarkSurfaceVariant).clickable {
                        val synthetic = media.map { m -> com.example.database.MediaEntity(uriString = m.mediaUrl, physicalName = m.title, displayName = m.title, parentUriString = profileId, size = 0L, dateModified = 0L, mimeType = m.mimeType, mediaType = com.example.model.MediaType.VIDEO, isHidden = false, isObfuscated = false, thumbnailPath = m.thumbnailUrl.takeIf { it.isNotBlank() }) }
                        viewModel.openMediaViewer(synthetic, media.indexOf(item))
                    }) {
                        AsyncImage(model = item.thumbnailUrl.ifBlank { null }, contentDescription = item.title, modifier = Modifier.fillMaxWidth().height(115.dp).background(Color(0xFF10151A)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        Text(item.title, color = TextPrimary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(9.dp))
                    }
                }
            }
        }
    }
}
