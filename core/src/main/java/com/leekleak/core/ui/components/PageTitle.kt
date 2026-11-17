package com.leekleak.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.core.R
import com.leekleak.core.ui.theme.LocalBlurEnabled
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.materials.HazeMaterials

@Composable
fun PageTitle(
    backAction: BackAction = BackAction.None,
    hazeState: HazeState? = null,
    text: String?,
    customElement: @Composable (RowScope.() -> Unit)? = null,
){
    val blurEnabled = LocalBlurEnabled.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    hazeState == null -> Modifier
                    blurEnabled -> Modifier.hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = HazeMaterials.ultraThin().then {
                            progressive(HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f))
                        }
                    )
                    else -> Modifier.background(MaterialTheme.colorScheme.surface)
                }
            )
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row (modifier = Modifier.height(TOP_BAR_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
                if (backAction is BackAction.Normal) {
                    IconButton(onClick = { backAction.navigator.goBack() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.go_back),
                        )
                    }
                }
                text?.let {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis,
                        text = text
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            customElement?.let { it() }
        }
    }
}

val TOP_BAR_HEIGHT: Dp = 52.dp
