package com.google.android.material.color

import android.content.Context
import android.os.Build

/** Adapter for the pinned Material 1.11 resource loader; never changes surface resources. */
object TextoColorResources {
    fun apply(context: Context, colors: Map<Int, Int>): Boolean =
        Build.VERSION.SDK_INT >= 30 && ResourcesLoaderUtils.addResourcesLoaderToContext(context, colors)
}
