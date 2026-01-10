package com.seat.hvac

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * FixSetManager - 管理More菜单中的设置功能
 * 包含灯光控制、自动灯光开关、强制关闭PSD、后视镜下翻等功能
 */
class FixSetManager(
    private val context: Context,
    private var climateThread: ClimateThreadManager.ClimateThread? = null
) {
    companion object {
        private const val TAG = "FixSetManager"
        private const val PREFS_NAME = "hvac_fix_settings"

        // SharedPreferences键
        const val KEY_AUTO_PSD_ENABLED = "auto_psd_enabled"
        const val KEY_FORCE_PSD_OFF = "force_psd_off"
        const val KEY_AUTO_BENDING_LIGHT = "auto_bending_light"
        const val KEY_AUTO_COURTESY_LIGHT = "auto_courtesy_light"
        const val KEY_AUTO_CORNERING_LIGHT = "auto_cornering_light"
        // 新增：后视镜下翻模式保存键
        const val KEY_MIRROR_DIPPING_MODE = "mirror_dipping_mode"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 设置ClimateThread（用于后续初始化）
     */
    fun setClimateThread(thread: ClimateThreadManager.ClimateThread?) {
        this.climateThread = thread
        Log.d(TAG, "ClimateThread 已设置: ${thread != null}")
    }

    // ============== 状态缓存 ==============
    private var bendingLightStatus: Boolean = false
    private var courtesyLightStatus: Boolean = false
    private var corneringLightStatus: Boolean = false

    // 后视镜下翻模式缓存（从SharedPreferences加载）
    private var mirrorDippingMode: Int = prefs.getInt(KEY_MIRROR_DIPPING_MODE, IdNames.MIRROR_DIPPING_BOTH)

    // ============== 灯光直接控制 ==============

    /**
     * 设置转向辅助灯开关
     */
    fun setBendingLight(enable: Boolean) {
        climateThread?.setFunctionValueChecked(
            IdNames.SETTING_FUNC_LAMP_BENDINGLIGHT,
            if (enable) IdNames.LIGHT_ON else IdNames.LIGHT_OFF
        )
        bendingLightStatus = enable
        Log.d(TAG, "设置转向辅助灯: ${if (enable) "开" else "关"}")
    }

    /**
     * 切换转向辅助灯状态
     */
    fun toggleBendingLight() {
        setBendingLight(!bendingLightStatus)
    }

    /**
     * 获取转向辅助灯状态
     */
    fun getBendingLightStatus(): Boolean = bendingLightStatus

    /**
     * 设置迎宾灯开关
     */
    fun setCourtesyLight(enable: Boolean) {
        climateThread?.setFunctionValueChecked(
            IdNames.SETTING_FUNC_LAMP_COURTESY_LIGHT,
            if (enable) IdNames.LIGHT_ON else IdNames.LIGHT_OFF
        )
        courtesyLightStatus = enable
        Log.d(TAG, "设置迎宾灯: ${if (enable) "开" else "关"}")
    }

    /**
     * 切换迎宾灯状态
     */
    fun toggleCourtesyLight() {
        setCourtesyLight(!courtesyLightStatus)
    }

    /**
     * 获取迎宾灯状态
     */
    fun getCourtesyLightStatus(): Boolean = courtesyLightStatus

    /**
     * 设置转弯灯开关
     */
    fun setCorneringLight(enable: Boolean) {
        climateThread?.setFunctionValueChecked(
            IdNames.SETTING_FUNC_LAMP_CORNERING_LIGHT,
            if (enable) IdNames.LIGHT_ON else IdNames.LIGHT_OFF
        )
        corneringLightStatus = enable
        Log.d(TAG, "设置转弯灯: ${if (enable) "开" else "关"}")
    }

    /**
     * 切换转弯灯状态
     */
    fun toggleCorneringLight() {
        setCorneringLight(!corneringLightStatus)
    }

    /**
     * 获取转弯灯状态
     */
    fun getCorneringLightStatus(): Boolean = corneringLightStatus

    /**
     * 更新灯光状态缓存（从车辆系统回调更新）
     */
    fun updateLightStatus(lightType: Int, isOn: Boolean) {
        when (lightType) {
            IdNames.LAMP_BENDING_MSG -> bendingLightStatus = isOn
            IdNames.LAMP_COURTESY_MSG -> courtesyLightStatus = isOn
            IdNames.LAMP_CORNERING_MSG -> corneringLightStatus = isOn
        }
    }

    // ============== 自动灯光设置（持久化） ==============

    /**
     * 设置转弯时自动开启转向辅助灯
     */
    fun setAutoBendingLight(enable: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BENDING_LIGHT, enable).apply()
        Log.d(TAG, "自动转向辅助灯: ${if (enable) "开" else "关"}")
    }

    fun isAutoBendingLightEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_BENDING_LIGHT, false)

    /**
     * 设置转弯时自动开启迎宾灯
     */
    fun setAutoCourtesyLight(enable: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COURTESY_LIGHT, enable).apply()
        Log.d(TAG, "自动迎宾灯: ${if (enable) "开" else "关"}")
    }

    fun isAutoCourtesyLightEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_COURTESY_LIGHT, false)

    /**
     * 设置转弯时自动开启转弯灯
     */
    fun setAutoCorneringLight(enable: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CORNERING_LIGHT, enable).apply()
        Log.d(TAG, "自动转弯灯: ${if (enable) "开" else "关"}")
    }

    fun isAutoCorneringLightEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_CORNERING_LIGHT, false)

    // ============== PSD控制设置（持久化） ==============

    /**
     * 设置自动PSD控制
     */
    fun setAutoPsdEnabled(enable: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PSD_ENABLED, enable).apply()
        Log.d(TAG, "自动PSD控制: ${if (enable) "开" else "关"}")
    }

    fun isAutoPsdEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_PSD_ENABLED, false)

    /**
     * 设置强制关闭副驾驶屏幕（无论有人无人）
     */
    fun setForcePsdOff(enable: Boolean) {
        prefs.edit().putBoolean(KEY_FORCE_PSD_OFF, enable).apply()
        Log.d(TAG, "强制关闭PSD: ${if (enable) "开" else "关"}")
    }

    fun isForcePsdOffEnabled(): Boolean = prefs.getBoolean(KEY_FORCE_PSD_OFF, false)

    // ============== 后视镜下翻控制 ==============

    /**
     * 设置后视镜下翻模式
     * @param mode 下翻模式：OFF(0), DRIVER(537461505), PASSENGER(537461506), BOTH(537461507)
     */
    fun setMirrorDippingMode(mode: Int) {
        try {
            climateThread?.setFunctionValueChecked(
                IdNames.SETTING_FUNC_MIRROR_DIPPING,
                mode
            )
            mirrorDippingMode = mode

            // 保存到SharedPreferences
            prefs.edit().putInt(KEY_MIRROR_DIPPING_MODE, mode).apply()

            Log.d(TAG, "设置后视镜下翻模式: $mode (${getMirrorDippingModeText()})")
        } catch (e: Exception) {
            Log.e(TAG, "设置后视镜下翻模式失败", e)
        }
    }

    /**
     * 获取当前后视镜下翻模式
     */
    fun getMirrorDippingMode(): Int = mirrorDippingMode

    /**
     * 更新后视镜下翻模式缓存（从车辆系统回调更新）
     */
    fun updateMirrorDippingMode(mode: Int) {
        mirrorDippingMode = mode
        prefs.edit().putInt(KEY_MIRROR_DIPPING_MODE, mode).apply()
        Log.d(TAG, "更新后视镜下翻模式: $mode")
    }

    /**
     * 获取后视镜下翻模式的显示文本
     */
    fun getMirrorDippingModeText(): String {
        return when (mirrorDippingMode) {
            IdNames.MIRROR_DIPPING_OFF -> "关闭"
            IdNames.MIRROR_DIPPING_DRIVER -> "仅驾驶侧"
            IdNames.MIRROR_DIPPING_PASSENGER -> "仅副驾侧"
            IdNames.MIRROR_DIPPING_BOTH -> "双侧"
            else -> "未知"
        }
    }

    /**
     * 切换到下一个后视镜下翻模式
     */
    fun cycleMirrorDippingMode() {
        val newMode = when (mirrorDippingMode) {
            IdNames.MIRROR_DIPPING_OFF -> IdNames.MIRROR_DIPPING_DRIVER
            IdNames.MIRROR_DIPPING_DRIVER -> IdNames.MIRROR_DIPPING_PASSENGER
            IdNames.MIRROR_DIPPING_PASSENGER -> IdNames.MIRROR_DIPPING_BOTH
            IdNames.MIRROR_DIPPING_BOTH -> IdNames.MIRROR_DIPPING_OFF
            else -> IdNames.MIRROR_DIPPING_BOTH
        }
        setMirrorDippingMode(newMode)
    }

    /**
     * 处理挡位变化，触发后视镜下翻
     * @param gearValue 挡位值：PARK(1), REVERSE(2), NEUTRAL(4), DRIVE(8)
     */
    fun handleGearChange(gearValue: Int) {
        Log.d(TAG, "挡位变化: $gearValue, 当前后视镜下翻模式: ${getMirrorDippingModeText()}")

        if (gearValue == IdNames.GEAR_REVERSE) {
            // 挂入倒挡
            Log.d(TAG, "挂入倒挡(R)，应执行后视镜下翻")

            // 检查后视镜下翻是否启用
            if (mirrorDippingMode != IdNames.MIRROR_DIPPING_OFF) {
                Log.d(TAG, "后视镜下翻已启用，模式: ${getMirrorDippingModeText()}")
                // 注意：车辆系统会自动根据设置的模式执行下翻，我们不需要额外操作
            } else {
                Log.d(TAG, "后视镜下翻已关闭，不执行下翻")
            }
        } else {
            // 退出倒挡
            Log.d(TAG, "退出倒挡，后视镜应自动恢复")
        }
    }
}