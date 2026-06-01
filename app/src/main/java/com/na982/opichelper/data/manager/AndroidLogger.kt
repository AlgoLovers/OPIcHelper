package com.na982.opichelper.data.manager

import android.util.Log
import com.na982.opichelper.domain.manager.AppLogger
import javax.inject.Inject

class AndroidLogger @Inject constructor() : AppLogger {
    override fun e(tag: String, msg: String, t: Throwable?) {
        if (t != null) Log.e(tag, msg, t) else Log.e(tag, msg)
    }

    override fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }
}
