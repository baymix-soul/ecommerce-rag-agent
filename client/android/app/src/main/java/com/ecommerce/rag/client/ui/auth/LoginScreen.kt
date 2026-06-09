package com.ecommerce.rag.client.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 阶段 9：电商风格登录页。
 *
 * 简化点：
 *  - 默认预填 demo / demo123，方便演示；
 *  - 提供"使用 demo 账号一键登录"按钮，等价于直接发起登录；
 *  - 登录成功后通过 onLoggedIn 回调由 MainActivity 决定返回哪一页（默认 Browse）；
 *  - 不做注册 / 找回密码；这些只是占位文字。
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoggedIn: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var username by rememberSaveable {
        mutableStateOf(uiState.username.ifBlank { AuthViewModel.DEFAULT_DEMO_USERNAME })
    }
    var password by rememberSaveable {
        mutableStateOf(AuthViewModel.DEFAULT_DEMO_PASSWORD)
    }

    val latestOnLoggedIn by rememberUpdatedState(onLoggedIn)

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            latestOnLoggedIn()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginColors.PageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            LoginTopRow(onBack = onBack)

            Spacer(modifier = Modifier.height(24.dp))

            HeroBanner()

            Spacer(modifier = Modifier.height(24.dp))

            LoginCard(
                username = username,
                password = password,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onUsernameChange = {
                    username = it
                    if (uiState.error != null) viewModel.clearError()
                },
                onPasswordChange = {
                    password = it
                    if (uiState.error != null) viewModel.clearError()
                },
                onSubmit = {
                    viewModel.login(username, password)
                },
                onUseDemo = {
                    username = AuthViewModel.DEFAULT_DEMO_USERNAME
                    password = AuthViewModel.DEFAULT_DEMO_PASSWORD
                    viewModel.login(
                        AuthViewModel.DEFAULT_DEMO_USERNAME,
                        AuthViewModel.DEFAULT_DEMO_PASSWORD
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            FooterHint()

            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun LoginTopRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 0.dp,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onBack() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "‹",
                    style = MaterialTheme.typography.headlineSmall,
                    color = LoginColors.TextPrimary,
                    fontWeight = FontWeight.Light
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "账号登录",
            style = MaterialTheme.typography.titleMedium,
            color = LoginColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        LoginColors.Dark,
                        Color(0xFF1F0608),
                        LoginColors.AccentRed
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Text(
                    text = "✨ 欢迎回来",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Text(
                text = "登录后同步购物车与导购记录",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "支持 demo 体验账号，一键登录即可解锁加购 / 收藏 / 对话式买单。",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun LoginCard(
    username: String,
    password: String,
    isLoading: Boolean,
    error: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onUseDemo: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "账号",
                style = MaterialTheme.typography.labelMedium,
                color = LoginColors.TextSecondary
            )
            LoginField(
                value = username,
                onValueChange = onUsernameChange,
                placeholder = "请输入用户名",
                isPassword = false
            )

            Text(
                text = "密码",
                style = MaterialTheme.typography.labelMedium,
                color = LoginColors.TextSecondary
            )
            LoginField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = "请输入密码",
                isPassword = true
            )

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = LoginColors.AccentRed
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            PrimaryGradientButton(
                text = if (isLoading) "登录中…" else "登录",
                enabled = !isLoading,
                onClick = onSubmit
            )

            Spacer(modifier = Modifier.height(2.dp))

            SecondaryButton(
                text = "使用 demo 账号一键登录",
                enabled = !isLoading,
                onClick = onUseDemo
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = LoginColors.AccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LoginColors.FieldBg,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LoginColors.TextTertiary
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = LoginColors.TextPrimary,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    ),
                    visualTransformation = if (isPassword) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    cursorBrush = SolidColor(LoginColors.AccentRed),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PrimaryGradientButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(LoginColors.Dark, LoginColors.AccentRed))
    } else {
        Brush.horizontalGradient(listOf(LoginColors.TextTertiary, LoginColors.TextTertiary))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(brush = brush)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = LoginColors.FieldBg,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = LoginColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FooterHint() {
    Text(
        text = "登录即代表同意《用户协议》与《隐私政策》（演示文本）",
        style = MaterialTheme.typography.labelSmall,
        color = LoginColors.TextTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

private object LoginColors {
    val PageBg = Color(0xFFF6F6F6)
    val TextPrimary = Color(0xFF18181B)
    val TextSecondary = Color(0xFF71717A)
    val TextTertiary = Color(0xFFA1A1AA)
    val AccentRed = Color(0xFFEF4444)
    val Dark = Color(0xFF09090B)
    val FieldBg = Color(0xFFF1F1F1)
}
