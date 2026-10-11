package com.leekleak.trafficlight.ui.iperf

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.ui.navigation.IperfLogScreenKey
import com.leekleak.trafficlight.ui.navigation.Navigator
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ServerScreen(
    navigator: Navigator,
    myIp: String?,
    viewModel: IperfServerVM = koinViewModel(),
) {
    val context = LocalContext.current
    val data by viewModel.data.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (myIp != null) {
            Button(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.TopCenter),
                shape = MaterialTheme.shapes.medium,
                onClick = {},
                contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 4.dp)
            ) {
                Column {
                    Text(stringResource(R.string.server_address), fontWeight = FontWeight.Bold)
                    Text("$myIp:5201")
                }
            }
        }

        PlayButton(
            modifier = Modifier.padding(32.dp).align(Alignment.Center),
            iPerf3Provider = viewModel.iPerf3Provider,
            onStartTest = { viewModel.startTest(context) },
            onStopTest = { viewModel.stopTest() }
        )

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = data.isNotEmpty(),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            FilledTonalButton(
                onClick = { navigator.goTo(IperfLogScreenKey(data, isServer = true)) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.text_snippet), null)
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = stringResource(R.string.logs)
                    )
                }
            }
        }
    }
}
