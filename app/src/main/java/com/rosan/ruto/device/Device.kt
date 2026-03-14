package com.rosan.ruto.device

import com.rosan.ruto.service.IActivityManager
import com.rosan.ruto.service.IDisplayManager
import com.rosan.ruto.service.IPackageManager

interface Device : AutoCloseable {
    val displayManager: IDisplayManager
    val activityManager: IActivityManager
    val packageManager: IPackageManager
}
