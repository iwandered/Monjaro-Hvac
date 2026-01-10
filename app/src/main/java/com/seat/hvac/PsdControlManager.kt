package com.seat.hvac

import android.util.Log

class PsdControlManager(private val climateThread: ClimateThreadManager.ClimateThread) {

    companion object {
        private const val TAG = "PsdControlManager"
    }

    /**
     * 切换副驾驶屏幕开关状态
     * 当屏幕开启时关闭，关闭时开启
     */
    fun togglePsdScreen() {
        try {
            // 先获取当前状态
            val currentStatus = climateThread.getFunctionValue(
                IdNames.FUNC_PSD_SCREEN_SWITCH.toInt(),
                IdNames.ZONE_ALL
            )

            Log.d(TAG, "当前副驾驶屏幕状态: $currentStatus")

            // 根据当前状态设置相反的状态
            val newStatus = if (currentStatus == 1) {
                0  // 如果当前是开启，则关闭
            } else {
                1  // 如果当前是关闭，则开启
            }

            // 设置新状态
            climateThread.setFunctionValueChecked(
                IdNames.FUNC_PSD_SCREEN_SWITCH.toInt(),
                IdNames.ZONE_ALL,
                newStatus
            )

            Log.d(TAG, "设置副驾驶屏幕状态: $newStatus")

        } catch (e: Exception) {
            Log.e(TAG, "切换副驾驶屏幕状态失败", e)
        }
    }

    /**
     * 开启副驾驶屏幕
     */
    fun turnOnPsdScreen() {
        try {
            climateThread.setFunctionValueChecked(
                IdNames.FUNC_PSD_SCREEN_SWITCH.toInt(),
                IdNames.ZONE_ALL,
                1
            )
            Log.d(TAG, "开启副驾驶屏幕")
        } catch (e: Exception) {
            Log.e(TAG, "开启副驾驶屏幕失败", e)
        }
    }

    /**
     * 关闭副驾驶屏幕
     */
    fun turnOffPsdScreen() {
        try {
            climateThread.setFunctionValueChecked(
                IdNames.FUNC_PSD_SCREEN_SWITCH.toInt(),
                IdNames.ZONE_ALL,
                0
            )
            Log.d(TAG, "关闭副驾驶屏幕")
        } catch (e: Exception) {
            Log.e(TAG, "关闭副驾驶屏幕失败", e)
        }
    }

    /**
     * 获取当前副驾驶屏幕状态
     * @return 1=开启, 0=关闭, -1=获取失败
     */
    fun getPsdStatus(): Int {
        return try {
            val status = climateThread.getFunctionValue(
                IdNames.FUNC_PSD_SCREEN_SWITCH.toInt(),
                IdNames.ZONE_ALL
            )
            status
        } catch (e: Exception) {
            Log.e(TAG, "获取副驾驶屏幕状态失败", e)
            -1
        }
    }

    /**
     * 新增：将雨刮器调整到服务位置（维修位置）
     * 无论欢迎按钮是否显示，点击欢迎按钮时都调用此方法
     */
    fun setWiperServicePosition() {
        try {
            Log.d(TAG, "开始设置雨刮器到服务位置")

            // 设置雨刮器到服务位置（值1表示到服务位置）
            climateThread.setFunctionValueChecked(
                IdNames.FUNC_WIPER_SERVICE_POSITION,
                1
            )

            Log.d(TAG, "已发送雨刮器服务位置指令")
        } catch (e: Exception) {
            Log.e(TAG, "设置雨刮器服务位置失败", e)
        }
    }
}