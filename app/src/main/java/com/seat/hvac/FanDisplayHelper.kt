package com.seat.hvac

import android.util.Log

/**
 * 风量显示帮助类 - 专门处理风量显示的转换逻辑
 * 参考 Mconfig 应用的风量显示方法
 */
class FanDisplayHelper {

    /**
     * 获取风量显示字符串
     * 自动模式显示 A1-A5，手动模式显示 0-9
     * 不显示"风量"标签，只显示转换后的数字
     */
    fun getFanDisplayString(
        currentFanSpeed: Int,
        currentAutoFanSetting: Int,
        currentHvacAutoMode: Int,
        currentBlowingAutoMode: Int
    ): String {
        val isAutoMode = isAutoMode(currentHvacAutoMode, currentBlowingAutoMode)

        return if (isAutoMode) {
            // 自动模式显示
            getAutoFanString(currentAutoFanSetting)
        } else {
            // 手动模式显示
            getManualFanString(currentFanSpeed)
        }
    }

    /**
     * 判断当前是否为自动模式
     */
    fun isAutoMode(currentHvacAutoMode: Int, currentBlowingAutoMode: Int): Boolean {
        return currentHvacAutoMode == 1 || currentBlowingAutoMode == 1
    }

    /**
     * 获取自动风量显示字符串 (A1-A5)
     */
    private fun getAutoFanString(autoFanSetting: Int): String {
        return when (autoFanSetting) {
            IdNames.AUTO_FAN_SETTING_QUIETER -> "A1"
            IdNames.AUTO_FAN_SETTING_SILENT -> "A2"
            IdNames.AUTO_FAN_SETTING_NORMAL -> "A3"
            IdNames.AUTO_FAN_SETTING_HIGH -> "A4"
            IdNames.AUTO_FAN_SETTING_HIGHER -> "A5"
            else -> "--"
        }
    }

    /**
     * 获取手动风量显示字符串 (0-9)
     */
    private fun getManualFanString(fanSpeed: Int): String {
        return when (fanSpeed) {
            IdNames.FAN_SPEED_OFF -> "0"
            IdNames.FAN_SPEED_LEVEL_1 -> "1"
            IdNames.FAN_SPEED_LEVEL_2 -> "2"
            IdNames.FAN_SPEED_LEVEL_3 -> "3"
            IdNames.FAN_SPEED_LEVEL_4 -> "4"
            IdNames.FAN_SPEED_LEVEL_5 -> "5"
            IdNames.FAN_SPEED_LEVEL_6 -> "6"
            IdNames.FAN_SPEED_LEVEL_7 -> "7"
            IdNames.FAN_SPEED_LEVEL_8 -> "8"
            IdNames.FAN_SPEED_LEVEL_9 -> "9"
            else -> "--"
        }
    }

    /**
     * 获取风量显示颜色 - 自动模式为青色，手动模式为白色
     */
    fun getFanDisplayColor(isAutoMode: Boolean): Int {
        return if (isAutoMode) {
            // 青色 - 自动模式
            android.graphics.Color.CYAN
        } else {
            // 白色 - 手动模式
            android.graphics.Color.WHITE
        }
    }

    /**
     * 更新风量显示（可用于回调）
     */
    interface FanDisplayCallback {
        fun onFanDisplayUpdated(displayText: String, isAutoMode: Boolean)
    }
}