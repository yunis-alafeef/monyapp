package com.example.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.model.DebtDirection
import com.example.ui.screens.AddDebtScreen
import com.example.ui.screens.AddEditContactScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.BackupSyncScreen
import com.example.ui.screens.ContactDetailScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtDetailScreen
import com.example.ui.screens.DebtsHubScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.InstallmentsScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RecordPaymentScreen
import com.example.ui.screens.ReminderSettingsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmsSettingsScreen
import com.example.ui.screens.StatementScreen
import com.example.ui.screens.TransactionDetailScreen
import com.example.ui.screens.WhatsAppSettingsScreen
import com.example.ui.screens.WorkspaceMembersScreen
import com.example.ui.viewmodel.HisabatiViewModel

object HisabatiRoutes {
    const val DASHBOARD = "dashboard"
    const val DEBTS_HUB = "debts_hub?tab={tab}"
    const val CONTACTS = "contacts"
    const val MORE_HUB = "more_hub"

    const val ADD_CONTACT = "add_contact?contactId={contactId}"
    const val CONTACT_DETAIL = "contact_detail/{contactId}"
    const val ADD_DEBT = "add_debt?contactId={contactId}&dir={dir}"
    const val DEBT_DETAIL = "debt_detail/{debtId}"
    const val RECORD_PAYMENT = "record_payment/{debtId}"
    const val INSTALLMENTS = "installments/{debtId}"
    const val TRANSACTION_DETAIL = "transaction_detail/{paymentId}"

    const val NOTIFICATIONS = "notifications"
    const val REMINDER_SETTINGS = "reminder_settings"
    const val WHATSAPP_SETTINGS = "whatsapp_settings"
    const val SMS_SETTINGS = "sms_settings"
    const val REPORTS = "reports"
    const val STATEMENT = "statement?contactId={contactId}"
    const val AI_ASSISTANT = "ai_assistant"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val WORKSPACE_MEMBERS = "workspace_members"
    const val BACKUP_SYNC = "backup_sync"
    const val PROFILE = "profile"
    const val HELP_SUPPORT = "help_support"

    fun debtsHub(tab: Int = 0) = "debts_hub?tab=$tab"
    fun addContact(contactId: String? = null) = "add_contact?contactId=${Uri.encode(contactId ?: "")}"
    fun contactDetail(contactId: String) = "contact_detail/${Uri.encode(contactId)}"
    fun addDebt(contactId: String? = null, dir: DebtDirection = DebtDirection.RECEIVABLE) =
        "add_debt?contactId=${Uri.encode(contactId ?: "")}&dir=${dir.name}"
    fun debtDetail(debtId: String) = "debt_detail/${Uri.encode(debtId)}"
    fun recordPayment(debtId: String) = "record_payment/${Uri.encode(debtId)}"
    fun installments(debtId: String) = "installments/${Uri.encode(debtId)}"
    fun transactionDetail(paymentId: String) = "transaction_detail/${Uri.encode(paymentId)}"
    fun statement(contactId: String = "") = "statement?contactId=${Uri.encode(contactId)}"
}

