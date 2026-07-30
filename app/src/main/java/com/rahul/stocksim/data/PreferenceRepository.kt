package com.rahul.stocksim.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("tradr_prefs", Context.MODE_PRIVATE)

    var rebrandNoticeShown: Boolean
        get() = prefs.getBoolean("rebrand_notice_shown", false)
        set(value) = prefs.edit().putBoolean("rebrand_notice_shown", value).apply()
}
