package com.leekleak.trafficlight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.integrations.PlayServicesProvider
import com.leekleak.trafficlight.model.DataPlanManager
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.services.notifications.NotificationService
import com.leekleak.trafficlight.ui.app.App
import com.leekleak.trafficlight.ui.theme.AppThemeWrapper
import com.leekleak.trafficlight.widget.installWidgetPreviews
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.mp.KoinPlatform

class MainActivity : ComponentActivity() {

    private val appPreferenceRepo: AppPreferenceRepo by inject()
    private val dataPlanManager: DataPlanManager by inject()
    private val playServicesProvider: PlayServicesProvider? by KoinPlatform.getKoin().injectOrNull()
    private val imageLoader: ImageLoader by inject()
    private val permissionManager: PermissionManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionManager.update()
        enableEdgeToEdge()
        installWidgetPreviews(applicationContext, lifecycleScope)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataPlanManager.activePlansWithNotifications
                    .combine(appPreferenceRepo.notification) { _, _ -> }
                    .collect { NotificationService.startService(applicationContext) }
            }
        }

        lifecycleScope.launch {
            playServicesProvider?.onAppLaunch(this@MainActivity)
        }

        setContent {
            setSingletonImageLoaderFactory { imageLoader }

            AppThemeWrapper(appPreferenceRepo) {
                App(permissionManager = permissionManager)
            }
        }
    }
}
