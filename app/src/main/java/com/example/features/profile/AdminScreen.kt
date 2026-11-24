package com.example.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.components.ApnaSubScreenTopBar
import com.example.core.components.SafetyNoticeBanner
import com.example.core.constants.PakistanConstants
import com.example.core.firebase.ApnaDependencies
import com.example.core.i18n.LocalApnaStrings
import com.example.data.models.ReportItem
import com.example.features.create.SimpleDropdownField
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(
    deps: ApnaDependencies,
    isAdmin: Boolean,
    onBack: () -> Unit
) {
    val strings = LocalApnaStrings.current
    val scope = rememberCoroutineScope()

    var statusMsg by remember { mutableStateOf<String?>(null) }
    var reports by remember { mutableStateOf<List<ReportItem>>(emptyList()) }

    // Announcement fields
    var annTitle by remember { mutableStateOf("") }
    var annBody by remember { mutableStateOf("") }

    // Job publish fields
    var jobTitle by remember { mutableStateOf("") }
    var jobCompany by remember { mutableStateOf("") }
    var jobCity by remember { mutableStateOf("Lahore") }
    var jobCategory by remember { mutableStateOf("Private") }
    var jobQual by remember { mutableStateOf("Bachelor") }
    var jobSalary by remember { mutableStateOf("") }
    var jobDesc by remember { mutableStateOf("") }
    var jobInstructions by remember { mutableStateOf("") }
    var isSampleJob by remember { mutableStateOf(false) }

    LaunchedEffect(isAdmin) {
        if (isAdmin) {
            deps.listingsRepository.adminFetchReports().onSuccess { reports = it }
        }
    }

    Scaffold(
        topBar = { ApnaSubScreenTopBar(title = strings.adminTitle, onBack = onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!isAdmin) {
                item {
                    SafetyNoticeBanner(
                        text = "Admin Console Access Restricted: Write privileges for jobs, official announcements, and moderation reports are strictly enforced by Firestore Security Rules using verified admin claims and the admins collection."
                    )
                }
            } else {
                if (statusMsg != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(statusMsg.orEmpty(), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Publish Official Announcement
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Publish Official Announcement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = annTitle, onValueChange = { annTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = annBody, onValueChange = { annBody = it }, label = { Text("Announcement Body") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    scope.launch {
                                        deps.listingsRepository.adminPublishAnnouncement(annTitle, annBody, "General")
                                            .onSuccess {
                                                annTitle = ""
                                                annBody = ""
                                                statusMsg = "Announcement published!"
                                            }
                                            .onFailure { statusMsg = it.localizedMessage }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("admin_publish_announcement")
                            ) { Text("Publish Announcement") }
                        }
                    }
                }

                // Publish Job Listing
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Publish Job Listing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = jobTitle, onValueChange = { jobTitle = it }, label = { Text("Job Title") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = jobCompany, onValueChange = { jobCompany = it }, label = { Text("Organization / Company") }, modifier = Modifier.fillMaxWidth())
                            SimpleDropdownField("City", jobCity, PakistanConstants.selectableCities, onSelect = { jobCity = it })
                            SimpleDropdownField("Category", jobCategory, PakistanConstants.jobCategories.filter { it != "All" }, onSelect = { jobCategory = it })
                            SimpleDropdownField("Qualification", jobQual, PakistanConstants.jobQualifications.filter { it != "All" }, onSelect = { jobQual = it })
                            OutlinedTextField(value = jobSalary, onValueChange = { jobSalary = it }, label = { Text("Salary / Scale") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = jobDesc, onValueChange = { jobDesc = it }, label = { Text("Role Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = jobInstructions, onValueChange = { jobInstructions = it }, label = { Text("Official Application Instructions") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Mark as Sample Demo Listing")
                                Switch(checked = isSampleJob, onCheckedChange = { isSampleJob = it })
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        deps.listingsRepository.adminPublishJob(
                                            jobTitle, jobCompany, jobCity, jobCategory, jobQual,
                                            jobSalary, jobDesc, jobInstructions, isSampleJob
                                        ).onSuccess {
                                            jobTitle = ""
                                            jobCompany = ""
                                            jobDesc = ""
                                            jobInstructions = ""
                                            statusMsg = "Job published to Firestore!"
                                        }.onFailure { statusMsg = it.localizedMessage }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("admin_publish_job")
                            ) { Text("Publish Job") }
                        }
                    }
                }

                // Moderation Queue
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reported Content (${reports.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = {
                            scope.launch {
                                deps.listingsRepository.adminFetchReports().onSuccess { reports = it }
                            }
                        }) { Text("Refresh") }
                    }
                }

                items(reports, key = { it.id }) { report ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${report.targetType.uppercase()} • ${report.status}", style = MaterialTheme.typography.labelMedium)
                            Text("Reason: ${report.reason}", style = MaterialTheme.typography.bodyMedium)
                            Text("Target ID: ${report.targetId}", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        deps.listingsRepository.adminResolveReport(report.id, "dismissed")
                                        deps.listingsRepository.adminFetchReports().onSuccess { reports = it }
                                    }
                                }) { Text("Dismiss") }
                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    onClick = {
                                        scope.launch {
                                            deps.listingsRepository.adminRemoveTargetContent(report.targetType, report.targetId)
                                            deps.listingsRepository.adminResolveReport(report.id, "resolved")
                                            deps.listingsRepository.adminFetchReports().onSuccess { reports = it }
                                        }
                                    }
                                ) { Text("Remove & Resolve") }
                            }
                        }
                    }
                }
            }
        }
    }
}
