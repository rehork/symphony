package io.github.zyrouge.symphony

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.zyrouge.symphony.services.AppMeta
import io.github.zyrouge.symphony.services.PermissionsManager
import io.github.zyrouge.symphony.services.SettingsManager
import io.github.zyrouge.symphony.services.database.Database
import io.github.zyrouge.symphony.services.groove.GrooveManager
import io.github.zyrouge.symphony.services.i18n.Translations
import io.github.zyrouge.symphony.services.i18n.Translator
import io.github.zyrouge.symphony.services.radio.Radio
import io.github.zyrouge.symphony.utils.AndroidXShorty
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface SymphonyHooks {
    fun onSymphonyReady() {}
    fun onSymphonyPause() {}
    fun onSymphonyDestroy() {}
}

class Symphony(application: Application) : AndroidViewModel(application), SymphonyHooks {
    val shorty = AndroidXShorty(this)
    val permission = PermissionsManager(this)
    val settings = SettingsManager(this)
    val database = Database(this)
    val groove = GrooveManager(this)
    val radio = Radio(this)

    val translator = Translator(this)
    val t: Translations
        get() = translator.t

    val applicationContext: Context
        get() = getApplication<Application>().applicationContext
    private var isReady = false
    private var hooks = listOf(this, radio)

    fun ready() {
        if (isReady) return
        isReady = true
        notifyHooks { onSymphonyReady() }
    }

    fun pause() {
        notifyHooks { onSymphonyPause() }
    }

    fun destroy() {
        notifyHooks { onSymphonyDestroy() }
    }

    fun launchInScope(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ) = viewModelScope.launch(context, start, block)

    override fun onSymphonyReady() {
        checkVersion()
    }

    private fun notifyHooks(fn: SymphonyHooks.() -> Unit) {
        hooks.forEach { fn.invoke(it) }
    }

    private fun checkVersion() {
        viewModelScope.launch {
            val latestVersion = withContext(Dispatchers.IO) { AppMeta.fetchLatestVersion() }
            latestVersion?.let {
                withContext(Dispatchers.Main) {
                    if (settings.getCheckForUpdates() && AppMeta.version != it) {
                        Toast
                            .makeText(
                                applicationContext,
                                t.newVersionAvailableX(it),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
        }
    }
}
