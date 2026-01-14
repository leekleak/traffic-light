package com.leekleak.trafficlight.model

import android.content.Context
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options

data class AppIcon(val packageName: String)

class AppIconFetcher(
    private val data: AppIcon,
    private val context: Context
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val drawable = context.packageManager.getApplicationIcon(data.packageName)

        return ImageFetchResult(
            image = drawable.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<AppIcon> {
        override fun create(data: AppIcon, options: Options, imageLoader: ImageLoader): Fetcher {
            return AppIconFetcher(data, context)
        }
    }
}