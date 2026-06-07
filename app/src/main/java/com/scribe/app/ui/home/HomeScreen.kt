package com.scribe.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.scribe.app.R
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirlineSeatFlat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.scribe.app.data.auth.Feature
import com.scribe.app.data.auth.Permissions
import com.scribe.app.data.auth.UserRole
import com.scribe.app.data.repository.AuthState
import com.scribe.app.data.i18n.LocaleStore
import com.scribe.app.data.repository.ContentRepository
import com.scribe.app.data.repository.SessionRepository
import com.scribe.app.ui.brancardage.BrancardageSection
import com.scribe.app.ui.capacite.CapaciteSection
import com.scribe.app.ui.cellule.CelluleSection
import com.scribe.app.ui.chat.ChatSection
import com.scribe.app.ui.communiques.CommuniquesSection
import com.scribe.app.ui.dashboard.DashboardSection
import com.scribe.app.ui.incidents.IncidentsSection
import com.scribe.app.ui.kanban.KanbanSection
import com.scribe.app.ui.messages.MessagesSection
import com.scribe.app.ui.soins.SoinsSection
import com.scribe.app.ui.transferts.TransfertsSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeSection(val title: String, val icon: ImageVector, val feature: Feature, val navKey: String) {
    DASHBOARD("Tableau de bord", Icons.Filled.Dashboard, Feature.DASHBOARD, "nav.dashboard"),
    INCIDENTS("Incidents", Icons.Filled.Warning, Feature.INCIDENTS, "nav.veille"),
    KANBAN("Tâches (Kanban)", Icons.Filled.ViewKanban, Feature.KANBAN, "nav.kanban"),
    CELLULE("Cellule de crise", Icons.Filled.Gavel, Feature.CELLULE, "nav.cellule"),
    TRANSFERTS("Transferts", Icons.Filled.LocalShipping, Feature.TRANSFERTS, "nav.transferts"),
    BRANCARDAGE("Brancardage", Icons.Filled.AirlineSeatFlat, Feature.BRANCARDAGE, "nav.brancardage"),
    COMMUNIQUES("Communiqués", Icons.Filled.Campaign, Feature.COMMUNIQUES, "nav.communique"),
    SOINS("Soins", Icons.Filled.LocalHospital, Feature.SOINS, "nav.soins"),
    CAPACITE("Capacité", Icons.Filled.Hotel, Feature.CAPACITE, "nav.capacite"),
    MESSAGES("Messages", Icons.Filled.Email, Feature.MESSAGES, "nav.messagerie"),
    CHAT("Chat", Icons.AutoMirrored.Filled.Chat, Feature.CHAT, "nav.chat"),
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: ContentRepository,
    session: SessionRepository,
    locale: LocaleStore,
) : ViewModel() {
    val dict: StateFlow<Map<String, String>> = locale.dict
    private val _unread = MutableStateFlow(0)
    val unread: StateFlow<Int> = _unread.asStateFlow()

    val role: StateFlow<UserRole> = session.state
        .map { st -> UserRole.fromApi((st as? AuthState.LoggedIn)?.role) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserRole.UNKNOWN)

    init {
        viewModelScope.launch {
            while (true) {
                _unread.value = repo.unread()
                delay(30_000)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDetail: (Int) -> Unit,
    onCreateIncident: () -> Unit,
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    var section by remember { mutableStateOf(HomeSection.DASHBOARD) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val unread by homeViewModel.unread.collectAsStateWithLifecycle()
    val role by homeViewModel.role.collectAsStateWithLifecycle()
    val dict by homeViewModel.dict.collectAsStateWithLifecycle()
    fun tr(sec: HomeSection): String = dict[sec.navKey] ?: sec.title

    BackHandler(enabled = section != HomeSection.DASHBOARD) {
        section = HomeSection.DASHBOARD
    }

    // Repli si la section courante n'est plus autorisée (rôle chargé/changé).
    LaunchedEffect(role) {
        if (!Permissions.allows(section.feature, role)) section = HomeSection.DASHBOARD
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                androidx.compose.foundation.layout.Column(
                    Modifier.verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text(
                            "SCRIBE",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Image(
                            painter = painterResource(R.drawable.logo_scribe),
                            contentDescription = null,
                            modifier = Modifier.size(width = 42.dp, height = 36.dp),
                        )
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    HomeSection.entries.filter { Permissions.allows(it.feature, role) }.forEach { s ->
                        NavigationDrawerItem(
                            icon = { Icon(s.icon, contentDescription = null) },
                            label = { Text(tr(s)) },
                            badge = if (s == HomeSection.MESSAGES && unread > 0) {
                                { Badge { Text("$unread") } }
                            } else null,
                            selected = s == section,
                            onClick = {
                                section = s
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                        label = { Text("Se déconnecter") },
                        selected = false,
                        onClick = onLogout,
                        modifier = Modifier.padding(12.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(tr(section)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            },
            floatingActionButton = {
                if (section == HomeSection.INCIDENTS) {
                    FloatingActionButton(
                        onClick = onCreateIncident,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    ) { Icon(Icons.Filled.Add, contentDescription = "Déclarer un incident") }
                }
            },
        ) { padding ->
            val m = Modifier.padding(padding).fillMaxSize()
            when (section) {
                HomeSection.DASHBOARD -> DashboardSection(
                    m,
                    role = role,
                    onGoIncidents = { section = HomeSection.INCIDENTS },
                    onGoKanban = { section = HomeSection.KANBAN },
                    onGoTransferts = { section = HomeSection.TRANSFERTS },
                    onGoMessages = { section = HomeSection.MESSAGES },
                    onGoSoins = { section = HomeSection.SOINS },
                )
                HomeSection.INCIDENTS -> IncidentsSection(m, onOpenDetail = onOpenDetail)
                HomeSection.KANBAN -> KanbanSection(m)
                HomeSection.CELLULE -> CelluleSection(m)
                HomeSection.TRANSFERTS -> TransfertsSection(m)
                HomeSection.BRANCARDAGE -> BrancardageSection(m)
                HomeSection.COMMUNIQUES -> CommuniquesSection(m)
                HomeSection.SOINS -> SoinsSection(m)
                HomeSection.CAPACITE -> CapaciteSection(m)
                HomeSection.MESSAGES -> MessagesSection(m)
                HomeSection.CHAT -> ChatSection(m)
            }
        }
    }
}
