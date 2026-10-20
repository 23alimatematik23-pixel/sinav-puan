package com.sinavpuan

import android.app.Application
import android.content.Context
import com.sinavpuan.data.AppDao
import com.sinavpuan.data.AppDb

class App : Application() {
    val db: AppDb by lazy { AppDb.build(this) }
}

val Context.dao: AppDao
    get() = (applicationContext as App).db.dao()
