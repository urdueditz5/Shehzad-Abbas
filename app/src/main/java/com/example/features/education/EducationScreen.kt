package com.example.features.education

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.components.ApnaSubScreenTopBar
import com.example.core.constants.PakistanConstants
import com.example.core.firebase.ApnaDependencies
import com.example.core.i18n.LocalApnaStrings
import com.example.core.preferences.UserPrefsState
import com.example.core.theme.ElectricCyan
import com.example.core.theme.RoyalBlue
import com.example.core.theme.SuccessGreen
import com.example.data.models.EducationResource
import com.example.data.models.SavedItem

private data class StudyNoteSample(
    val id: String,
    val category: String,
    val subject: String,
    val title: String,
    val summary: String,
    val keyPoints: List<String>
)

private data class VocabEntry(
    val english: String,
    val urdu: String,
    val romanUrdu: String,
    val meaning: String
)

private data class QuizQuestion(
    val id: String,
    val category: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

private val sampleStudyNotes = listOf(
    StudyNoteSample(
        id = "edu_matric_bio",
        category = "Matric",
        subject = "Biology",
        title = "Cell Structure & Organelles",
        summary = "Core overview of prokaryotic vs eukaryotic cells for 9th/10th board preparation.",
        keyPoints = listOf(
            "Mitochondria produce ATP via cellular respiration.",
            "Ribosomes synthesize proteins from amino acids.",
            "Plant cells contain cell walls and chloroplasts."
        )
    ),
    StudyNoteSample(
        id = "edu_inter_eng",
        category = "Intermediate",
        subject = "English",
        title = "Formal Application & Essay Structure",
        summary = "Board exam layout for applications, letters, and structured paragraphs.",
        keyPoints = listOf(
            "Keep salutation and subject line concise and polite.",
            "Use 3-paragraph body: context, request, conclusion.",
            "Review tense consistency and punctuation before submission."
        )
    ),
    StudyNoteSample(
        id = "edu_fa_civics",
        category = "FA",
        subject = "Pakistan Studies",
        title = "Constitutional Milestones of Pakistan",
        summary = "Quick revision notes covering key dates from 1947 to 1973.",
        keyPoints = listOf(
            "Objectives Resolution was passed in March 1949.",
            "1973 Constitution established a parliamentary federal system.",
            "Allama Iqbal's 1930 Allahabad Address outlined vision."
        )
    ),
    StudyNoteSample(
        id = "edu_fsc_phys",
        category = "FSc",
        subject = "Physics",
        title = "Newton's Laws & Momentum",
        summary = "Essential formulas and conceptual checks for FSc Part 1.",
        keyPoints = listOf(
            "F = ma (Second Law relates net force to acceleration).",
            "Linear momentum p = mv is conserved in isolated systems.",
            "Impulse equals change in momentum (F * dt = dp)."
        )
    ),
    StudyNoteSample(
        id = "edu_ics_cs",
        category = "ICS",
        subject = "Computer Science",
        title = "Database Keys & Normalization",
        summary = "Primary, foreign, and candidate keys explained simply for ICS Part 2.",
        keyPoints = listOf(
            "Primary key uniquely identifies each record and cannot be null.",
            "Foreign key links two tables to maintain referential integrity.",
            "1NF removes repeating groups; 3NF removes transitive dependencies."
        )
    ),
    StudyNoteSample(
        id = "edu_icom_acc",
        category = "ICom",
        subject = "Accounting",
        title = "Double-Entry Accounting Equation",
        summary = "Assets = Liabilities + Owner's Equity with practical debit/credit rules.",
        keyPoints = listOf(
            "Assets and Expenses increase on Debit side.",
            "Liabilities, Capital, and Revenue increase on Credit side.",
            "Every transaction affects at least two accounts equally."
        )
    ),
    StudyNoteSample(
        id = "edu_uni_cv",
        category = "University",
        subject = "Career Readiness",
        title = "Writing an ATS-Friendly CV",
        summary = "How university graduates in Pakistan can present projects clearly.",
        keyPoints = listOf(
            "Quantify final year project results instead of listing buzzwords.",
            "Keep layout single-column and export as crisp PDF.",
            "Highlight GitHub, internships, and verified certifications."
        )
    ),
    StudyNoteSample(
        id = "edu_gk_pk",
        category = "General Knowledge",
        subject = "Pakistan Geography",
        title = "Major Rivers, Passes & Peaks",
        summary = "Quick facts for competitive & entry test preparation.",
        keyPoints = listOf(
            "K2 (8,611 m) is the highest peak in Pakistan and second highest globally.",
            "Indus is the longest river in Pakistan flowing north to south.",
            "Khunjerab Pass connects Gilgit-Baltistan with China."
        )
    ),
    StudyNoteSample(
        id = "edu_eng_spoken",
        category = "English Learning",
        subject = "Daily Communication",
        title = "Polite Workplace & Interview Phrases",
        summary = "Natural phrases for interviews, emails, and meetings.",
        keyPoints = listOf(
            "\"Could you please clarify the requirements?\"",
            "\"Thank you for the opportunity to discuss my experience.\"",
            "\"I will share an update by tomorrow afternoon.\""
        )
    ),
    StudyNoteSample(
        id = "edu_cs_git",
        category = "Computer Skills",
        subject = "Digital Productivity",
        title = "Spreadsheet Formulas & Cybersecurity Hygiene",
        summary = "Essential office and online safety skills for students and freelancers.",
        keyPoints = listOf(
            "Use =SUM(), =AVERAGE(), and =IF() to automate marksheets & budgets.",
            "Enable 2FA on email and never share OTP codes with anyone.",
            "Verify sender domain before clicking bank or scholarship links."
        )
    )
)

private val vocabDeck = listOf(
    VocabEntry("Opportunity", "موقع", "Mauqa", "A favorable time or chance to do something"),
    VocabEntry("Consistency", "تسلسل", "Tasalsul", "Steady effort over time"),
    VocabEntry("Collaboration", "تعاون", "Ta'awun", "Working together towards a shared goal"),
    VocabEntry("Responsibility", "ذمہ داری", "Zimmedari", "Being accountable for a duty or task"),
    VocabEntry("Innovation", "جدت", "Jiddat", "Introducing new ideas or practical methods"),
    VocabEntry("Verification", "تصدیق", "Tasdeeq", "Confirming accuracy from official sources")
)

private val sampleQuizQuestions = listOf(
    QuizQuestion(
        id = "q1",
        category = "General Knowledge",
        question = "Which mountain pass connects Pakistan with China along the Karakoram Highway?",
        options = listOf("Khyber Pass", "Khunjerab Pass", "Bolan Pass", "Babusar Pass"),
        correctIndex = 1,
        explanation = "Khunjerab Pass (4,693 m) connects Gilgit-Baltistan with Xinjiang, China."
    ),
    QuizQuestion(
        id = "q2",
        category = "Computer Skills",
        question = "In relational databases, what ensures every row in a table is uniquely identifiable?",
        options = listOf("Primary Key", "Index Alias", "View Schema", "Join Clause"),
        correctIndex = 0,
        explanation = "A Primary Key uniquely identifies each record and cannot contain NULL values."
    ),
    QuizQuestion(
        id = "q3",
        category = "FSc",
        question = "What is the SI unit of linear momentum?",
        options = listOf("kg·m/s²", "N/m", "kg·m/s", "Joule"),
        correctIndex = 2,
        explanation = "Momentum p = m × v, so its SI unit is kilogram-meter per second (kg·m/s)."
    ),
    QuizQuestion(
        id = "q4",
        category = "English Learning",
        question = "Choose the grammatically correct sentence:",
        options = listOf(
            "She don't like late submissions.",
            "Neither of the students was absent.",
            "He did not went to the library.",
            "They was preparing for ICS."
        ),
        correctIndex = 1,
        explanation = "'Neither' is singular and takes the singular verb 'was'."
    ),
    QuizQuestion(
        id = "q5",
        category = "ICom",
        question = "According to the accounting equation, Assets equal:",
        options = listOf(
            "Revenue - Expenses",
            "Liabilities + Owner's Equity",
            "Capital - Drawings",
            "Cash + Inventory"
        ),
        correctIndex = 1,
        explanation = "The fundamental equation is Assets = Liabilities + Owner's Equity."
    )
)

@Composable
fun EducationScreen(
    deps: ApnaDependencies,
    userPrefs: UserPrefsState,
    savedItems: List<SavedItem>,
    onToggleLessonCompleted: (String) -> Unit,
    onRecordQuizScore: (Int) -> Unit,
    onToggleSave: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalApnaStrings.current
    val remoteResources by deps.listingsRepository.observeEducationResources()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember { listOf("All") + PakistanConstants.educationCategories }

    // Quiz state
    var quizAnswers by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var quizSubmitted by remember { mutableStateOf(false) }

    val filteredSamples = remember(selectedCategory) {
        if (selectedCategory == "All") sampleStudyNotes
        else sampleStudyNotes.filter { it.category == selectedCategory }
    }
    val filteredRemote = remember(remoteResources, selectedCategory) {
        if (selectedCategory == "All") remoteResources
        else remoteResources.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }
    val savedEducationIds = remember(savedItems) {
        savedItems.filter { it.itemType == "education" }.map { it.itemId }.toSet()
    }

    Scaffold(
        topBar = {
            ApnaSubScreenTopBar(title = strings.educationTitle, onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Study Notes") },
                    modifier = Modifier.testTag("edu_tab_notes")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Vocabulary") },
                    modifier = Modifier.testTag("edu_tab_vocab")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Quiz") },
                    modifier = Modifier.testTag("edu_tab_quiz")
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Progress summary card
                item {
                    val totalSampleCount = sampleStudyNotes.size
                    val completedCount = userPrefs.completedLessons.size
                    val progress = (completedCount.toFloat() / totalSampleCount.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = RoyalBlue)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Learning Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    "Best Quiz: ${userPrefs.quizBestScore}% (${userPrefs.quizzesTaken} taken)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RoyalBlue
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = RoyalBlue
                            )
                            Text(
                                "$completedCount lessons marked completed • Bookmark notes to keep them on Profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(categories) { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat) },
                                        modifier = Modifier.testTag("edu_cat_$cat")
                                    )
                                }
                            }
                        }

                        if (filteredRemote.isNotEmpty()) {
                            items(filteredRemote, key = { "remote_${it.id}" }) { res ->
                                RemoteEducationCard(
                                    resource = res,
                                    completed = res.id in userPrefs.completedLessons,
                                    saved = res.id in savedEducationIds,
                                    onToggleComplete = { onToggleLessonCompleted(res.id) },
                                    onToggleSave = {
                                        onToggleSave("education", res.id, res.title, "${res.category} • ${res.subject}")
                                    }
                                )
                            }
                        }

                        items(filteredSamples, key = { it.id }) { note ->
                            val isCompleted = note.id in userPrefs.completedLessons
                            val isSaved = note.id in savedEducationIds
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            AssistChip(onClick = {}, label = { Text(note.category) })
                                            Surface(
                                                color = ElectricCyan.copy(alpha = 0.16f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = strings.sampleBadge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    onToggleSave(
                                                        "education",
                                                        note.id,
                                                        note.title,
                                                        "${note.category} • ${note.subject}"
                                                    )
                                                },
                                                modifier = Modifier.testTag("edu_save_${note.id}")
                                            ) {
                                                Icon(
                                                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                    contentDescription = "Save study note",
                                                    tint = if (isSaved) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onToggleLessonCompleted(note.id) },
                                                modifier = Modifier.testTag("edu_complete_${note.id}")
                                            ) {
                                                Icon(
                                                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = "Mark lesson complete",
                                                    tint = if (isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${note.subject}: ${note.title}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = note.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    note.keyPoints.forEach { pt ->
                                        Text("• $pt", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Translate, contentDescription = null, tint = RoyalBlue)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "English • اردو • Roman Urdu Builder",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(vocabDeck) { entry ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(entry.english, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RoyalBlue)
                                        Text(entry.urdu, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Roman Urdu: ${entry.romanUrdu}", style = MaterialTheme.typography.bodyMedium)
                                    Text(entry.meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    2 -> {
                        items(sampleQuizQuestions, key = { it.id }) { q ->
                            val picked = quizAnswers[q.id]
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(q.category, style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                                    Text(q.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    q.options.forEachIndexed { idx, opt ->
                                        val selected = picked == idx
                                        val isRight = quizSubmitted && idx == q.correctIndex
                                        val isWrongPicked = quizSubmitted && selected && idx != q.correctIndex
                                        OutlinedButton(
                                            onClick = {
                                                if (!quizSubmitted) {
                                                    quizAnswers = quizAnswers + (q.id to idx)
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("quiz_opt_${q.id}_$idx"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    isRight -> "✓ $opt"
                                                    isWrongPicked -> "✗ $opt"
                                                    selected -> "● $opt"
                                                    else -> opt
                                                },
                                                color = when {
                                                    isRight -> SuccessGreen
                                                    isWrongPicked -> MaterialTheme.colorScheme.error
                                                    selected -> RoyalBlue
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    if (quizSubmitted) {
                                        Text(
                                            text = q.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            if (!quizSubmitted) {
                                Button(
                                    onClick = {
                                        val correct = sampleQuizQuestions.count { quizAnswers[it.id] == it.correctIndex }
                                        val percent = (correct * 100) / sampleQuizQuestions.size
                                        quizSubmitted = true
                                        onRecordQuizScore(percent)
                                    },
                                    enabled = quizAnswers.size == sampleQuizQuestions.size,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("submit_quiz_button")
                                ) {
                                    Text("Submit Quiz (${quizAnswers.size}/${sampleQuizQuestions.size})")
                                }
                            } else {
                                val correct = sampleQuizQuestions.count { quizAnswers[it.id] == it.correctIndex }
                                val percent = (correct * 100) / sampleQuizQuestions.size
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Score: $correct / ${sampleQuizQuestions.size} ($percent%)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                quizAnswers = emptyMap()
                                                quizSubmitted = false
                                            },
                                            modifier = Modifier.testTag("retake_quiz_button")
                                        ) {
                                            Text("Retake Quiz")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteEducationCard(
    resource: EducationResource,
    completed: Boolean,
    saved: Boolean,
    onToggleComplete: () -> Unit,
    onToggleSave: () -> Unit
) {
    val strings = LocalApnaStrings.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = {}, label = { Text(resource.category) })
                    if (resource.sampleContent) {
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(strings.sampleBadge, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
                Row {
                    IconButton(onClick = onToggleSave) {
                        Icon(
                            if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (saved) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleComplete) {
                        Icon(
                            if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Complete",
                            tint = if (completed) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(resource.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (resource.description.isNotBlank()) {
                Text(resource.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (resource.content.isNotBlank()) {
                Text(resource.content, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
