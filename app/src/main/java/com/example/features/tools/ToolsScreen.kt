package com.example.features.tools

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.components.ApnaEmptyState
import com.example.core.components.ApnaSubScreenTopBar
import com.example.core.i18n.LocalApnaStrings
import com.example.core.theme.RoyalBlue
import com.example.features.create.SimpleDropdownField
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onBack: () -> Unit
) {
    val strings = LocalApnaStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val todos by viewModel.todos.collectAsStateWithLifecycle()

    val tools = listOf(
        "Calculator",
        "Unit Converter",
        "Percentage",
        "Age & Dates",
        "Notes",
        "To-Do & Reminders",
        "Password",
        "QR Tools"
    )
    var activeTool by remember { mutableStateOf("Calculator") }

    Scaffold(
        topBar = { ApnaSubScreenTopBar(title = strings.toolsTitle, onBack = onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tools) { tool ->
                        FilterChip(
                            selected = activeTool == tool,
                            onClick = { activeTool = tool },
                            label = { Text(tool) },
                            modifier = Modifier.testTag("tool_tab_${tool.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }

            when (activeTool) {
                "Calculator" -> item { CalculatorCard() }
                "Unit Converter" -> item { UnitConverterCard() }
                "Percentage" -> item { PercentageCard() }
                "Age & Dates" -> item { AgeAndDateCard() }
                "Notes" -> {
                    item {
                        NotesComposerCard(onSave = { t, c, cb -> viewModel.addNote(t, c, cb) })
                    }
                    if (notes.isEmpty()) {
                        item {
                            ApnaEmptyState(
                                title = "No notes saved yet",
                                subtitle = "Create personal notes backed by your private Firestore account."
                            )
                        }
                    } else {
                        items(notes, key = { it.id }) { note ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        if (note.content.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(note.content, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete note")
                                    }
                                }
                            }
                        }
                    }
                }
                "To-Do & Reminders" -> {
                    item {
                        TodoComposerCard(onAdd = { title, reminder, cb -> viewModel.addTodo(title, reminder, cb) })
                    }
                    if (todos.isEmpty()) {
                        item {
                            ApnaEmptyState(
                                title = "No tasks yet",
                                subtitle = "Add daily study, job, or household tasks with optional reminder labels."
                            )
                        }
                    } else {
                        items(todos, key = { it.id }) { todo ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = todo.completed,
                                        onCheckedChange = { checked -> viewModel.toggleTodo(todo.id, checked) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = todo.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                        if (todo.reminderLabel.isNotBlank()) {
                                            Text(
                                                text = "Reminder: ${todo.reminderLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RoyalBlue
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteTodo(todo.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete task")
                                    }
                                }
                            }
                        }
                    }
                }
                "Password" -> item {
                    PasswordGeneratorCard(onCopy = { clipboard.setText(AnnotatedString(it)) })
                }
                "QR Tools" -> item {
                    QrToolsCard(
                        onCopy = { clipboard.setText(AnnotatedString(it)) },
                        onDecodeUri = { uri, onResult ->
                            scope.launch {
                                onResult(QrHelper.decodeQrFromUri(context, uri))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatorCard() {
    var expr by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }
    val keys = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf("C", "0", ".", "+")
    )
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Quick Calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = expr,
                onValueChange = { expr = it },
                label = { Text("Expression (e.g., 1250*1.15)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_input")
            )
            Text(
                text = "Result: $resultText",
                style = MaterialTheme.typography.headlineSmall,
                color = RoyalBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("calc_result")
            )
            keys.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { ch ->
                        OutlinedButton(
                            onClick = {
                                when (ch) {
                                    "C" -> {
                                        expr = ""
                                        resultText = "0"
                                    }
                                    else -> expr += ch
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(ch, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Button(
                onClick = {
                    ToolCalculators.evaluateArithmetic(expr)
                        .onSuccess { v ->
                            resultText = if (v % 1.0 == 0.0) v.toLong().toString() else "%.4f".format(v).trimEnd('0').trimEnd('.')
                        }
                        .onFailure { resultText = it.localizedMessage ?: "Error" }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("calc_equals_button")
            ) {
                Text("Calculate (=)")
            }
        }
    }
}

@Composable
private fun UnitConverterCard() {
    val domains = mapOf(
        "Area (Pakistan Land)" to listOf("Marla (272.25)", "Marla (225)", "Kanal (20 Marla)", "Sq.Ft", "Sq.Yd (Gaz)", "Sq.Meter"),
        "Length" to listOf("Meter", "Kilometer", "Centimeter", "Foot", "Inch", "Mile"),
        "Weight" to listOf("Kilogram", "Gram", "Maund (40 Kg)", "Pound"),
        "Temperature" to listOf("Celsius", "Fahrenheit")
    )
    var domain by remember { mutableStateOf("Area (Pakistan Land)") }
    val units = domains.getValue(domain)
    var fromUnit by remember(domain) { mutableStateOf(units.first()) }
    var toUnit by remember(domain) { mutableStateOf(units.getOrElse(3) { units.last() }) }
    var inputVal by remember { mutableStateOf("5") }
    val output = remember(inputVal, fromUnit, toUnit) {
        val v = inputVal.toDoubleOrNull() ?: 0.0
        "%.4f".format(ToolCalculators.convertUnit(v, fromUnit, toUnit)).trimEnd('0').trimEnd('.')
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Unit Converter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SimpleDropdownField("Measurement Type", domain, domains.keys.toList(), onSelect = { domain = it })
            OutlinedTextField(
                value = inputVal,
                onValueChange = { inputVal = it },
                label = { Text("Value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SimpleDropdownField("From", fromUnit, units, onSelect = { fromUnit = it }, modifier = Modifier.weight(1f))
                SimpleDropdownField("To", toUnit, units, onSelect = { toUnit = it }, modifier = Modifier.weight(1f))
            }
            Text("$inputVal $fromUnit = $output $toUnit", style = MaterialTheme.typography.titleMedium, color = RoyalBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PercentageCard() {
    var total by remember { mutableStateOf("1100") }
    var obtained by remember { mutableStateOf("935") }
    val t = total.toDoubleOrNull() ?: 0.0
    val o = obtained.toDoubleOrNull() ?: 0.0
    val pct = if (t > 0) (o * 100.0 / t) else 0.0
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Percentage & Marks Calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = obtained,
                onValueChange = { obtained = it },
                label = { Text("Obtained / Part Value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = total,
                onValueChange = { total = it },
                label = { Text("Total Value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Percentage: ${"%.2f".format(pct)}%", style = MaterialTheme.typography.titleLarge, color = RoyalBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AgeAndDateCard() {
    var dob by remember { mutableStateOf("2000-08-14") }
    var ageOut by remember { mutableStateOf("") }
    var date1 by remember { mutableStateOf("2025-01-01") }
    var date2 by remember { mutableStateOf("2026-08-14") }
    var daysOut by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Age Calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        ageOut = ToolCalculators.calculateAge(dob).getOrElse { it.localizedMessage ?: "Invalid date" }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Compute Age") }
                if (ageOut.isNotBlank()) {
                    Text(ageOut, style = MaterialTheme.typography.titleSmall, color = RoyalBlue)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Date Difference Calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = date1, onValueChange = { date1 = it }, label = { Text("Start Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date2, onValueChange = { date2 = it }, label = { Text("End Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        daysOut = ToolCalculators.daysBetween(date1, date2)
                            .map { "$it days" }
                            .getOrElse { it.localizedMessage ?: "Invalid dates" }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Calculate Days") }
                if (daysOut.isNotBlank()) {
                    Text("Difference: $daysOut", style = MaterialTheme.typography.titleSmall, color = RoyalBlue)
                }
            }
        }
    }
}

@Composable
private fun NotesComposerCard(onSave: (String, String, (Result<Unit>) -> Unit) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Personal Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Note Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Write note...") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            if (errorMsg != null) Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = {
                    onSave(title, content) { res ->
                        res.onSuccess {
                            title = ""
                            content = ""
                            errorMsg = null
                        }.onFailure { errorMsg = it.localizedMessage }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("save_note_button")
            ) { Text("Save Note") }
        }
    }
}

@Composable
private fun TodoComposerCard(onAdd: (String, String, (Result<Unit>) -> Unit) -> Unit) {
    var title by remember { mutableStateOf("") }
    var reminder by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("To-Do & Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = reminder, onValueChange = { reminder = it }, label = { Text("Reminder Note (e.g., Today 5:00 PM)") }, modifier = Modifier.fillMaxWidth())
            if (errorMsg != null) Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = {
                    onAdd(title, reminder) { res ->
                        res.onSuccess {
                            title = ""
                            reminder = ""
                            errorMsg = null
                        }.onFailure { errorMsg = it.localizedMessage }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("add_todo_button")
            ) { Text("Add Task") }
        }
    }
}

@Composable
private fun PasswordGeneratorCard(onCopy: (String) -> Unit) {
    var length by remember { mutableFloatStateOf(14f) }
    var symbols by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf(ToolCalculators.generatePassword(14, true, true)) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Secure Password Generator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(password, style = MaterialTheme.typography.titleMedium, color = RoyalBlue, modifier = Modifier.weight(1f))
                IconButton(onClick = { onCopy(password) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy password")
                }
            }
            Text("Length: ${length.toInt()}")
            Slider(value = length, onValueChange = { length = it }, valueRange = 8f..28f)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Include Numbers")
                Switch(checked = digits, onCheckedChange = { digits = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Include Symbols")
                Switch(checked = symbols, onCheckedChange = { symbols = it })
            }
            Button(
                onClick = { password = ToolCalculators.generatePassword(length.toInt(), symbols, digits) },
                modifier = Modifier.fillMaxWidth().testTag("generate_password_button")
            ) { Text("Generate Password") }
        }
    }
}

@Composable
private fun QrToolsCard(
    onCopy: (String) -> Unit,
    onDecodeUri: (android.net.Uri, (Result<String>) -> Unit) -> Unit
) {
    var qrText by remember { mutableStateOf("https://apnahub.example") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(QrHelper.generateQrBitmap("https://apnahub.example")) }
    var decodedText by remember { mutableStateOf<String?>(null) }
    var decodeError by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onDecodeUri(uri) { res ->
                res.onSuccess {
                    decodedText = it
                    decodeError = null
                }.onFailure {
                    decodedText = null
                    decodeError = it.localizedMessage ?: "No QR code detected in image."
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = RoyalBlue)
                    Spacer(Modifier.size(8.dp))
                    Text("QR Code Generator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = qrText,
                    onValueChange = { qrText = it },
                    label = { Text("Text or URL to encode") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { qrBitmap = QrHelper.generateQrBitmap(qrText) },
                    modifier = Modifier.fillMaxWidth().testTag("generate_qr_button")
                ) { Text("Generate QR") }

                qrBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Generated QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = RoyalBlue)
                    Spacer(Modifier.size(8.dp))
                    Text("QR Code Image Reader", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Pick any QR screenshot or image from your gallery using Android Photo Picker — no camera or storage permission required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("pick_qr_image_button")
                ) { Text("Pick QR Image to Scan") }

                if (decodedText != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Decoded: $decodedText", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onCopy(decodedText!!) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy decoded QR text")
                        }
                    }
                }
                if (decodeError != null) {
                    Text(decodeError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
