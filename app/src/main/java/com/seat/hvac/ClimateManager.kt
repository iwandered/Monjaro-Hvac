package com.seat.hvac

import android.util.Log

class ClimateManager(private val climateThread: ClimateThreadManager.ClimateThread) {

    // 保存关闭前的状态（去掉电源状态保存）
    private var savedFanSpeed: Int = -1
    private var savedAutoFanSetting: Int = -1
    private var savedBlowingMode: Int = -1
    private var savedHvacAutoMode: Int = -1
    private var savedCirculationMode: Int = -1
    private var savedACState: Int = -1
    private var savedGCleanState: Int = -1
    private var savedTemperature: Float = 22.0f
    private var isSaved: Boolean = false

    /**
     * 设置出风模式
     */
    fun setBlowingMode(mode: Int) {
        climateThread.setFunctionValueChecked(IdNames.HVAC_FUNC_BLOWING_MODE, IdNames.SEAT_ROW_1_LEFT, mode)
        Log.d("ClimateManager", "设置出风模式: mode=$mode")
    }

    /**
     * 设置AUTO模式
     */
    fun setAuto(enable: Boolean) {
        val valInt = if (enable) 1 else 0
        climateThread.setFunctionValueChecked(IdNames.HVAC_FUNC_AUTO, valInt)
        Log.d("ClimateManager", "设置AUTO模式: enable=$enable")
    }

    /**
     * 设置AC开关
     */
    fun setAc(enable: Boolean) {
        val valInt = if (enable) 1 else 0
        climateThread.setFunctionValueChecked(IdNames.HVAC_FUNC_AC, valInt)
        Log.d("ClimateManager", "设置AC开关: enable=$enable")
    }

    /**
     * 设置内外循环
     * inner=true: 设置为内循环 (CIRCULATION_INNER)
     * inner=false: 设置为外循环 (CIRCULATION_OFF)
     */
    fun setCirculation(inner: Boolean) {
        val valInt = if (inner) IdNames.CIRCULATION_INNER else IdNames.CIRCULATION_OFF
        climateThread.setFunctionValueChecked(IdNames.HVAC_FUNC_CIRCULATION, valInt)
        Log.d("ClimateManager", "设置循环模式: ${if (inner) "内循环" else "外循环(OFF)"}")
    }

    /**
     * 设置G-Clean（净化）开关
     */
    fun setGClean(enable: Boolean) {
        val valInt = if (enable) IdNames.G_CLEAN_ON else IdNames.G_CLEAN_OFF
        climateThread.setFunctionValueChecked(IdNames.HVAC_FUNC_G_CLEAN, valInt)
        Log.d("ClimateManager", "设置G-Clean: enable=$enable")
    }

    /**
     * OFF按钮功能：只关闭AC、AUTO、净化，不关闭空调电源
     * 按照新的需求：关闭AC，关闭AUTO，关闭净化
     */
    fun turnOff() {
        if (!isSaved) {
            savedFanSpeed = climateThread.getFunctionValue(IdNames.HVAC_FUNC_FAN_SPEED)
            savedAutoFanSetting = climateThread.getFunctionValue(IdNames.HVAC_FUNC_AUTO_FAN_SETTING)
            savedBlowingMode = climateThread.getFunctionValue(IdNames.HVAC_FUNC_BLOWING_MODE, IdNames.SEAT_ROW_1_LEFT)
            savedHvacAutoMode = climateThread.getFunctionValue(IdNames.HVAC_FUNC_AUTO)
            savedCirculationMode = climateThread.getFunctionValue(IdNames.HVAC_FUNC_CIRCULATION)
            savedACState = climateThread.getFunctionValue(IdNames.HVAC_FUNC_AC)
            savedGCleanState = climateThread.getFunctionValue(IdNames.HVAC_FUNC_G_CLEAN)
            savedTemperature = climateThread.getFunctionValueFloat(IdNames.HVAC_FUNC_TEMP_SET, IdNames.SEAT_ROW_1_LEFT)

            isSaved = true

            Log.d("ClimateManager", "保存状态: fan=$savedFanSpeed, blow=$savedBlowingMode, auto=$savedHvacAutoMode")
        }

        try {
            setAc(false)
            Log.d("ClimateManager", "OFF: 关闭AC")
            setAuto(false)
            Log.d("ClimateManager", "OFF: 关闭AUTO模式")
            setGClean(false)
            Log.d("ClimateManager", "OFF: 关闭净化")
        } catch (e: Exception) {
            Log.e("ClimateManager", "OFF操作失败", e)
        }
    }

