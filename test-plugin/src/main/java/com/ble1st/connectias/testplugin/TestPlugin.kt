package com.ble1st.connectias.testplugin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.BitmapFactory
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.Fragment
import com.ble1st.connectias.plugin.sdk.IPlugin
import com.ble1st.connectias.plugin.sdk.PluginCategory
import com.ble1st.connectias.plugin.sdk.PluginContext
import com.ble1st.connectias.plugin.sdk.PluginMetadata
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Hardware Bridge APIs (v2.0) - No direct hardware imports needed

/**
 * Test-Plugin für das Connectias Plugin-System
 * 
 * Implementiert sowohl IPlugin als auch Fragment
 * Das Plugin-System erwartet, dass die Klasse über fragmentClassName geladen wird
 * und IPlugin implementiert
 */
class TestPlugin : Fragment(), IPlugin {
    
    private var pluginContext: PluginContext? = null
    private var clickCount by mutableStateOf(0)
    private var isPluginEnabled by mutableStateOf(false)
    
    // HTTP request state
    private var urlInput by mutableStateOf("https://httpbin.org/get")
    private var httpResult by mutableStateOf<String?>(null)
    private var isLoading by mutableStateOf(false)
    private var httpError by mutableStateOf<String?>(null)
    
    // Camera state
    private var cameraImageData by mutableStateOf<ByteArray?>(null)
    private var cameraError by mutableStateOf<String?>(null)
    
    // Camera Preview state
    private var isPreviewActive by mutableStateOf(false)
    private var previewInfo by mutableStateOf("")
    private var previewError by mutableStateOf("")
    
    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // IPlugin implementation
    override fun onLoad(context: PluginContext): Boolean {
        Timber.i("TestPlugin: onLoad called")
        this.pluginContext = context
        context.logVerbose("TestPlugin: Verbose log - detailed initialization info")
        context.logInfo("TestPlugin loaded successfully")
        context.logDebug("TestPlugin: Debug info - context initialized")
        return true
    }
    
    override fun onEnable(): Boolean {
        Timber.i("TestPlugin: onEnable called")
        isPluginEnabled = true
        pluginContext?.logInfo("TestPlugin enabled")
        pluginContext?.logDebug("TestPlugin: State changed to enabled")
        return true
    }
    
    override fun onDisable(): Boolean {
        Timber.i("TestPlugin: onDisable called")
        isPluginEnabled = false
        pluginContext?.logWarning("TestPlugin disabled - features may not be available")
        pluginContext?.logDebug("TestPlugin: State changed to disabled")
        return true
    }
    
    override fun onUnload(): Boolean {
        Timber.i("TestPlugin: onUnload called")
        pluginContext?.logInfo("TestPlugin unloaded")
        pluginContext?.logDebug("TestPlugin: Cleanup completed")
        return true
    }
    
    override fun getMetadata(): PluginMetadata {
        return PluginMetadata(
            pluginId = "com.ble1st.connectias.testplugin",
            pluginName = "Test Plugin",
            version = "1.0.0",
            author = "Connectias Team",
            minApiLevel = 33,
            maxApiLevel = 36,
            minAppVersion = "1.0.0",
            nativeLibraries = emptyList(),
            fragmentClassName = "com.ble1st.connectias.testplugin.TestPlugin",
            description = "Ein Test-Plugin für das Connectias Plugin-System mit Compose UI",
            permissions = listOf("android.permission.INTERNET", "android.permission.CAMERA"),
            category = PluginCategory.UTILITY,
            dependencies = emptyList()
        )
    }
    
