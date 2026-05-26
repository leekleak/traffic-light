package com.leekleak.trafficlight.integrations

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesView
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.ui.theme.card
import org.koin.compose.koinInject
import timber.log.Timber

val LocalNativeAdView = compositionLocalOf<NativeAdView?> { null }

@Composable
fun Ad(
    adType: AdType,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val adsEnabled by appPreferenceRepo.ads.collectAsState(false)

    if (!adsEnabled) return

    val adUnitId = when(adType) {
        AdType.NativeBanner -> BuildConfig.ADMOB_UNIT_ID_OVERVIEW
    }
    var nativeAdState by remember { mutableStateOf<NativeAd?>(null) }
    var adLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(adUnitId) {
        Timber.d("Loading ad: $adUnitId")
        adLoaded = false

        val adRequest = NativeAdRequest.Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE))
            .setAdChoicesPlacement(AdChoicesPlacement.TOP_RIGHT)
            .build()

        NativeAdLoader.load(adRequest, object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                Timber.d("Ad loaded successfully")
                nativeAdState = nativeAd
                adLoaded = true
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Timber.e("Ad failed to load: ${adError.message} (code ${adError.code})")
                adLoaded = false
            }
        })
    }

    DisposableEffect(nativeAdState) {
        val adToDestroy = nativeAdState
        onDispose {
            adToDestroy?.destroy()
        }
    }

    AnimatedVisibility (
        visible = adLoaded,
        enter = fadeIn(tween()) + slideInVertically() + expandVertically(),
        exit = fadeOut(tween()) + slideOutVertically() + shrinkVertically()
    ) {
        nativeAdState?.let { CallNativeAd(it, backgroundColor) }
    }
}

@Composable
fun CallNativeAd(
    nativeAd: NativeAd,
    backgroundColor: Color
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .background(backgroundColor),
        factory = { ctx ->
            NativeAdView(ctx).apply {
                val composeView = ComposeView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                addView(composeView)

                val adChoicesView = AdChoicesView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = android.view.Gravity.TOP or android.view.Gravity.END
                        val margin = (4 * ctx.resources.displayMetrics.density).toInt()
                        topMargin = margin
                        rightMargin = margin
                    }
                }
                addView(adChoicesView)
                this.adChoicesView = adChoicesView
            }
        },
        update = { adView ->
            val composeView = adView.getChildAt(0) as ComposeView
            composeView.setContent {
                CompositionLocalProvider(LocalNativeAdView provides adView) {
                    NativeAdLayout(nativeAd)
                }
            }
        }
    )
}

@Composable
fun NativeAdLayout(nativeAd: NativeAd) {
    val adView = LocalNativeAdView.current
    
    Row(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AndroidView(
                factory = { context -> 
                    MediaView(context).apply {
                        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                },
                update = { mediaView ->
                    if (mediaView.tag != nativeAd) {
                        adView?.registerNativeAd(nativeAd, mediaView)
                        mediaView.tag = nativeAd
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Text(
                text = stringResource(R.string.ad),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                modifier = Modifier
                    .padding(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .height(120.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                AndroidView(
                    factory = { ctx ->
                        ComposeView(ctx).apply {
                            adView?.headlineView = this
                        }
                    },
                    update = { composeView ->
                        composeView.setContent {
                            Text(
                                text = nativeAd.headline ?: "",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(end = 20.dp)
                )

                AndroidView(
                    factory = { ctx ->
                        ComposeView(ctx).apply {
                            adView?.bodyView = this
                        }
                    },
                    update = { composeView ->
                        composeView.setContent {
                            Text(
                                text = nativeAd.body ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                nativeAd.icon?.let { icon ->
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                adView?.iconView = this
                            }
                        },
                        update = { imageView ->
                            imageView.setImageDrawable(icon.drawable)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                AndroidView(
                    factory = { ctx ->
                        ComposeView(ctx).apply {
                            adView?.callToActionView = this
                        }
                    },
                    update = { composeView ->
                        composeView.setContent {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = nativeAd.callToAction ?: "",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .height(36.dp)
                        .weight(1f)
                )
            }
        }
    }
}
