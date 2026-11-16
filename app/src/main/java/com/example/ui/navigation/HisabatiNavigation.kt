package com.example.ui.navigation

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.model.DebtDirection
import com.example.notifications.NotificationHelper
import com.example.ui.components.AnmaSoftFooterBar
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
import com.example.ui.screens.PrintSettingsScreen
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
import com.example.ui.theme.HisabatiTheme
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
    const val PRINT_SETTINGS = "print_settings"
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
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
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

    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var promptedNotificationPerm by rememberSaveable { mutableStateOf(false) }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            NotificationHelper.schedulePeriodicWorkers(
                context = context,
                preferredHour = state.preferences.defaultReminderHour,
                preferredMinute = state.preferences.defaultReminderMinute
            )
            viewModel.checkLocalNotifications(context, notifyEmpty = false)
        }
    }

    LaunchedEffect(viewModel.currentUserId, state.preferences.notificationsEnabled) {
        if (state.preferences.notificationsEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !NotificationHelper.hasNotificationPermission(context) &&
                !promptedNotificationPerm
            ) {
                promptedNotificationPerm = true
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                NotificationHelper.schedulePeriodicWorkers(
                    context = context,
                    preferredHour = state.preferences.defaultReminderHour,
                    preferredMinute = state.preferences.defaultReminderMinute
                )
            }
        }
    }

    LaunchedEffect(viewModel.currentUserId, state.debts.size, state.installments.size) {
        if (!state.isLoading && state.preferences.notificationsEnabled) {
            viewModel.checkLocalNotifications(context, notifyEmpty = false)
        }
    }

    LaunchedEffect(state.feedbackMessage) {
        val msg = state.feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val topDestinations = listOf(
        TopLevelDestination(HisabatiRoutes.DASHBOARD, HisabatiRoutes.DASHBOARD, "الرئيسية", Icons.Rounded.Home, Icons.Outlined.Home, "nav_dashboard"),
        TopLevelDestination(HisabatiRoutes.debtsHub(0), "debts_hub", "الديون", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_debts"),
        TopLevelDestination(HisabatiRoutes.CONTACTS, HisabatiRoutes.CONTACTS, "الأشخاص", Icons.Rounded.Group, Icons.Outlined.Group, "nav_contacts"),
        TopLevelDestination(HisabatiRoutes.MORE_HUB, HisabatiRoutes.MORE_HUB, "المزيد", Icons.Rounded.GridView, Icons.Outlined.GridView, "nav_more")
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
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showTopLevelNav && !isWide) {
                        SlimBottomNavigationBar(
                            destinations = topDestinations,
                            currentRoute = currentRoute,
                            onSelect = { navigateTopLevel(it.route) }
                        )
                    }
                    AnmaSoftFooterBar(
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                if (showTopLevelNav && isWide) {
                    NavigationRail {
                        topDestinations.forEach { dest ->
                            val selected = currentRoute.startsWith(dest.baseRoute)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        if (selected) dest.activeIcon else dest.inactiveIcon,
                                        contentDescription = dest.label
                                    )
                                },
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
                                onOpenSettings = { navController.navigate(HisabatiRoutes.SETTINGS) },
                                onSelectCurrency = { code -> viewModel.setDefaultCurrency(code) },
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
                                onOpenPrintSettings = { navController.navigate(HisabatiRoutes.PRINT_SETTINGS) },
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
                                preferences = state.preferences,
                                onBack = { navController.popBackStack() },
                                onSave = { name, phone, addr, notes, avatar, idempotencyKey, onError, onSuccess ->
                                    viewModel.saveContact(
                                        existingId = existing?.id,
                                        fullName = name,
                                        phone = phone,
                                        address = addr,
                                        notes = notes,
                                        avatarUri = avatar,
                                        idempotencyKey = idempotencyKey,
                                        onError = onError,
                                        onSaved = onSuccess
                                    )
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
                                onSaveDebt = { contactId, d, minor, cur, recAt, dueAt, reason, pm, att, notes, instCount, idemKey, onError, onSuccess ->
                                    viewModel.addDebt(
                                        contactId = contactId,
                                        direction = d,
                                        principalMinor = minor,
                                        currencyCode = cur,
                                        recordedAtMillis = recAt,
                                        dueAtMillis = dueAt,
                                        reason = reason,
                                        paymentMethod = pm,
                                        attachmentUri = att,
                                        notes = notes,
                                        installmentCount = instCount,
                                        idempotencyKey = idemKey,
                                        onError = onError,
                                        onSaved = onSuccess
                                    )
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
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onSubmitPayment = { amt, disc, method, paidAt, notes, settle, idemKey, onError, onSuccess ->
                                        viewModel.recordPayment(
                                            debtId = debt.id,
                                            amountMinor = amt,
                                            discountMinor = disc,
                                            method = method,
                                            paidAtMillis = paidAt,
                                            notes = notes,
                                            settleRemaining = settle,
                                            idempotencyKey = idemKey,
                                            onError = onError,
                                            onSuccess = onSuccess
                                        )
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
                                    preferences = state.preferences,
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
                                onOpenReminderSettings = { navController.navigate(HisabatiRoutes.REMINDER_SETTINGS) },
                                onCheckDueNow = { viewModel.checkLocalNotifications(context, notifyEmpty = true) }
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
                                    NotificationHelper.scheduleOneShotReminder(context, rem.triggerAtMillis)
                                },
                                onDeleteReminder = { id -> viewModel.deleteReminder(id) },
                                onCheckDueNow = { viewModel.checkLocalNotifications(context, notifyEmpty = true) },
                                onSendTestNotification = { viewModel.sendTestLocalNotification(context) }
                            )
                        }

                        composable(HisabatiRoutes.WHATSAPP_SETTINGS) {
                            WhatsAppSettingsScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onSaveRelayConfig = { wa, sms, url, phId, sid ->
                                    viewModel.setMessagingRelaySettings(wa, sms, url, phId, sid)
                                },
                                onSaveWhatsAppFormat = { style, hdr, brk, dts, rsn, ftr, sig ->
                                    viewModel.setWhatsAppFormatSettings(style, hdr, brk, dts, rsn, ftr, sig)
                                },
                                onSaveTemplate = { tpl -> viewModel.saveTemplate(tpl) },
                                onExportWhatsAppJson = { viewModel.exportWhatsAppConfigJson() },
                                onImportWhatsAppJson = { viewModel.importWhatsAppConfigJson(it) },
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
                                onOpenStatement = { cid -> navController.navigate(HisabatiRoutes.statement(cid)) },
                                onOpenPrintSettings = { navController.navigate(HisabatiRoutes.PRINT_SETTINGS) }
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
                                onAskQuestion = { q, grant -> viewModel.askAiAssistant(q, grantConsentAndSend = grant) },
                                onClearChat = { viewModel.clearAiChat() }
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
                                onSetThemePalette = { viewModel.setThemePalette(it) },
                                onApplyCustomColors = { p, a, m -> viewModel.setCustomThemeColors(p, a, m) },
                                onSaveCustomTheme = { n, p, a, d -> viewModel.saveCustomThemePreset(n, p, a, d) },
                                onDeleteCustomTheme = { viewModel.deleteCustomThemePreset(it) },
                                onSetDefaultCurrency = { viewModel.setDefaultCurrency(it) },
                                onAddCustomCurrency = { code, nameAr, sym, rate ->
                                    viewModel.addCustomCurrency(code, nameAr, sym, rate)
                                },
                                onOpenNotifications = { navController.navigate(HisabatiRoutes.REMINDER_SETTINGS) },
                                onOpenPrintSettings = { navController.navigate(HisabatiRoutes.PRINT_SETTINGS) },
                                onOpenWhatsApp = { navController.navigate(HisabatiRoutes.WHATSAPP_SETTINGS) },
                                onOpenSms = { navController.navigate(HisabatiRoutes.SMS_SETTINGS) },
                                onOpenWorkspace = { navController.navigate(HisabatiRoutes.WORKSPACE_MEMBERS) },
                                onOpenBackup = { navController.navigate(HisabatiRoutes.BACKUP_SYNC) },
                                onOpenProfile = { navController.navigate(HisabatiRoutes.PROFILE) },
                                onOpenHelp = { navController.navigate(HisabatiRoutes.HELP_SUPPORT) }
                            )
                        }

                        composable(HisabatiRoutes.PRINT_SETTINGS) {
                            PrintSettingsScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onSavePrintSettings = { tpl, name, phone, addr, tax, footer, showLogo, showInmaH, showInmaF, showSig, showWm, dCol, rCol, sCol, gCol ->
                                    viewModel.setPrintSettings(
                                        template = tpl,
                                        companyName = name,
                                        companyPhone = phone,
                                        companyAddress = addr,
                                        taxOrCrNumber = tax,
                                        footerNote = footer,
                                        showLogoInHeader = showLogo,
                                        showInmaSoftInHeader = showInmaH,
                                        showInmaSoftInFooter = showInmaF,
                                        showSignatureBox = showSig,
                                        showWatermarkBadge = showWm,
                                        debtColorHex = dCol,
                                        receiptColorHex = rCol,
                                        statementColorHex = sCol,
                                        reportColorHex = gCol
                                    )
                                },
                                onLogoUpdated = { path -> viewModel.setPrintLogoPath(path) }
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

@Composable
private fun SlimBottomNavigationBar(
    destinations: List<TopLevelDestination>,
    currentRoute: String,
    onSelect: (TopLevelDestination) -> Unit
) {
    val hc = HisabatiTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 0.5.dp, color = hc.dividerSubtle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { dest ->
                    val selected = currentRoute.startsWith(dest.baseRoute)
                    val interactionSource = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onSelect(dest) }
                            .testTag(dest.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 14.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selected) dest.activeIcon else dest.inactiveIcon,
                                contentDescription = dest.label,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
