package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.components.ApiKeyConfigDialog
import com.example.ui.components.SubscriptionPaywallDialog
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReceiptDetailScreen
import com.example.ui.screens.ScanReceiptScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.theme.TaxSnapTheme
import com.example.ui.viewmodel.TaxSnapViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TaxSnapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TaxSnapTheme(darkTheme = true) {
                TaxSnapApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TaxSnapApp(viewModel: TaxSnapViewModel) {
    val activeNavTab by viewModel.activeNavTab.collectAsState()
    val selectedReceipt by viewModel.selectedReceiptForDetail.collectAsState()
    val showPaywall by viewModel.showPaywallModal.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMsg) {
        toastMsg?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        AnimatedContent(
            targetState = Pair(activeNavTab, selectedReceipt),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition",
            modifier = Modifier.padding(paddingValues)
        ) { (tab, receipt) ->
            when {
                receipt != null -> {
                    ReceiptDetailScreen(
                        receipt = receipt,
                        viewModel = viewModel,
                        onBack = { viewModel.selectedReceiptForDetail.value = null }
                    )
                }
                tab == "scan" -> {
                    ScanReceiptScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.activeNavTab.value = "home" }
                    )
                }
                tab == "export" -> {
                    ExportScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.activeNavTab.value = "home" }
                    )
                }
                tab == "subscription" -> {
                    SubscriptionScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.activeNavTab.value = "home" }
                    )
                }
                else -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onReceiptClick = { clickedReceipt ->
                            viewModel.selectedReceiptForDetail.value = clickedReceipt
                        },
                        onScanClick = {
                            viewModel.onScanReceiptClicked()
                        },
                        onExportClick = {
                            viewModel.activeNavTab.value = "export"
                        },
                        onSubscriptionClick = {
                            viewModel.activeNavTab.value = "subscription"
                        }
                    )
                }
            }
        }

        // Modals & Dialogs
        if (showPaywall) {
            SubscriptionPaywallDialog(
                subscriptionManager = viewModel.subscriptionManager,
                onDismiss = { viewModel.showPaywallModal.value = false },
                onSuccess = { msg ->
                    viewModel.toastMessage.value = msg
                }
            )
        }

        if (showApiKeyDialog) {
            ApiKeyConfigDialog(
                geminiService = viewModel.geminiService,
                subscriptionManager = viewModel.subscriptionManager,
                onDismiss = { viewModel.showApiKeyDialog.value = false }
            )
        }
    }
}