    /**
     * ON按钮功能：恢复之前保存的状态（不需要打开电源）
     * 修改：当没有保存状态时，直接从车辆系统读取当前状态，而不是使用默认值
     */
    fun turnOn() {
        if (!isSaved) {
            // 直接从车辆系统读取当前真实状态，而不是使用硬编码默认值
            Log.d("ClimateManager", "没有保存的状态，直接从车辆系统读取当前状态")
            val currentBlowingMode = loadCurrentStateFromVehicle()
            Log.d("ClimateManager", "从车辆系统读取当前状态完成: 风向=$currentBlowingMode")

            // 重要：这里只返回，不执行恢复操作，因为我们要保持当前车辆状态
            return
        }

        Log.d("ClimateManager", "恢复状态: fan=$savedFanSpeed, blow=$savedBlowingMode, auto=$savedHvacAutoMode")

        try {
            val tempInt = (savedTemperature * 10).toInt()
            climateThread.setFunctionValueChecked(IdNames.HVAC_FUNC_TEMP_SET, IdNames.SEAT_ROW_1_LEFT, tempInt)
            Log.d("ClimateManager", "ON: 恢复温度设置: $savedTemperature")

            setAc(savedACState == 1)
            Log.d("ClimateManager", "ON: 恢复AC状态: ${savedACState == 1}")

            setAuto(savedHvacAutoMode == 1)
            Log.d("ClimateManager", "ON: 恢复AUTO模式: ${savedHvacAutoMode == 1}")

            val isInner = savedCirculationMode == IdNames.CIRCULATION_INNER
            setCirculation(isInner)
            Log.d("ClimateManager", "ON: 恢复循环模式: ${if (isInner) "内循环" else "外循环"}")

            setBlowingMode(savedBlowingMode)
            Log.d("ClimateManager", "ON: 恢复出风模式: $savedBlowingMode")

            if (savedHvacAutoMode == 1) {
                val autoLevel = mapAutoSettingToLevel(savedAutoFanSetting)
                val setting = when (autoLevel) {
                    1 -> IdNames.AUTO_FAN_SETTING_QUIETER
                    2 -> IdNames.AUTO_FAN_SETTING_SILENT
                    3 -> IdNames.AUTO_FAN_SETTING_NORMAL
                    4 -> IdNames.AUTO_FAN_SETTING_HIGH
                    5 -> IdNames.AUTO_FAN_SETTING_HIGHER
                    else -> IdNames.AUTO_FAN_SETTING_NORMAL
                }
                climateThread.setFunctionValueChecked(IdNames.HVAC_FUNC_AUTO_FAN_SETTING, setting)
                Log.d("ClimateManager", "ON: 恢复自动风量: 级别$autoLevel")
            } else {
                val manualLevel = mapSpeedToLevel(savedFanSpeed)
                val mappedSpeed = when(manualLevel) {
                    0 -> IdNames.FAN_SPEED_OFF
                    1 -> IdNames.FAN_SPEED_LEVEL_1
                    2 -> IdNames.FAN_SPEED_LEVEL_2
                    3 -> IdNames.FAN_SPEED_LEVEL_3
                    4 -> IdNames.FAN_SPEED_LEVEL_4
                    5 -> IdNames.FAN_SPEED_LEVEL_5
                    6 -> IdNames.FAN_SPEED_LEVEL_6
                    7 -> IdNames.FAN_SPEED_LEVEL_7
                    8 -> IdNames.FAN_SPEED_LEVEL_8
                    9 -> IdNames.FAN_SPEED_LEVEL_9
                    else -> IdNames.FAN_SPEED_OFF
                }
                climateThread.setFunctionValueChecked(IdNames.HVAC_FUNC_FAN_SPEED, mappedSpeed)
                Log.d("ClimateManager", "ON: 恢复手动风量: 级别$manualLevel")
            }

            setGClean(savedGCleanState == IdNames.G_CLEAN_ON)
            Log.d("ClimateManager", "ON: 恢复G-Clean状态: ${savedGCleanState == IdNames.G_CLEAN_ON}")

            isSaved = false
            Log.d("ClimateManager", "ON: 恢复所有状态完成")

        } catch (e: Exception) {
            Log.e("ClimateManager", "ON操作失败", e)
        }
    }

