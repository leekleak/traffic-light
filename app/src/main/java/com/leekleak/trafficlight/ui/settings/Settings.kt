package com.leekleak.trafficlight.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.CategoryTitleSmallText
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.components.preference.DialogItemSelectPreference
import com.leekleak.core.ui.components.preference.NavigatePreference
import com.leekleak.core.ui.components.preference.NavigatePreferenceIcon
import com.leekleak.core.ui.components.preference.SwitchPreference
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.core.ui.theme.Theme
import com.leekleak.core.ui.theme.card
import com.leekleak.core.ui.theme.googleSans
import com.leekleak.core.util.openLink
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.ui.navigation.LibraryLicenseScreen

@Composable
fun Settings(
    navigator: Navigator,
    uiState: SettingsUiState,
    setTheme: (Theme) -> Unit,
    setBlur: (Boolean) -> Unit,
    setSpeedMetric: (Boolean) -> Unit,
    setSizeMetric: (Boolean) -> Unit,
) {
    HazeScaffold(
        title = stringResource(R.string.settings),
        verticalArrangement = Arrangement.Top,
        backAction = BackAction.Normal(navigator),
    ) {
        BasesSettings(uiState, setSpeedMetric, setSizeMetric)
        InterfaceSettings(uiState, setTheme, setBlur)
        AboutSettings(navigator)
    }
}

@Composable
private fun AboutSettings(navigator: Navigator) {
    val activity = LocalActivity.current

    CategoryTitleSmallText(stringResource(R.string.about))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NavigatePreference(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.donate),
            icon = R.drawable.donate,
            onClick = { openLink(activity, "https://github.com/sponsors/leekleak") },
            showControl = false
        )
        NavigatePreferenceIcon(
            icon = painterResource(R.drawable.github),
            contentDescription = stringResource(R.string.github),
            onClick = { openLink(activity, "https://github.com/leekleak/traffic-light") },
        )
        NavigatePreferenceIcon(
            icon = painterResource(R.drawable.gavel),
            contentDescription = stringResource(R.string.licenses),
            onClick = { navigator.goTo(LibraryLicenseScreen) },
        )
    }

    val fontFamily = remember { googleSans(weight = 600f, roundness = 100f) }
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.6f)
            .padding(vertical = 4.dp),
        fontFamily = fontFamily,
        text = stringResource(R.string.version_short, BuildConfig.VERSION_NAME),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun InterfaceSettings(
    uiState: SettingsUiState,
    setTheme: (Theme) -> Unit,
    setBlur: (Boolean) -> Unit,
) {
    CategoryTitleSmallText(stringResource(R.string.ui))
    val scroll = rememberScrollState(0)

    val spacing = 8.dp
    val spacingPx = with(LocalDensity.current) { spacing.roundToPx() }
    var panelWidth by remember { mutableIntStateOf(0) }
    var aligned by remember { mutableStateOf(false) }

    LaunchedEffect(panelWidth) {
        if (panelWidth == 0) return@LaunchedEffect
        scroll.scrollTo(if (uiState.theme.isMaterial) 0 else panelWidth + spacingPx)
        aligned = true
    }
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .card()
            .horizontalScroll(scroll)
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        ThemePreferenceContainer(
            currentTheme = uiState.theme,
            material = true,
            modifier = Modifier.onSizeChanged { panelWidth = it.width },
            onThemeChanged = setTheme,
        )
        ThemePreferenceContainer(uiState.theme, false, onThemeChanged = setTheme)
    }
    SwitchPreference(
        title = stringResource(R.string.blur_effects),
        icon = R.drawable.blur_on,
        value = uiState.blur,
        onValueChanged = setBlur,
    )
}

@Composable
private fun BasesSettings(
    uiState: SettingsUiState,
    setSpeedMetric: (Boolean) -> Unit,
    setSizeMetric: (Boolean) -> Unit,
) {
    CategoryTitleSmallText(stringResource(R.string.sizes))
    val values = listOf(
        true to stringResource(R.string.base_1000),
        false to stringResource(R.string.base_1024)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DialogItemSelectPreference(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.speed_unit),
            icon = R.drawable.network_check,
            value = uiState.speedMetric,
            values = values,
            onValueChanged = setSpeedMetric,
        )
        DialogItemSelectPreference(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.size_unit),
            icon = R.drawable.database,
            value = uiState.sizeMetric,
            values = values,
            onValueChanged = setSizeMetric,
        )
    }
}
