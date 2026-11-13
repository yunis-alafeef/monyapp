package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.local.PreferencesStore
import com.example.data.local.UserAppPreferences
import com.example.data.repository.AiFinancialRepository
import com.example.data.repository.HisabatiRepository
import com.example.notifications.NotificationHelper
import com.example.security.configureAppCheck
import com.example.ui.components.PinUnlockScreen
import com.example.ui.navigation.HisabatiAppShell
import com.example.ui.screens.AuthGatewayScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.HisabatiTheme
import com.example.ui.viewmodel.HisabatiViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // App Check must be initialized before ViewModel/Firebase SDK access with the launch Intent
        configureAppCheck(this, intent)

        val preferencesStore = PreferencesStore(applicationContext)
        val repository = HisabatiRepository(applicationContext)
        val aiRepository = AiFinancialRepository()

        setContent {
            val prefs by preferencesStore.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = UserAppPreferences()
            )
            val firebaseUser by remember { Firebase.auth.authStateFlow() }
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

            val scope = rememberCoroutineScope()
            val credentialManager = remember { CredentialManager.create(this@MainActivity) }

            var splashFinished by rememberSaveable { mutableStateOf(false) }
            var pinUnlocked by rememberSaveable { mutableStateOf(false) }

            val biometricAvailable = remember {
                BiometricManager.from(this@MainActivity).canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                ) == BiometricManager.BIOMETRIC_SUCCESS
            }

            fun triggerBiometricUnlock() {
                val executor = ContextCompat.getMainExecutor(this@MainActivity)
                val prompt = BiometricPrompt(
                    this@MainActivity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            pinUnlocked = true
                        }
                    }
                )
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("فتح تطبيق حساباتي")
                    .setSubtitle("استخدم البصمة الحيوية للوصول إلى سجلاتك المالية")
                    .setNegativeButtonText("استخدام رمز PIN")
                    .build()
                prompt.authenticate(promptInfo)
            }

            HisabatiTheme(themeMode = prefs.themeMode) {
                when {
                    !splashFinished -> {
                        SplashScreen(onFinished = { splashFinished = true })
                    }

                    !prefs.onboardingCompleted -> {
                        OnboardingScreen(
                            onFinishOnboarding = {
                                scope.launch { preferencesStore.setOnboardingCompleted(true) }
                            }
                        )
                    }

                    firebaseUser == null -> {
                        pinUnlocked = false
                        AuthGatewayScreen(onAuthSuccess = {})
                    }

                    prefs.pinEnabled && !pinUnlocked -> {
                        LaunchedEffect(prefs.biometricEnabled, biometricAvailable) {
                            if (prefs.biometricEnabled && biometricAvailable) {
                                triggerBiometricUnlock()
                            }
                        }
                        PinUnlockScreen(
                            biometricAvailable = prefs.biometricEnabled && biometricAvailable,
                            onVerifyPin = { candidate ->
                                preferencesStore.verifyPin(candidate, prefs.pinSalt, prefs.pinHash)
                            },
                            onBiometricClick = { triggerBiometricUnlock() },
                            onUnlocked = { pinUnlocked = true }
                        )
                    }

                    else -> {
                        val user = firebaseUser!!
                        LaunchedEffect(user.uid) {
                            NotificationHelper.schedulePeriodicWorkers(applicationContext)
                        }
                        val vm: HisabatiViewModel = viewModel(
                            key = user.uid,
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return HisabatiViewModel(
                                        repository = repository,
                                        preferencesStore = preferencesStore,
                                        aiRepository = aiRepository,
                                        currentUserId = user.uid,
                                        userDisplayName = user.displayName ?: "مستخدم حساباتي",
                                        userEmail = user.email
                                    ) as T
                                }
                            }
                        )
                        HisabatiAppShell(
                            viewModel = vm,
                            onSignOut = {
                                pinUnlocked = false
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
