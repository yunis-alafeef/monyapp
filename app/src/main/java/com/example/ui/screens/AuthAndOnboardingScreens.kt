package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.FintechSurfaceCard
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyRich
import com.example.ui.theme.PayableCoral
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        visible = true
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
        delay(1200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDeep, NavyRich, Color(0xFF0A2E2B))
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hisabati_launcher_icon),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "حساباتي | HISABATI",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldBright,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))
                CircularProgressIndicator(
                    color = GoldAccent,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val slides = listOf(
        Triple(
            "إدارة دقيقة لكل ديونك ومستحقاتك",
            "سجّل المستحق لك والمستحق عليك بعملات متعددة، مع حساب تلقائي للرصيد المتبقي وجدولة مرنة للأقساط.",
            Icons.Rounded.AccountBalanceWallet
        ),
        Triple(
            "يعمل دون إنترنت مع مزامنة سحابية آمنة",
            "أضف الأشخاص والدفعات في أي وقت حتى عند انقطاع الشبكة، وتتم المزامنة تلقائياً دون تكرار.",
            Icons.Rounded.CloudSync
        ),
        Triple(
            "تذكيرات ذكية وكشوف حساب PDF عربية",
            "أرسل تذكيرات احترافية عبر واتساب أو SMS، واستخرج تقارير وكشوف حساب جاهزة للطباعة والمشاركة.",
            Icons.Rounded.NotificationsActive
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "حساباتي",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onFinishOnboarding) {
                Text("تخطي")
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_onboarding_ledger),
                contentDescription = "رسم توضيحي للنظام المالي",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(26.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = slides[step].third,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = slides[step].first,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = slides[step].second,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                slides.indices.forEach { idx ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (idx == step) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (idx == step) EmeraldPrimary else MaterialTheme.colorScheme.outline)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (step < slides.lastIndex) step++ else onFinishOnboarding()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_next_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
            ) {
                Text(if (step < slides.lastIndex) "التالي" else "ابدأ الآن")
            }
        }
    }
}

@Composable
fun AuthGatewayScreen(
    onAuthSuccess: () -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0 = Login, 1 = SignUp, 2 = Recovery
    when (mode) {
        0 -> LoginScreen(
            onAuthSuccess = onAuthSuccess,
            onNavigateSignUp = { mode = 1 },
            onNavigateRecovery = { mode = 2 }
        )
        1 -> SignUpScreen(
            onAuthSuccess = onAuthSuccess,
            onNavigateLogin = { mode = 0 }
        )
        else -> AccountRecoveryScreen(
            onBack = { mode = 0 },
            onAuthSuccess = onAuthSuccess
        )
    }
}

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
    onNavigateSignUp: () -> Unit,
    onNavigateRecovery: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.hisabati_launcher_icon),
                    contentDescription = "شعار حساباتي",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "حساباتي | HISABATI",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "تسجيل الدخول الآمن",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "سجّل الدخول بحساب Google للوصول إلى دفاترك المالية المزامنة والمشفرة عبر جميع أجهزتك.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            FintechSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "عزل كامل للبيانات وحماية بصلاحيات وصول صارمة لكل مستخدم.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = GoldAccent)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "دعم الحسابات الشخصية والتجارية والعمل دون إنترنت.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = PayableCoral.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = PayableCoral,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = { isLoading = false }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.sign_in_google),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNavigateSignUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("go_to_signup_button")
            ) {
                Text("إنشاء حساب جديد")
            }
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(
                onClick = onNavigateRecovery,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("go_to_recovery_button")
            ) {
                Text("استعادة الوصول إلى الحساب أو كلمة المرور")
            }
        }
    }
}

@Composable
fun SignUpScreen(
    onAuthSuccess: () -> Unit,
    onNavigateLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            IconButton(onClick = onNavigateLogin) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "إنشاء حساب جديد في حساباتي",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "أنشئ مساحة عملك المالية خلال ثوانٍ عبر حساب Google الموثوق، مع تهيئة العملات والقوالب العربية تلقائياً.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            FintechSurfaceCard {
                Text("ماذا ستحصل عليه فور إنشاء الحساب؟", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("• دفتر ديون محلي سريع يعمل دون إنترنت.", style = MaterialTheme.typography.bodyMedium)
                Text("• مزامنة سحابية تلقائية ونسخ احتياطي مشفر.", style = MaterialTheme.typography.bodyMedium)
                Text("• إمكانية التبديل بين الحساب الشخصي والتجاري ودعوة موظفين.", style = MaterialTheme.typography.bodyMedium)
            }
            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(errorMessage!!, color = PayableCoral, style = MaterialTheme.typography.bodySmall)
            }
        }

        Column {
            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = { isLoading = false }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_up_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(stringResource(R.string.sign_up_google), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onNavigateLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("لدي حساب بالفعل — تسجيل الدخول")
            }
        }
    }
}

@Composable
fun AccountRecoveryScreen(
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
            }
            Spacer(Modifier.height(12.dp))
            Icon(Icons.Rounded.Security, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "استعادة الوصول وحساب Google",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            FintechSurfaceCard {
                Text(
                    text = "يعتمد تطبيق حساباتي على مصادقة Google الآمنة (Credential Manager) لحماية بياناتك المالية دون تخزين كلمات مرور تقليدية عرضة للاختراق.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "لاستعادة الوصول عند تغيير الهاتف أو نسيان كلمة مرور بريدك:\n1. تأكد من استعادة حساب Google الخاص بك.\n2. اضغط على تسجيل الدخول واختر نفس الحساب.\n3. ستُستعاد جميع الديون والمدفوعات من السحابة تلقائياً.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = {
                isLoading = true
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        isLoading = false
                        onAuthSuccess()
                    },
                    onAuthError = { isLoading = false },
                    scope = scope,
                    onAuthCancelled = { isLoading = false }
                )
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("تسجيل الدخول واستعادة البيانات")
        }
    }
}
