package com.qiscus.mychatui.util

import android.os.CountDownTimer
import java.util.concurrent.TimeUnit

/**
 * Created on 6/14/21 by Pengkuh Dwi Septiandi (@pengdst)
 *
 * - Github https://github.com/pengdst
 * - Gitlab https://gitlab.com/pengdst
 * - LinkedIn https://linkedin.com/in/pengdst
 */
abstract class UnitCountDownTimer(timer: Long, unit: TimeUnit, interval: Long = 1) : CountDownTimer(
    TimeUnit.MILLISECONDS.convert(timer, unit),
    TimeUnit.MILLISECONDS.convert(interval, unit)
) {

    override fun onTick(millisUntilFinished: Long) {
        onUnitTick(TimeUnit.SECONDS.convert(millisUntilFinished, TimeUnit.MILLISECONDS))
    }

    override fun onFinish() {
        this.cancel()
    }

    abstract fun onUnitTick(secondsUntilFinished: Long, unit: TimeUnit = TimeUnit.SECONDS)

}