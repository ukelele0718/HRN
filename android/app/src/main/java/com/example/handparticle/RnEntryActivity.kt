package com.example.handparticle

import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultReactActivityDelegate

class RnEntryActivity : ReactActivity() {
  override fun getMainComponentName(): String = "RnEntry"

  override fun createReactActivityDelegate(): ReactActivityDelegate =
    DefaultReactActivityDelegate(this, mainComponentName, false)
}
