package com.example.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.components.ApnaSubScreenTopBar

@Composable
fun LegalScreen(
    isPrivacy: Boolean,
    onBack: () -> Unit
) {
    val title = if (isPrivacy) "Privacy Policy" else "Terms, Help & Architecture"
    Scaffold(
        topBar = { ApnaSubScreenTopBar(title = title, onBack = onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isPrivacy) {
                item {
                    LegalSectionCard(
                        heading = "1. Data Minimization",
                        body = "ApnaHub authenticates via Google Sign-In and stores only minimal profile details (display name, username, email in private user doc, optional city, and optional bio). Your email address is never exposed on public_profiles."
                    )
                }
                item {
                    LegalSectionCard(
                        heading = "2. Permissions & Media Privacy",
                        body = "ApnaHub uses the zero-permission Android Photo Picker for image posts, avatars, marketplace photos, chat images, and QR image decoding. No broad storage, SMS, contacts, call-log, or background location permissions are requested."
                    )
                }
                item {
                    LegalSectionCard(
                        heading = "3. Safety & Account Deletion",
                        body = "You can block users, report inappropriate posts/listings/messages, and permanently delete your profile data directly from Settings."
                    )
                }
            } else {
                item {
                    LegalSectionCard(
                        heading = "Terms & Community Guidelines",
                        body = "Users are responsible for respectful conduct across Social Feed, Short Videos, Messaging, Marketplace, and Local Services. Harassment, impersonation, advance-payment fraud, or unlawful content may be reported and removed by administrators."
                    )
                }
                item {
                    LegalSectionCard(
                        heading = "Marketplace & Career Disclaimers",
                        body = "ApnaHub does not process payments or guarantee third-party deals. Never share OTPs, bank PINs, or advance transfers to unverified individuals. Sample demo job listings are labeled clearly."
                    )
                }
                item {
                    LegalSectionCard(
                        heading = "Monetization Readiness (Disabled by Default)",
                        body = "AdMob banners/interstitials and sponsored promotion slots are intentionally disabled by default in this build so there are zero fake ads, simulated payments, or test ad units. Future production releases can enable AdMob/Play Billing strictly with verified SDK configuration."
                    )
                }
                item {
                    LegalSectionCard(
                        heading = "About ApnaHub",
                        body = "ApnaHub — One App. Many Possibilities. Built natively with Kotlin, Jetpack Compose, Material 3, Media3 ExoPlayer, ZXing, Firebase Firestore, App Check, and Firebase AI Logic."
                    )
                }
            }
        }
    }
}

@Composable
private fun LegalSectionCard(heading: String, body: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(heading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