private data class TopLevelDestination(
    val route: String,
    val baseRoute: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun HisabatiAppShell(
    viewModel: HisabatiViewModel,
    onSignOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val aiStreaming by viewModel.aiStreamingText.collectAsStateWithLifecycle()
    val aiBusy by viewModel.aiBusy.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.feedbackMessage) {
        val msg = state.feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val topDestinations = listOf(
        TopLevelDestination(HisabatiRoutes.DASHBOARD, HisabatiRoutes.DASHBOARD, "الرئيسية", Icons.Rounded.Home, "nav_dashboard"),
        TopLevelDestination(HisabatiRoutes.debtsHub(0), "debts_hub", "الديون", Icons.Rounded.AccountBalanceWallet, "nav_debts"),
        TopLevelDestination(HisabatiRoutes.CONTACTS, HisabatiRoutes.CONTACTS, "الأشخاص", Icons.Rounded.Groups, "nav_contacts"),
        TopLevelDestination(HisabatiRoutes.MORE_HUB, HisabatiRoutes.MORE_HUB, "المزيد", Icons.Rounded.Apps, "nav_more")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route.orEmpty()
    val showTopLevelNav = topDestinations.any { currentRoute.startsWith(it.baseRoute) }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                if (showTopLevelNav && !isWide) {
                    NavigationBar {
                        topDestinations.forEach { dest ->
                            val selected = currentRoute.startsWith(dest.baseRoute)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && isWide) {
                    NavigationRail {
                        topDestinations.forEach { dest ->
                            val selected = currentRoute.startsWith(dest.baseRoute)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = HisabatiRoutes.DASHBOARD
                    ) {
                        composable(HisabatiRoutes.DASHBOARD) {
                            DashboardScreen(
                                state = state,
                                userDisplayName = viewModel.userDisplayName,
                                onAddDebtClick = { navController.navigate(HisabatiRoutes.addDebt()) },
                                onQuickPaymentClick = { debtId -> navController.navigate(HisabatiRoutes.recordPayment(debtId)) },
                                onDebtClick = { debtId -> navController.navigate(HisabatiRoutes.debtDetail(debtId)) },
                                onPaymentClick = { paymentId -> navController.navigate(HisabatiRoutes.transactionDetail(paymentId)) },
                                onOpenOverdue = { navController.navigate(HisabatiRoutes.debtsHub(2)) },
                                onOpenNotifications = { navController.navigate(HisabatiRoutes.NOTIFICATIONS) },
                                onOpenSearch = { navController.navigate(HisabatiRoutes.SEARCH) },
                                onOpenAiAssistant = { navController.navigate(HisabatiRoutes.AI_ASSISTANT) },
                                onOpenContacts = { navigateTopLevel(HisabatiRoutes.CONTACTS) },
                                onSyncNow = { viewModel.syncNow() }
                            )
                        }

                        composable(
                            route = HisabatiRoutes.DEBTS_HUB,
                            arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
                        ) { entry ->
                            val tab = entry.arguments?.getInt("tab") ?: 0
                            DebtsHubScreen(
                                state = state,
                                initialTab = tab,
                                onAddDebt = { dir -> navController.navigate(HisabatiRoutes.addDebt(dir = dir)) },
                                onDebtClick = { debtId -> navController.navigate(HisabatiRoutes.debtDetail(debtId)) }
                            )
                        }

                        composable(HisabatiRoutes.CONTACTS) {
                            ContactsScreen(
                                state = state,
                                onAddContact = { navController.navigate(HisabatiRoutes.addContact()) },
                                onContactClick = { cid -> navController.navigate(HisabatiRoutes.contactDetail(cid)) }
                            )
                        }

                        composable(HisabatiRoutes.MORE_HUB) {
                            MoreHubScreen(
                                onOpenReports = { navController.navigate(HisabatiRoutes.REPORTS) },
                                onOpenStatement = { navController.navigate(HisabatiRoutes.statement()) },
                                onOpenAi = { navController.navigate(HisabatiRoutes.AI_ASSISTANT) },
                                onOpenSearch = { navController.navigate(HisabatiRoutes.SEARCH) },
                                onOpenNotifications = { navController.navigate(HisabatiRoutes.NOTIFICATIONS) },
                                onOpenReminders = { navController.navigate(HisabatiRoutes.REMINDER_SETTINGS) },
                                onOpenWhatsApp = { navController.navigate(HisabatiRoutes.WHATSAPP_SETTINGS) },
                                onOpenSms = { navController.navigate(HisabatiRoutes.SMS_SETTINGS) },
                                onOpenWorkspace = { navController.navigate(HisabatiRoutes.WORKSPACE_MEMBERS) },
                                onOpenBackup = { navController.navigate(HisabatiRoutes.BACKUP_SYNC) },
                                onOpenSettings = { navController.navigate(HisabatiRoutes.SETTINGS) },
                                onOpenProfile = { navController.navigate(HisabatiRoutes.PROFILE) },
                                onOpenHelp = { navController.navigate(HisabatiRoutes.HELP_SUPPORT) }
                            )
                        }

                        composable(
                            route = HisabatiRoutes.ADD_CONTACT,
                            arguments = listOf(navArgument("contactId") { type = NavType.StringType; defaultValue = "" })
                        ) { entry ->
                            val cid = entry.arguments?.getString("contactId").orEmpty()
                            val existing = state.contacts.find { it.id == cid }
                            AddEditContactScreen(
                                existingContact = existing,
                                onBack = { navController.popBackStack() },
                                onSave = { name, phone, addr, notes, avatar ->
                                    viewModel.saveContact(existing?.id, name, phone, addr, notes, avatar) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable(
                            route = HisabatiRoutes.CONTACT_DETAIL,
                            arguments = listOf(navArgument("contactId") { type = NavType.StringType })
                        ) { entry ->
                            val cid = entry.arguments?.getString("contactId").orEmpty()
                            val contact = state.contacts.find { it.id == cid }
                            if (contact != null) {
                                ContactDetailScreen(
                                    contact = contact,
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onEditContact = { navController.navigate(HisabatiRoutes.addContact(contact.id)) },
                                    onAddDebtForContact = { navController.navigate(HisabatiRoutes.addDebt(contactId = contact.id)) },
                                    onDebtClick = { did -> navController.navigate(HisabatiRoutes.debtDetail(did)) },
                                    onOpenStatement = { navController.navigate(HisabatiRoutes.statement(contact.id)) },
                                    onPaymentClick = { pid -> navController.navigate(HisabatiRoutes.transactionDetail(pid)) }
                                )
                            }
                        }

                        composable(
                            route = HisabatiRoutes.ADD_DEBT,
                            arguments = listOf(
                                navArgument("contactId") { type = NavType.StringType; defaultValue = "" },
                                navArgument("dir") { type = NavType.StringType; defaultValue = DebtDirection.RECEIVABLE.name }
                            )
                        ) { entry ->
                            val cid = entry.arguments?.getString("contactId")?.takeIf { it.isNotBlank() }
                            val dirName = entry.arguments?.getString("dir") ?: DebtDirection.RECEIVABLE.name
                            val dir = runCatching { DebtDirection.valueOf(dirName) }.getOrDefault(DebtDirection.RECEIVABLE)
                            AddDebtScreen(
                                state = state,
                                preselectedContactId = cid,
                                initialDirection = dir,
                                onBack = { navController.popBackStack() },
                                onQuickAddContact = { navController.navigate(HisabatiRoutes.addContact()) },
                                onSaveDebt = { contactId, d, minor, cur, recAt, dueAt, reason, pm, att, notes, instCount ->
                                    viewModel.addDebt(contactId, d, minor, cur, recAt, dueAt, reason, pm, att, notes, instCount) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable(
                            route = HisabatiRoutes.DEBT_DETAIL,
                            arguments = listOf(navArgument("debtId") { type = NavType.StringType })
                        ) { entry ->
                            val did = entry.arguments?.getString("debtId").orEmpty()
                            val debt = state.debts.find { it.id == did }
                            if (debt != null) {
                                DebtDetailScreen(
                                    debt = debt,
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onRecordPayment = { navController.navigate(HisabatiRoutes.recordPayment(debt.id)) },
                                    onOpenInstallments = { navController.navigate(HisabatiRoutes.installments(debt.id)) },
                                    onPaymentClick = { pid -> navController.navigate(HisabatiRoutes.transactionDetail(pid)) },
                                    onUpdateDebtDetails = { due, reason, notes, editReason ->
                                        viewModel.updateDebtDetails(debt.id, due, reason, notes, editReason)
                                    },
                                    onSendCloudMessage = { ch, ph, body, type ->
                                        viewModel.sendBackendRelayMessage(ch, ph, body, type)
                                    }
                                )
                            }
                        }

                        composable(
                            route = HisabatiRoutes.RECORD_PAYMENT,
                            arguments = listOf(navArgument("debtId") { type = NavType.StringType })
                        ) { entry ->
                            val did = entry.arguments?.getString("debtId").orEmpty()
                            val debt = state.debts.find { it.id == did }
                            if (debt != null) {
                                RecordPaymentScreen(
                                    debt = debt,
                                    onBack = { navController.popBackStack() },
                                    onSubmitPayment = { amt, disc, method, paidAt, notes, settle, onSuccess ->
                                        viewModel.recordPayment(debt.id, amt, disc, method, paidAt, notes, settle, onSuccess)
                                    },
                                    onViewReceipt = { pid ->
                                        navController.popBackStack()
                                        navController.navigate(HisabatiRoutes.transactionDetail(pid))
                                    }
                                )
                            }
                        }

                        composable(
                            route = HisabatiRoutes.INSTALLMENTS,
                            arguments = listOf(navArgument("debtId") { type = NavType.StringType })
                        ) { entry ->
                            val did = entry.arguments?.getString("debtId").orEmpty()
                            val debt = state.debts.find { it.id == did }
                            if (debt != null) {
                                val list = state.installments.filter { it.debtId == did }
                                InstallmentsScreen(
                                    debt = debt,
                                    installments = list,
                                    canEditSchedule = state.activeMember?.canEditDebt ?: true,
                                    onBack = { navController.popBackStack() },
                                    onPayInstallment = { navController.navigate(HisabatiRoutes.recordPayment(debt.id)) },
                                    onReschedule = { instId, due, amt, reason ->
                                        viewModel.rescheduleInstallment(instId, due, amt, reason)
                                    }
                                )
                            }
                        }

                        composable(
                            route = HisabatiRoutes.TRANSACTION_DETAIL,
                            arguments = listOf(navArgument("paymentId") { type = NavType.StringType })
                        ) { entry ->
                            val pid = entry.arguments?.getString("paymentId").orEmpty()
                            val payment = state.payments.find { it.id == pid }
                            if (payment != null) {
                                val parentDebt = state.debts.find { it.id == payment.debtId }
                                TransactionDetailScreen(
                                    payment = payment,
                                    debt = parentDebt,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        composable(HisabatiRoutes.NOTIFICATIONS) {
                            NotificationsScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onMarkAllRead = { viewModel.markAllNotificationsRead() },
                                onOpenDebt = { did -> navController.navigate(HisabatiRoutes.debtDetail(did)) },
                                onOpenReminderSettings = { navController.navigate(HisabatiRoutes.REMINDER_SETTINGS) }
                            )
                        }

                        composable(HisabatiRoutes.REMINDER_SETTINGS) {
                            ReminderSettingsScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onSaveNotificationSettings = { en, h, m, hp ->
                                    viewModel.setNotificationSettings(en, h, m, hp)
                                },
                                onCreateReminder = { rem ->
                                    viewModel.saveReminder(rem.copy(userId = viewModel.currentUserId))
                                },
                                onDeleteReminder = { id -> viewModel.deleteReminder(id) }
                            )
                        }

                        composable(HisabatiRoutes.WHATSAPP_SETTINGS) {
                            WhatsAppSettingsScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onSaveRelayConfig = { wa, sms, url, phId, sid ->
                                    viewModel.setMessagingRelaySettings(wa, sms, url, phId, sid)
                                },
                                onSaveTemplate = { tpl -> viewModel.saveTemplate(tpl) },
                                onSendCloudTest = { ch, ph, body, type ->
                                    viewModel.sendBackendRelayMessage(ch, ph, body, type)
                                }
                            )
                        }

                        composable(HisabatiRoutes.SMS_SETTINGS) {
                            SmsSettingsScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onSaveRelayConfig = { wa, sms, url, phId, sid ->
                                    viewModel.setMessagingRelaySettings(wa, sms, url, phId, sid)
                                },
                                onSendCloudTest = { ch, ph, body, type ->
                                    viewModel.sendBackendRelayMessage(ch, ph, body, type)
                                }
                            )
                        }

                        composable(HisabatiRoutes.REPORTS) {
                            ReportsScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onOpenStatement = { cid -> navController.navigate(HisabatiRoutes.statement(cid)) }
                            )
                        }

                        composable(
                            route = HisabatiRoutes.STATEMENT,
                            arguments = listOf(navArgument("contactId") { type = NavType.StringType; defaultValue = "" })
                        ) { entry ->
                            val cid = entry.arguments?.getString("contactId").orEmpty()
                            StatementScreen(
                                initialContactId = cid,
                                state = state,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(HisabatiRoutes.AI_ASSISTANT) {
                            AiAssistantScreen(
                                state = state,
                                messages = aiMessages,
                                streamingText = aiStreaming,
                                isBusy = aiBusy,
                                onBack = { navController.popBackStack() },
                                onToggleConsent = { granted -> viewModel.setAiConsent(granted) },
                                onAskQuestion = { q -> viewModel.askAiAssistant(q) }
                            )
                        }

                        composable(HisabatiRoutes.SEARCH) {
                            SearchScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onOpenDebt = { did -> navController.navigate(HisabatiRoutes.debtDetail(did)) },
                                onOpenContact = { cid -> navController.navigate(HisabatiRoutes.contactDetail(cid)) }
                            )
                        }

                        composable(HisabatiRoutes.SETTINGS) {
                            SettingsScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onSetDefaultCurrency = { viewModel.setDefaultCurrency(it) },
                                onAddCustomCurrency = { code, nameAr, sym, rate ->
                                    viewModel.addCustomCurrency(code, nameAr, sym, rate)
                                },
                                onOpenNotifications = { navController.navigate(HisabatiRoutes.REMINDER_SETTINGS) },
                                onOpenWhatsApp = { navController.navigate(HisabatiRoutes.WHATSAPP_SETTINGS) },
                                onOpenSms = { navController.navigate(HisabatiRoutes.SMS_SETTINGS) },
                                onOpenWorkspace = { navController.navigate(HisabatiRoutes.WORKSPACE_MEMBERS) },
                                onOpenBackup = { navController.navigate(HisabatiRoutes.BACKUP_SYNC) },
                                onOpenProfile = { navController.navigate(HisabatiRoutes.PROFILE) },
                                onOpenHelp = { navController.navigate(HisabatiRoutes.HELP_SUPPORT) }
                            )
                        }

                        composable(HisabatiRoutes.WORKSPACE_MEMBERS) {
                            WorkspaceMembersScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onUpdateWorkspace = { type, name -> viewModel.setWorkspaceConfig(type, name) },
                                onAddMember = { n, e, r, a, ed, p, rep ->
                                    viewModel.addWorkspaceMember(n, e, r, a, ed, p, rep)
                                },
                                onRemoveMember = { id -> viewModel.removeWorkspaceMember(id) },
                                onSetActiveMember = { id -> viewModel.setActiveMember(id) }
                            )
                        }

                        composable(HisabatiRoutes.BACKUP_SYNC) {
                            BackupSyncScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onSyncNow = { viewModel.syncNow() },
                                onExportJson = { viewModel.exportBackupJson() },
                                onImportJson = { viewModel.importBackupJson(it) }
                            )
                        }

                        composable(HisabatiRoutes.PROFILE) {
                            ProfileScreen(
                                state = state,
                                userDisplayName = viewModel.userDisplayName,
                                userEmail = viewModel.userEmail,
                                onBack = { navController.popBackStack() },
                                onSetPin = { viewModel.setPin(it) },
                                onDisablePin = { viewModel.disablePin() },
                                onSetBiometric = { viewModel.setBiometricEnabled(it) },
                                onSignOut = onSignOut,
                                onDeleteAccountData = {
                                    viewModel.deleteAccountAndAllData(onComplete = onSignOut)
                                }
                            )
                        }

                        composable(HisabatiRoutes.HELP_SUPPORT) {
                            HelpSupportScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
