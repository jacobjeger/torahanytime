package com.torahanytime.audio.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torahanytime.audio.BuildConfig
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.focusable()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                "TorahAnytime",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TATBlue
            )
            Text(
                "Audio",
                fontSize = 16.sp,
                color = TATTextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                fontSize = 13.sp,
                color = TATTextSecondary
            )
            Spacer(Modifier.height(32.dp))

            HorizontalDivider()

            AboutLink(
                icon = Icons.Outlined.Language,
                label = "Website",
                subtitle = "www.torahanytime.com",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.torahanytime.com")))
                }
            )
            HorizontalDivider()

            AboutLink(
                icon = Icons.Outlined.PrivacyTip,
                label = "Privacy Policy",
                subtitle = "View our privacy policy",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.torahanytime.com/privacy")))
                }
            )
            HorizontalDivider()

            AboutLink(
                icon = Icons.Outlined.Email,
                label = "Contact",
                subtitle = "info@torahanytime.com",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:info@torahanytime.com")
                        putExtra(Intent.EXTRA_SUBJECT, "TorahAnytime Audio App Feedback")
                    }
                    context.startActivity(intent)
                }
            )
            HorizontalDivider()

            AboutLink(
                icon = Icons.Outlined.Info,
                label = "Build Info",
                subtitle = "SDK ${android.os.Build.VERSION.SDK_INT} | ${android.os.Build.MODEL}",
                onClick = {}
            )
            HorizontalDivider()

            Spacer(Modifier.weight(1f))
            Text(
                "Made with love for Torah learning",
                fontSize = 12.sp,
                color = TATTextSecondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun AboutLink(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusable()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = TATBlue,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = TATTextSecondary)
        }
    }
}