    // Fragment implementation
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("TestPlugin: onCreateView called")
        pluginContext?.logDebug("TestPlugin: Creating Compose view")
        
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    TestPluginScreen(
                        clickCount = clickCount,
                        isPluginEnabled = isPluginEnabled,
                        onIncrementClick = { 
                            clickCount++
                            pluginContext?.logDebug("Counter incremented to $clickCount")
                        },
                        onResetClick = { 
                            pluginContext?.logInfo("Counter reset from $clickCount to 0")
                            clickCount = 0 
                        },
                        onCrashClick = {
                            pluginContext?.logWarning("TestPlugin: RuntimeException crash triggered")
                            throw RuntimeException("TestPlugin intentional crash - testing plugin isolation")
                        },
                        onNullPointerCrash = {
                            pluginContext?.logWarning("TestPlugin: NullPointerException crash triggered")
                            val nullObject: String? = null
                            nullObject!!.length // Force NPE
                        },
                        onOutOfMemoryCrash = {
                            pluginContext?.logWarning("TestPlugin: OutOfMemoryError crash triggered")
                            val list = mutableListOf<ByteArray>()
                            while (true) {
                                list.add(ByteArray(10 * 1024 * 1024)) // Allocate 10MB chunks
                            }
                        },
                        onIllegalStateCrash = {
                            pluginContext?.logWarning("TestPlugin: IllegalStateException crash triggered")
                            throw IllegalStateException("TestPlugin: Invalid state - testing isolation")
                        },
                        onStackOverflowCrash = {
                            pluginContext?.logWarning("TestPlugin: StackOverflowError crash triggered")
                            crashWithStackOverflow()
                        },
                        onIndexOutOfBoundsCrash = {
                            pluginContext?.logWarning("TestPlugin: IndexOutOfBoundsException crash triggered")
                            val array = arrayOf(1, 2, 3)
                            val value = array[10] // Access invalid index
                        },
                        onClassCastCrash = {
                            pluginContext?.logWarning("TestPlugin: ClassCastException crash triggered")
                            val obj: Any = "This is a string"
                            val number = obj as Int // Force ClassCastException
                        },
                        onArithmeticCrash = {
                            pluginContext?.logWarning("TestPlugin: ArithmeticException crash triggered")
                            val result = 10 / 0 // Division by zero
                        },
                        urlInput = urlInput,
                        onUrlInputChange = { urlInput = it },
                        onCurlClick = { performHttpRequest() },
                        httpResult = httpResult,
                        isLoading = isLoading,
                        httpError = httpError,
                        cameraImageData = cameraImageData,
                        cameraError = cameraError,
                        onCaptureImage = { captureImageViaBridge() },
                        isPreviewActive = isPreviewActive,
                        previewInfo = previewInfo,
                        previewError = previewError,
                        onStartPreview = { startCameraPreview() },
                        onStopPreview = { stopCameraPreview() }
                    )
                }
            }
        }
    }
    
    /**
     * Performs HTTP GET request via Hardware Bridge (v2.0)
     * Uses PluginContext.httpRequest() instead of direct OkHttp
     */
    private fun performHttpRequest() {
        if (urlInput.isBlank()) {
            httpError = "URL darf nicht leer sein"
            httpResult = null
            pluginContext?.logWarning("TestPlugin: HTTP request failed - empty URL")
            return
        }
        
        val context = pluginContext
        if (context == null) {
            httpError = "Plugin context not available"
            return
        }
        
        // Validate URL format
        val url = try {
            if (!urlInput.startsWith("http://") && !urlInput.startsWith("https://")) {
                "https://$urlInput"
            } else {
                urlInput
            }
        } catch (e: Exception) {
            httpError = "Ungültige URL: ${e.message}"
            httpResult = null
            context.logError("TestPlugin: Invalid URL format", e)
            return
        }
        
        isLoading = true
        httpError = null
        httpResult = null
        context.logInfo("TestPlugin: Starting HTTP request via Hardware Bridge to $url")
        
        coroutineScope.launch {
            try {
                // Use Hardware Bridge API (v2.0)
                val result = context.httpRequest(url = url, method = "GET")
                
                result.onSuccess { responseBody ->
                    httpResult = buildString {
                        appendLine("=== Hardware Bridge Response ===")
                        appendLine("URL: $url")
                        appendLine("\nResponse Body:")
                        appendLine(responseBody)
                    }
                    httpError = null
                    context.logInfo("TestPlugin: HTTP request successful via Hardware Bridge")
                }.onFailure { error ->
                    val errorMessage = "Fehler: ${error.message ?: error.javaClass.simpleName}"
                    httpError = errorMessage
                    httpResult = null
                    context.logError("TestPlugin: HTTP request failed", error)
                }
                
            } catch (e: Exception) {
                val errorMessage = "Fehler: ${e.message ?: e.javaClass.simpleName}"
                httpError = errorMessage
                httpResult = null
                context.logError("TestPlugin: HTTP request failed", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Start camera preview via Hardware Bridge (v2.0)
     */
    private fun startCameraPreview() {
        lifecycleScope.launch {
            isPreviewActive = true
            previewError = ""
            previewInfo = ""
            
            val context = pluginContext
            if (context == null) {
                previewError = "Plugin context not available"
                isPreviewActive = false
                return@launch
            }
            
            context.logInfo("TestPlugin: Starting camera preview via Hardware Bridge")
            
            val result = context.startCameraPreview()
            
            result.onSuccess { preview ->
                previewInfo = buildString {
                    appendLine("Preview Active")
                    appendLine("Resolution: ${preview.width}x${preview.height}")
                    appendLine("Format: ${preview.format}")
                    appendLine("Frame Size: ${preview.frameSize} bytes")
                    appendLine("Buffer Size: ${preview.bufferSize} bytes")
                    appendLine("FD: ${preview.fileDescriptor}")
                }
                context.logInfo("TestPlugin: Camera preview started (${preview.width}x${preview.height})")
            }.onFailure { error ->
                previewError = error.message ?: "Unknown error"
                isPreviewActive = false
                context.logError("TestPlugin: Camera preview failed", error)
            }
        }
    }
    
    /**
     * Stop camera preview via Hardware Bridge (v2.0)
     */
    private fun stopCameraPreview() {
        lifecycleScope.launch {
            val context = pluginContext
            if (context == null) {
                previewError = "Plugin context not available"
                return@launch
            }
            
            context.logInfo("TestPlugin: Stopping camera preview")
            
            val result = context.stopCameraPreview()
            
            result.onSuccess {
                isPreviewActive = false
                previewInfo = ""
                context.logInfo("TestPlugin: Camera preview stopped")
            }.onFailure { error ->
                previewError = error.message ?: "Unknown error"
                context.logError("TestPlugin: Stop preview failed", error)
            }
        }
    }
    
    /**
     * Captures image via Hardware Bridge (v2.0)
     */
    private fun captureImageViaBridge() {
        val context = pluginContext
        if (context == null) {
            cameraError = "Plugin context not available"
            return
        }
        
        context.logInfo("TestPlugin: Starting image capture via Hardware Bridge")
        cameraError = null
        
        coroutineScope.launch {
            try {
                val result = context.captureImage()
                
                result.onSuccess { imageData ->
                    cameraImageData = imageData
                    cameraError = null
                    context.logInfo("TestPlugin: Image captured successfully (${imageData.size} bytes)")
                }.onFailure { error ->
                    cameraError = error.message ?: "Unknown error"
                    cameraImageData = null
                    context.logError("TestPlugin: Image capture failed", error)
                }
            } catch (e: Exception) {
                cameraError = e.message ?: "Unknown error"
                cameraImageData = null
                context.logError("TestPlugin: Image capture failed", e)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("TestPlugin: View created successfully")
        pluginContext?.logInfo("TestPlugin UI view created and ready (Hardware Bridge v2.0)")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("TestPlugin: onDestroy called")
        pluginContext?.logDebug("TestPlugin: Fragment destroyed")
    }
    
    // Override onPause to resolve conflict between Fragment and IPlugin
    override fun onPause() {
        super<Fragment>.onPause()
        // IPlugin.onPause() is called automatically via default implementation
        pluginContext?.logDebug("TestPlugin: onPause called")
    }
    
    // Override onResume to resolve conflict between Fragment and IPlugin
    override fun onResume() {
        super<Fragment>.onResume()
        // IPlugin.onResume() is called automatically via default implementation
        pluginContext?.logDebug("TestPlugin: onResume called")
    }
    
    // Helper function for StackOverflowError
    private fun crashWithStackOverflow(depth: Int = 0) {
        if (depth > 10000) {
            // This should never be reached, but prevents infinite recursion in some cases
            return
        }
        crashWithStackOverflow(depth + 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestPluginScreen(
    clickCount: Int,
    isPluginEnabled: Boolean,
    onIncrementClick: () -> Unit,
    onResetClick: () -> Unit,
    onCrashClick: () -> Unit,
    onNullPointerCrash: () -> Unit,
    onOutOfMemoryCrash: () -> Unit,
    onIllegalStateCrash: () -> Unit,
    onStackOverflowCrash: () -> Unit,
    onIndexOutOfBoundsCrash: () -> Unit,
    onClassCastCrash: () -> Unit,
    onArithmeticCrash: () -> Unit,
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    onCurlClick: () -> Unit,
    httpResult: String?,
    isLoading: Boolean,
    httpError: String?,
    cameraImageData: ByteArray?,
    cameraError: String?,
    onCaptureImage: () -> Unit,
    isPreviewActive: Boolean,
    previewInfo: String,
    previewError: String,
    onStartPreview: () -> Unit,
    onStopPreview: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Plugin") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPluginEnabled) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isPluginEnabled) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (isPluginEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPluginEnabled) "Plugin Aktiv" else "Plugin Geladen",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Plugin-Informationen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    InfoRow(
                        icon = Icons.Default.Build,
                        label = "Plugin ID",
                        value = "com.ble1st.connectias.testplugin"
                    )
                    
                    InfoRow(
                        icon = Icons.Default.Person,
                        label = "Autor",
                        value = "Connectias Team"
                    )
                    
                    InfoRow(
                        icon = Icons.Default.Settings,
                        label = "Kategorie",
                        value = "UTILITY"
                    )
                }
            }

            // Counter Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Interaktiver Counter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "$clickCount",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onIncrementClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Erhöhen")
                        }
                        
                        OutlinedButton(
                            onClick = onResetClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset")
                        }
                    }
                    
                }
            }

            // Crash Tests Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Crash Tests (Isolation)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Text(
                        text = "Teste die Isolation des Plugin-Systems",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Row 1: RuntimeException, NullPointerException
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCrashClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("RuntimeException", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Button(
                            onClick = onNullPointerCrash,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("NullPointer", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    // Row 2: OutOfMemoryError, IllegalStateException
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOutOfMemoryCrash,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("OutOfMemory", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Button(
                            onClick = onIllegalStateCrash,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("IllegalState", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    // Row 3: StackOverflowError, IndexOutOfBoundsException
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStackOverflowCrash,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("StackOverflow", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Button(
                            onClick = onIndexOutOfBoundsCrash,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("IndexOutOfBounds", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    // Row 4: ClassCastException, ArithmeticException
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onClassCastCrash,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("ClassCast", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Button(
                            onClick = onArithmeticCrash,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Arithmetic", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // HTTP Request Card (Curl-like)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "HTTP Request (Curl)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = onUrlInputChange,
                        label = { Text("URL") },
                        placeholder = { Text("https://example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null
                            )
                        }
                    )
                    
                    Button(
                        onClick = onCurlClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && urlInput.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lädt...")
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Anfrage senden")
                        }
                    }
                    
                    // Error display
                    httpError?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    
                    // Result display
                    httpResult?.let { result ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Antwort",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                // Scrollable result text
                                val scrollState = rememberScrollState()
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = result,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .verticalScroll(scrollState),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Camera Preview Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Kamera-Vorschau",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onCaptureImage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Capture",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture Image via Bridge")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Camera Preview Section
                    Text(
                        text = "Camera Preview (Live)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartPreview,
                            enabled = !isPreviewActive,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start Preview")
                        }
                        
                        Button(
                            onClick = onStopPreview,
                            enabled = isPreviewActive,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Stop Preview")
                        }
                    }
                    
                    if (previewInfo.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = previewInfo,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    if (previewError.isNotEmpty()) {
                        Text(
                            text = "Preview Error: $previewError",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    // Camera result display
                    if (cameraImageData != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Bild erfasst: ${cameraImageData.size} bytes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                // Display captured image
                                val bitmap = remember(cameraImageData) {
                                    try {
                                        BitmapFactory.decodeByteArray(cameraImageData, 0, cameraImageData.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                
                                if (bitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Captured Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text(
                                        text = "${bitmap.width}x${bitmap.height} px",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                } else {
                                    Text(
                                        text = "Fehler beim Dekodieren des Bildes",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    
                    if (cameraError != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Fehler:",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = cameraError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Features Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Plugin-Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    FeatureItem("✅ Jetpack Compose UI")
                    FeatureItem("✅ Material 3 Design")
                    FeatureItem("✅ Fragment-basiert")
                    FeatureItem("✅ Lifecycle-Management")
                    FeatureItem("✅ Timber Logging")
                    FeatureItem("✅ Database Logging")
                    FeatureItem("✅ Standalone APK")
                    FeatureItem("✅ HTTP Requests (Curl)")
                    FeatureItem("✅ Kamera-Vorschau (Live)")
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// CameraX Preview removed - Hardware Bridge (v2.0) handles camera via IPC