    /**
     * 从车辆系统读取当前真实状态（新增方法）
     * @return 当前风向值，用于调试信息
     */
    private fun loadCurrentStateFromVehicle(): Int {
        try {
            Log.d("ClimateManager", "开始从车辆系统读取当前真实状态")

            val currentFanSpeed = climateThread.getFunctionValue(IdNames.HVAC_FUNC_FAN_SPEED)
            val currentAutoFanSetting = climateThread.getFunctionValue(IdNames.HVAC_FUNC_AUTO_FAN_SETTING)
            val currentBlowingMode = climateThread.getFunctionValue(
                IdNames.HVAC_FUNC_BLOWING_MODE,
                IdNames.SEAT_ROW_1_LEFT
            )
            val currentHvacAutoMode = climateThread.getFunctionValue(IdNames.HVAC_FUNC_AUTO)
            val currentCirculationMode = climateThread.getFunctionValue(IdNames.HVAC_FUNC_CIRCULATION)
            val currentACState = climateThread.getFunctionValue(IdNames.HVAC_FUNC_AC)
            val currentGCleanState = climateThread.getFunctionValue(IdNames.HVAC_FUNC_G_CLEAN)
            val currentTemperature = climateThread.getFunctionValueFloat(
                IdNames.HVAC_FUNC_TEMP_SET,
                IdNames.SEAT_ROW_1_LEFT
            )

            Log.d("ClimateManager", "从车辆读取状态: 风量=$currentFanSpeed, 风向=$currentBlowingMode, " +
                    "自动模式=$currentHvacAutoMode, 循环=$currentCirculationMode, " +
                    "AC=$currentACState, 净化=$currentGCleanState, 温度=$currentTemperature")

            return currentBlowingMode

        } catch (e: Exception) {
            Log.e("ClimateManager", "从车辆读取状态失败", e)
            return -1
        }
    }

    /**
     * 刷新当前状态（新增方法）
     * 直接从车辆系统读取并更新显示，不保存状态
     */
    fun refreshCurrentState() {
        try {
            Log.d("ClimateManager", "开始刷新当前状态")

            val currentFanSpeed = climateThread.getFunctionValue(IdNames.HVAC_FUNC_FAN_SPEED)
            val currentBlowingMode = climateThread.getFunctionValue(
                IdNames.HVAC_FUNC_BLOWING_MODE,
                IdNames.SEAT_ROW_1_LEFT
            )
            val currentACState = climateThread.getFunctionValue(IdNames.HVAC_FUNC_AC)
            val currentAutoMode = climateThread.getFunctionValue(IdNames.HVAC_FUNC_AUTO)
            val currentCirculation = climateThread.getFunctionValue(IdNames.HVAC_FUNC_CIRCULATION)
            val currentGClean = climateThread.getFunctionValue(IdNames.HVAC_FUNC_G_CLEAN)

            Log.d("ClimateManager", "刷新状态: 风量=$currentFanSpeed, 风向=$currentBlowingMode, " +
                    "AC=$currentACState, AUTO=$currentAutoMode, 循环=$currentCirculation, 净化=$currentGClean")

        } catch (e: Exception) {
            Log.e("ClimateManager", "刷新状态失败", e)
        }
    }

    /**
     * 将自动风量常量转换为档位级别
     */
    private fun mapAutoSettingToLevel(setting: Int): Int {
        return when (setting) {
            IdNames.AUTO_FAN_SETTING_QUIETER -> 1
            IdNames.AUTO_FAN_SETTING_SILENT -> 2
            IdNames.AUTO_FAN_SETTING_NORMAL -> 3
            IdNames.AUTO_FAN_SETTING_HIGH -> 4
            IdNames.AUTO_FAN_SETTING_HIGHER -> 5
            else -> 3
        }
    }

    /**
     * 将风量常量转换为档位级别
     */
    private fun mapSpeedToLevel(speed: Int): Int {
        return when (speed) {
            IdNames.FAN_SPEED_OFF -> 0
            IdNames.FAN_SPEED_LEVEL_1 -> 1
            IdNames.FAN_SPEED_LEVEL_2 -> 2
            IdNames.FAN_SPEED_LEVEL_3 -> 3
            IdNames.FAN_SPEED_LEVEL_4 -> 4
            IdNames.FAN_SPEED_LEVEL_5 -> 5
            IdNames.FAN_SPEED_LEVEL_6 -> 6
            IdNames.FAN_SPEED_LEVEL_7 -> 7
            IdNames.FAN_SPEED_LEVEL_8 -> 8
            IdNames.FAN_SPEED_LEVEL_9 -> 9
            else -> 0
        }
    }
}