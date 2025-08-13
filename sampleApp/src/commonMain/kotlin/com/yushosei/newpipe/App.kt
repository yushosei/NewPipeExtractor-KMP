package com.yushosei.newpipe

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.player.playerModule
import com.yushosei.newpipe.presentation.di.presentationModules
import com.yushosei.newpipe.presentation.ui.main.MainScreen
import com.yushosei.newpipe.util.DefaultDownloaderImpl
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.compose.KoinContext
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools

@Composable
@Preview
fun App() {
    AppDI {
        MaterialTheme() {
            InitNewPipe {
                MainScreen()
            }
        }
    }
}

@Composable
fun AppDI(
    appDeclaration: KoinAppDeclaration = {},
    content: @Composable () -> Unit
) {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        KoinApplication(
            application = {
                appDeclaration()

                val baseModules = presentationModules + playerModule
                modules(baseModules)
            },
            content = content
        )
    } else {
        KoinContext {
            content()
        }
    }
}

@Composable
fun InitNewPipe(
    content: @Composable () -> Unit
) {
    var isInitialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        NewPipe.init(DefaultDownloaderImpl.initDefault())
        isInitialized = true
    }

    if (isInitialized) {
        content()
    }
}