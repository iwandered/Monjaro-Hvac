package com.seat.hvac

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.ecarx.xui.adaptapi.FunctionStatus
import com.ecarx.xui.adaptapi.car.Car
import com.ecarx.xui.adaptapi.car.base.ICarFunction
import com.ecarx.xui.adaptapi.car.sensor.ISensor
import java.util.concurrent.atomic.AtomicBoolean
import com.ecarx.xui.adaptapi.car.sensor.ISensorEvent
import android.util.Log
import com.ecarx.xui.adaptapi.car.sensor.ISensor.ISensorListener

class ClimateThreadManager(applicationContext: Context, handler: Handler?) {

    private var threadWithRunnable: Thread? = null
    private var needToStop: AtomicBoolean = AtomicBoolean(false)
    private var context = applicationContext
    private var handlerFromThread = handler

    companion object {
        var handleToThread: Handler? = null
        fun sendMessageToThread(what: Int, s: String) {
            if (handleToThread != null) {
                val msg: Message = handleToThread!!.obtainMessage()
                msg.what = what
                msg.obj = s
                handleToThread!!.sendMessage(msg)
            }
        }
    }

    private var climateThreadRunnable: ClimateThread? = null

    init {
        climateThreadRunnable = ClimateThread()
        threadWithRunnable = Thread(climateThreadRunnable)
        threadWithRunnable!!.start()
    }

    fun getClimateThread(): ClimateThread? {
        return climateThreadRunnable
    }

    fun destroy() {
        if (threadWithRunnable != null) {
            needToStop.set(true)
            threadWithRunnable!!.join()
            threadWithRunnable = null
        }
        needToStop.set(false)
    }

    inner class ClimateThread : Runnable {

        private var iCarFunction: ICarFunction? = null
        private var iCarSensors: ISensor? = null

        // 添加函数值监听器
        private var functionValueWatcher: ICarFunction.IFunctionValueWatcher? = null

        // 添加传感器监听器
        private var sensorListener: ISensorListener? = null

        // 新增：副驾驶屏幕自动控制标志
        private var autoPsdControlEnabled = false

        @Synchronized
        fun sendMessageToUI(what: Int, s: String) {
            if (handlerFromThread != null) {
                val msg: Message = handlerFromThread!!.obtainMessage()
                msg.what = what
                msg.obj = s
                handlerFromThread!!.sendMessage(msg)
            }
        }

        private fun initICarFunction(): Int {
            var ret = -1
            try {
                iCarFunction = Car.create(context).iCarFunction
                ret = 0
            } catch (e: Exception) {
                Log.e("ClimateThread", "初始化ICarFunction失败", e)
            } catch (e: Error) {
                Log.e("ClimateThread", "初始化ICarFunction错误", e)
            }
            return ret
        }

        private fun initICarSensors(): Int {
            var ret = -1
            try {
                iCarSensors = Car.create(context).sensorManager
                ret = 0
            } catch (e: Exception) {
                Log.e("ClimateThread", "初始化ICarSensors失败", e)
            } catch (e: Error) {
                Log.e("ClimateThread", "初始化ICarSensors错误", e)
            }
            return ret
        }

        // 修正：根据ISensorListener接口定义实现
        private fun initSensorListener(): Int {
            var ret = -1
            try {
                sensorListener = object : ISensorListener {
                    override fun onSensorValueChanged(sensorType: Int, value: Float) {
                        handleSensorValueChange(sensorType, value)
                    }

                    override fun onSensorEventChanged(sensorType: Int, value: Int) {
                        // 处理传感器事件
                        handleSensorEventChange(sensorType, value)
                    }

                    // 根据接口定义：第二个参数是FunctionStatus，不是Boolean
                    override fun onSensorSupportChanged(sensorType: Int, status: FunctionStatus) {
                        // 处理传感器支持状态变化
                        handleSensorSupportChange(sensorType, status)
                    }
                }

                // 注册内温和外温传感器监听
                if (iCarSensors != null) {
                    // 注册环境温度（外温）监听
                    if (isICarSensorAvailable(ISensor.SENSOR_TYPE_TEMPERATURE_AMBIENT)) {
                        iCarSensors?.registerListener(sensorListener, ISensor.SENSOR_TYPE_TEMPERATURE_AMBIENT)
                        Log.d("ClimateThread", "注册外温传感器监听成功")
                    }

                    // 注册室内温度（内温）监听
                    if (isICarSensorAvailable(ISensor.SENSOR_TYPE_TEMPERATURE_INDOOR)) {
                        iCarSensors?.registerListener(sensorListener, ISensor.SENSOR_TYPE_TEMPERATURE_INDOOR)
                        Log.d("ClimateThread", "注册内温传感器监听成功")
                    }

                    // 注册副驾驶座位占用状态监听
                    if (isICarSensorAvailable(ISensor.SENSOR_TYPE_SEAT_OCCUPATION_STATUS_PASSENGER)) {
                        iCarSensors?.registerListener(sensorListener, ISensor.SENSOR_TYPE_SEAT_OCCUPATION_STATUS_PASSENGER)
                        Log.d("ClimateThread", "注册副驾驶座位占用传感器监听成功")
                    }

                    // 注册挡位传感器监听
                    if (isICarSensorAvailable(IdNames.SENSOR_TYPE_GEAR)) {
                        iCarSensors?.registerListener(sensorListener, IdNames.SENSOR_TYPE_GEAR)
                        Log.d("ClimateThread", "注册挡位传感器监听成功")
                    }

                    ret = 0
                }
            } catch (e: Exception) {
                Log.e("ClimateThread", "初始化传感器监听器失败", e)
            }
            return ret
        }

        fun isICarFunctionAvailable(i: Int): Boolean {
            return if (this.iCarFunction != null) {
                try {
                    val status: FunctionStatus = this.iCarFunction!!.isFunctionSupported(i)
                    status == FunctionStatus.active
                } catch (e: Exception) {
                    false
                }
            } else false
        }

        fun isICarFunctionAvailable(i: Int, i1: Int): Boolean {
            return if (this.iCarFunction != null) {
                try {
                    val status: FunctionStatus = this.iCarFunction!!.isFunctionSupported(i, i1)
                    status == FunctionStatus.active
                } catch (e: Exception) {
                    false
                }
            } else false
        }

        // 新增：检查传感器是否可用
        private fun isICarSensorAvailable(sensorType: Int): Boolean {
            return if (this.iCarSensors != null) {
                try {
                    val status: FunctionStatus = iCarSensors!!.isSensorSupported(sensorType)
                    status != FunctionStatus.notavailable && status != FunctionStatus.error
                } catch (e: Exception) {
                    false
                }
            } else false
        }

        fun getFunctionValue(v: Int): Int {
            try {
                if (this.iCarFunction != null) {
                    return this.iCarFunction!!.getFunctionValue(v)
                }
            } catch (e: Exception) {
                Log.e("ClimateThread", "获取函数值失败: v=$v", e)
            }
            return -1
        }

        fun getFunctionValue(v: Int, v1: Int): Int {
            try {
                if (this.iCarFunction != null) {
                    return this.iCarFunction!!.getFunctionValue(v, v1)
                }
            } catch (e: Exception) {
                Log.e("ClimateThread", "获取函数值失败: v=$v, v1=$v1", e)
            }
            return -1
        }

        fun getFunctionValueFloat(v: Int, v1: Int): Float {
            try {
                if (this.iCarFunction != null) {
                    val value = this.iCarFunction!!.getFunctionValue(v, v1)
                    return value / 10.0f
                }
            } catch (e: Exception) {
                Log.e("ClimateThread", "获取浮点函数值失败: v=$v, v1=$v1", e)
            }
            return -1f
        }

        private fun setFunctionValue(i: Int, i2: Int) {
            try {
                if (this.iCarFunction != null) {
                    this.iCarFunction!!.setFunctionValue(i, i2)
                }
            } catch (e: Exception) {
                Log.e("ClimateThread", "设置函数值失败: i=$i, i2=$i2", e)
            }
        }

        private fun setFunctionValue(i: Int, i2: Int, i3: Int) {
            try {
                if (this.iCarFunction != null) {
                    this.iCarFunction!!.setFunctionValue(i, i2, i3)
                }
            } catch (e: Exception) {
                Log.e("ClimateThread", "设置函数值失败: i=$i, i2=$i2, i3=$i3", e)
            }
        }

        fun setFunctionValueChecked(type: Int, zone: Int, value: Int) {
            if (isICarFunctionAvailable(type, zone)) {
                setFunctionValue(type, zone, value)
            } else {
                Log.w("ClimateThread", "功能不可用: type=$type, zone=$zone")
            }
        }

        fun setFunctionValueChecked(type: Int, value: Int) {
            if (isICarFunctionAvailable(type)) {
                setFunctionValue(type, value)
            } else {
                Log.w("ClimateThread", "功能不可用: type=$type")
            }
        }

        // 新增：设置自动PSD控制状态
        fun setAutoPsdControl(enabled: Boolean) {
            this.autoPsdControlEnabled = enabled
            Log.d("ClimateThread", "设置自动PSD控制: $enabled")
        }

        // 获取自动PSD控制状态
        fun isAutoPsdControlEnabled(): Boolean {
            return this.autoPsdControlEnabled
        }

        // 新增：处理传感器数值变化
        private fun handleSensorValueChange(sensorType: Int, value: Float) {
            Log.d("ClimateThread", "传感器值变化: sensorType=$sensorType, value=$value")

            when (sensorType) {
                ISensor.SENSOR_TYPE_TEMPERATURE_AMBIENT -> {
                    // 外温变化，格式化保留一位小数（只显示数值，不显示单位）
                    val tempStr = String.format("%.1f", value)
                    sendMessageToUI(IdNames.AMBIENT_TEMPERATURE_MSG, tempStr)
                }

                ISensor.SENSOR_TYPE_TEMPERATURE_INDOOR -> {
                    // 内温变化，格式化保留一位小数（只显示数值，不显示单位）
                    val tempStr = String.format("%.1f", value)
                    sendMessageToUI(IdNames.INT_TEMPERATURE_MSG, tempStr)
                }
            }
        }

        // 新增：处理传感器事件变化
        private fun handleSensorEventChange(sensorType: Int, value: Int) {
            Log.d("ClimateThread", "传感器事件变化: sensorType=$sensorType, value=$value")

            when (sensorType) {
                ISensor.SENSOR_TYPE_SEAT_OCCUPATION_STATUS_PASSENGER -> {
                    // 副驾驶座位占用状态变化
                    val occupationStr = when (value) {
                        ISensorEvent.SEAT_OCCUPATION_STATUS_OCCUPIED -> "occupied"
                        ISensorEvent.SEAT_OCCUPATION_STATUS_NONE -> "none"
                        ISensorEvent.SEAT_OCCUPATION_STATUS_FAULT -> "fault"
                        else -> "unknown"
                    }
                    sendMessageToUI(IdNames.PASSENGER_SEAT_OCCUPATION_MSG, occupationStr)

                    // 如果启用了自动PSD控制且副驾驶无人，则关闭副驾驶屏幕
                    if (autoPsdControlEnabled && value == ISensorEvent.SEAT_OCCUPATION_STATUS_NONE) {
                        Log.d("ClimateThread", "副驾驶无人，自动关闭副驾驶屏幕")
                        setFunctionValueChecked(IdNames.FUNC_PSD_SCREEN_SWITCH.toInt(), IdNames.ZONE_ALL, 0)
                    }
                }

                IdNames.SENSOR_TYPE_GEAR -> {
                    // 挡位变化
                    Log.d("ClimateThread", "挡位变化: $value")
                    sendMessageToUI(IdNames.GEAR_MSG, value.toString())

                    // 触发倒车后视镜下翻逻辑（如果已设置）
                    handleMirrorDippingOnGearChange(value)
                }
            }
        }

        // 新增：挡位变化时触发后视镜下翻
        private fun handleMirrorDippingOnGearChange(gearValue: Int) {
            try {
                // 检查后视镜下翻功能是否可用
                if (isICarFunctionAvailable(IdNames.SETTING_FUNC_MIRROR_DIPPING)) {
                    val currentMode = getFunctionValue(IdNames.SETTING_FUNC_MIRROR_DIPPING)

                    // 当前后视镜下翻模式
                    val dippingMode = when (currentMode) {
                        IdNames.MIRROR_DIPPING_DRIVER -> "DRIVER"
                        IdNames.MIRROR_DIPPING_PASSENGER -> "PASSENGER"
                        IdNames.MIRROR_DIPPING_BOTH -> "BOTH"
                        else -> "OFF"
                    }

                    Log.d("ClimateThread", "当前挡位: $gearValue, 后视镜下翻模式: $dippingMode ($currentMode)")

                    if (gearValue == IdNames.GEAR_REVERSE) {
                        // 挂入R档（倒挡）
                        Log.d("ClimateThread", "挂入倒挡，执行后视镜下翻")

                        // 根据当前模式执行下翻（车辆系统会自动处理）
                        // 我们只需要确保模式设置正确，车辆会在倒挡时自动下翻
                        if (currentMode != IdNames.MIRROR_DIPPING_OFF) {
                            Log.d("ClimateThread", "后视镜下翻已启用，模式: $dippingMode")
                        } else {
                            Log.d("ClimateThread", "后视镜下翻已关闭")
                        }
                    } else {
                        // 退出倒挡（车辆系统会自动恢复后视镜位置）
                        Log.d("ClimateThread", "退出倒挡，后视镜应自动恢复")
                    }
                }
            } catch (e: Exception) {
                Log.e("ClimateThread", "处理后视镜下翻失败", e)
            }
        }

        // 修正：处理传感器支持状态变化（参数为FunctionStatus）
        private fun handleSensorSupportChange(sensorType: Int, status: FunctionStatus) {
            Log.d("ClimateThread", "传感器支持状态变化: sensorType=$sensorType, status=$status")

            val isSupported = status == FunctionStatus.active

            when (sensorType) {
                ISensor.SENSOR_TYPE_TEMPERATURE_AMBIENT -> {
                    if (isSupported) {
                        Log.d("ClimateThread", "外温传感器现在可用")
                    } else {
                        Log.w("ClimateThread", "外温传感器不再可用，状态: $status")
                    }
                }

                ISensor.SENSOR_TYPE_TEMPERATURE_INDOOR -> {
                    if (isSupported) {
                        Log.d("ClimateThread", "内温传感器现在可用")
                    } else {
                        Log.w("ClimateThread", "内温传感器不再可用，状态: $status")
                    }
                }

                ISensor.SENSOR_TYPE_SEAT_OCCUPATION_STATUS_PASSENGER -> {
                    if (isSupported) {
                        Log.d("ClimateThread", "副驾驶座位占用传感器现在可用")
                    } else {
                        Log.w("ClimateThread", "副驾驶座位占用传感器不再可用，状态: $status")
                    }
                }

                IdNames.SENSOR_TYPE_GEAR -> {
                    if (isSupported) {
                        Log.d("ClimateThread", "挡位传感器现在可用")
                    } else {
                        Log.w("ClimateThread", "挡位传感器不再可用，状态: $status")
                    }
                }
            }
        }

        // 新增：辅助方法 - 单独注册功能监听器
        private fun registerFunctionWatcher(functionId: Int) {
            try {
                if (iCarFunction != null && functionValueWatcher != null) {
                    iCarFunction?.registerFunctionValueWatcher(functionId, functionValueWatcher)
                    Log.d("ClimateThread", "成功注册监听器: functionId=$functionId")
                } else {
                    Log.w("ClimateThread", "无法注册监听器: iCarFunction或functionValueWatcher为空")
                }
            } catch (e: Exception) {
                Log.e("ClimateThread", "注册监听器失败: functionId=$functionId", e)
            }
        }

        // 修改方法：初始化监听器
        private fun initListeners() {
            try {
                Log.d("ClimateThread", "开始初始化监听器")

                // 创建函数值监听器
                functionValueWatcher = object : ICarFunction.IFunctionValueWatcher {
                    override fun onFunctionValueChanged(functionId: Int, zone: Int, value: Int) {
                        handleFunctionValueChange(functionId, zone, value)
                    }

                    override fun onFunctionChanged(functionId: Int) {
                        // 不需要处理
                    }

                    override fun onSupportedFunctionStatusChanged(functionId: Int, zone: Int, status: FunctionStatus?) {
                        // 不需要处理
                    }

                    override fun onSupportedFunctionValueChanged(functionId: Int, zone: IntArray?) {
                        // 不需要处理
                    }

                    override fun onCustomizeFunctionValueChanged(functionId: Int, zone: Int, value: Float) {
                        // 不需要处理
                    }
                }

                // 获取ICarFunction实例
                if (iCarFunction == null) {
                    Log.e("ClimateThread", "iCarFunction为空，无法注册监听器")
                    return
                }

                // 采用分开注册方式
                // 1. 风量相关功能
                registerFunctionWatcher(IdNames.HVAC_FUNC_FAN_SPEED)
                registerFunctionWatcher(IdNames.HVAC_FUNC_AUTO_FAN_SETTING)
                registerFunctionWatcher(IdNames.HVAC_FUNC_AUTO)

                // 2. 出风模式
                registerFunctionWatcher(IdNames.HVAC_FUNC_BLOWING_MODE)

                // 3. 循环和AC
                registerFunctionWatcher(IdNames.HVAC_FUNC_CIRCULATION)
                registerFunctionWatcher(IdNames.HVAC_FUNC_AC)

                // 4. G-Clean
                registerFunctionWatcher(IdNames.HVAC_FUNC_G_CLEAN)

                // 5. 温度设置
                registerFunctionWatcher(IdNames.HVAC_FUNC_TEMP_SET)

                // 6. 座椅功能
                registerFunctionWatcher(IdNames.HVAC_FUNC_SEAT_HEATING)
                registerFunctionWatcher(IdNames.HVAC_FUNC_SEAT_VENTILATION)

                // 7. 副驾驶屏幕功能 - 添加类型转换
                registerFunctionWatcher(IdNames.FUNC_PSD_SCREEN_SWITCH.toInt())

                // 8. 转向灯功能（可选）
                try {
                    registerFunctionWatcher(IdNames.BCM_FUNC_LIGHT_LEFT_TURN_SIGNAL)
                    registerFunctionWatcher(IdNames.BCM_FUNC_LIGHT_RIGHT_TURN_SIGNAL)
                    registerFunctionWatcher(IdNames.BCM_FUNC_LIGHT_HAZARD_FLASHERS)
                } catch (e: Exception) {
                    Log.w("ClimateThread", "注册转向灯监听器失败（可能不需要）", e)
                }

                // 9. 后视镜功能
                registerFunctionWatcher(IdNames.SETTING_FUNC_MIRROR_DIPPING)
                registerFunctionWatcher(IdNames.SETTING_FUNC_MIRROR_AUTO_FOLDING)

                Log.d("ClimateThread", "监听器初始化完成")

            } catch (e: Exception) {
                Log.e("ClimateThread", "初始化监听器失败", e)
            }
        }

        // 修改：处理函数值变化
        private fun handleFunctionValueChange(functionId: Int, zone: Int, value: Int) {
            Log.d("ClimateThread", "函数值变化: functionId=$functionId, zone=$zone, value=$value")

            when (functionId) {
                IdNames.HVAC_FUNC_FAN_SPEED -> {
                    sendMessageToUI(IdNames.HVAC_FUNC_FAN_SPEED_MSG, value.toString())
                }
                IdNames.HVAC_FUNC_AUTO_FAN_SETTING -> {
                    sendMessageToUI(IdNames.HVAC_FUNC_AUTO_FAN_SETTING_MSG, value.toString())
                }
                IdNames.HVAC_FUNC_AUTO -> {
                    sendMessageToUI(IdNames.HVAC_FUNC_AUTO_MSG, value.toString())
                }
                IdNames.HVAC_FUNC_BLOWING_MODE -> {
                    // 检查是否是主驾驶区域
                    if (zone == IdNames.SEAT_ROW_1_LEFT) {
                        sendMessageToUI(IdNames.HVAC_FUNC_BLOWING_MODE_MSG, value.toString())
                    }
                }
                IdNames.HVAC_FUNC_CIRCULATION -> {
                    sendMessageToUI(IdNames.HVAC_FUNC_CIRCULATION_MSG, value.toString())
                }
                IdNames.HVAC_FUNC_AC -> {
                    sendMessageToUI(IdNames.HVAC_FUNC_AC_MSG, value.toString())
                }
                IdNames.HVAC_FUNC_G_CLEAN -> {
                    sendMessageToUI(IdNames.HVAC_FUNC_G_CLEAN_MSG, value.toString())
                }
                IdNames.HVAC_FUNC_TEMP_SET -> {
                    when (zone) {
                        IdNames.SEAT_ROW_1_LEFT -> {
                            sendMessageToUI(IdNames.HVAC_FUNC_TEMP_LEFT, value.toString())
                        }
                        IdNames.SEAT_ROW_1_RIGHT -> {
                            // 副驾驶温度，如果有需要可以处理
                        }
                    }
                }
                IdNames.HVAC_FUNC_SEAT_HEATING -> {
                    when (zone) {
                        IdNames.SEAT_ROW_1_LEFT -> {
                            sendMessageToUI(IdNames.SEAT_HEATING_DRIVER, value.toString())
                        }
                        IdNames.SEAT_ROW_1_RIGHT -> {
                            sendMessageToUI(IdNames.SEAT_HEATING_PASSENGER, value.toString())
                        }
                    }
                }
                IdNames.HVAC_FUNC_SEAT_VENTILATION -> {
                    when (zone) {
                        IdNames.SEAT_ROW_1_LEFT -> {
                            sendMessageToUI(IdNames.SEAT_VENTILATION_DRIVER, value.toString())
                        }
                        IdNames.SEAT_ROW_1_RIGHT -> {
                            sendMessageToUI(IdNames.SEAT_VENTILATION_PASSENGER, value.toString())
                        }
                    }
                }
                IdNames.FUNC_PSD_SCREEN_SWITCH.toInt() -> {
                    // 副驾驶屏幕状态变化
                    sendMessageToUI(IdNames.PSD_STATUS_MSG, value.toString())
                }
                IdNames.BCM_FUNC_LIGHT_LEFT_TURN_SIGNAL -> {
                    sendMessageToUI(IdNames.LIGHT_LEFT_TURN, value.toString())
                }
                IdNames.BCM_FUNC_LIGHT_RIGHT_TURN_SIGNAL -> {
                    sendMessageToUI(IdNames.LIGHT_RIGHT_TURN, value.toString())
                }
                IdNames.BCM_FUNC_LIGHT_HAZARD_FLASHERS -> {
                    sendMessageToUI(IdNames.LIGHT_HAZARD_FLASHERS, value.toString())
                }
                IdNames.SETTING_FUNC_MIRROR_DIPPING -> {
                    // 后视镜下翻模式变化
                    sendMessageToUI(IdNames.MIRROR_DIPPING_MSG, value.toString())
                    Log.d("ClimateThread", "后视镜下翻模式变化: $value")
                }
                IdNames.SETTING_FUNC_MIRROR_AUTO_FOLDING -> {
                    // 后视镜自动折叠状态变化
                    sendMessageToUI(IdNames.MIRROR_FOLDING_MSG, value.toString())
                }
            }
        }

        // 修改方法：主动查询所有状态
        fun queryAllInitialStates() {
            try {
                Log.d("ClimateThread", "开始查询所有初始状态")

                // 查询传感器数据（主动查询作为初始化）
                if (isICarSensorAvailable(ISensor.SENSOR_TYPE_TEMPERATURE_INDOOR)) {
                    val indoorTemp = iCarSensors?.getSensorLatestValue(ISensor.SENSOR_TYPE_TEMPERATURE_INDOOR) ?: 0f
                    val tempStr = String.format("%.1f", indoorTemp)
                    sendMessageToUI(IdNames.INT_TEMPERATURE_MSG, tempStr)
                    Log.d("ClimateThread", "查询室内温度: value=$tempStr")
                }

                if (isICarSensorAvailable(ISensor.SENSOR_TYPE_TEMPERATURE_AMBIENT)) {
                    val ambientTemp = iCarSensors?.getSensorLatestValue(ISensor.SENSOR_TYPE_TEMPERATURE_AMBIENT) ?: 0f
                    val tempStr = String.format("%.1f", ambientTemp)
                    sendMessageToUI(IdNames.AMBIENT_TEMPERATURE_MSG, tempStr)
                    Log.d("ClimateThread", "查询环境温度: value=$tempStr")
                }

                // 检测副驾驶座位占用状态
                if (isICarSensorAvailable(ISensor.SENSOR_TYPE_SEAT_OCCUPATION_STATUS_PASSENGER)) {
                    val seatOccupation = iCarSensors?.getSensorLatestValue(ISensor.SENSOR_TYPE_SEAT_OCCUPATION_STATUS_PASSENGER) ?: -1f
                    val occupationValue = seatOccupation.toInt()
                    val occupationStr = when (occupationValue) {
                        ISensorEvent.SEAT_OCCUPATION_STATUS_OCCUPIED -> "occupied"
                        ISensorEvent.SEAT_OCCUPATION_STATUS_NONE -> "none"
                        ISensorEvent.SEAT_OCCUPATION_STATUS_FAULT -> "fault"
                        else -> "unknown"
                    }
                    sendMessageToUI(IdNames.PASSENGER_SEAT_OCCUPATION_MSG, occupationStr)
                    Log.d("ClimateThread", "查询副驾驶座位占用: value=$occupationStr")
                }

                // 查询挡位状态
                if (isICarSensorAvailable(IdNames.SENSOR_TYPE_GEAR)) {
                    val gear = iCarSensors?.getSensorLatestValue(IdNames.SENSOR_TYPE_GEAR) ?: 0f
                    val gearValue = gear.toInt()
                    sendMessageToUI(IdNames.GEAR_MSG, gearValue.toString())
                    Log.d("ClimateThread", "查询挡位: value=$gearValue")

                    // 查询后立即检查是否需要下翻
                    handleMirrorDippingOnGearChange(gearValue)
                }

                // 2. 风量相关状态
                if (isICarFunctionAvailable(IdNames.HVAC_FUNC_FAN_SPEED)) {
                    val fanSpeed = getFunctionValue(IdNames.HVAC_FUNC_FAN_SPEED)
                    sendMessageToUI(IdNames.HVAC_FUNC_FAN_SPEED_MSG, fanSpeed.toString())
                    Log.d("ClimateThread", "查询风量: value=$fanSpeed")
                }

                if (isICarFunctionAvailable(IdNames.HVAC_FUNC_AUTO_FAN_SETTING)) {
                    val autoFanSetting = getFunctionValue(IdNames.HVAC_FUNC_AUTO_FAN_SETTING)
                    sendMessageToUI(IdNames.HVAC_FUNC_AUTO_FAN_SETTING_MSG, autoFanSetting.toString())
                    Log.d("ClimateThread", "查询自动风量设置: value=$autoFanSetting")
                }

                if (isICarFunctionAvailable(IdNames.HVAC_FUNC_AUTO)) {
                    val autoMode = getFunctionValue(IdNames.HVAC_FUNC_AUTO)
                    sendMessageToUI(IdNames.HVAC_FUNC_AUTO_MSG, autoMode.toString())
                    Log.d("ClimateThread", "查询自动模式: value=$autoMode")
                }

                // 3. 出风模式
                if (isICarFunctionAvailable(IdNames.HVAC_FUNC_BLOWING_MODE, IdNames.SEAT_ROW_1_LEFT)) {
                    val blowingMode = getFunctionValue(IdNames.HVAC_FUNC_BLOWING_MODE, IdNames.SEAT_ROW_1_LEFT)
                    sendMessageToUI(IdNames.HVAC_FUNC_BLOWING_MODE_MSG, blowingMode.toString())
                    Log.d("ClimateThread", "查询出风模式: value=$blowingMode")
                }

                // 4. 循环和AC
                if (isICarFunctionAvailable(IdNames.HVAC_FUNC_CIRCULATION)) {
                    val circulation = getFunctionValue(IdNames.HVAC_FUNC_CIRCULATION)
                    sendMessageToUI(IdNames.HVAC_FUNC_CIRCULATION_MSG, circulation.toString())
                    Log.d("ClimateThread", "查询循环模式: value=$circulation")
                }

                if (isICarFunctionAvailable(IdNames.HVAC_FUNC_AC)) {
                    val acState = getFunctionValue(IdNames.HVAC_FUNC_AC)
                    sendMessageToUI(IdNames.HVAC_FUNC_AC_MSG, acState.toString())
                    Log.d("ClimateThread", "查询AC状态: value=$acState")
                }

                // 5. G-Clean
                if (isICarFunctionAvailable(IdNames.HVAC_FUNC_G_CLEAN)) {
                    val gCleanState = getFunctionValue(IdNames.HVAC_FUNC_G_CLEAN)
                    sendMessageToUI(IdNames.HVAC_FUNC_G_CLEAN_MSG, gCleanState.toString())
                    Log.d("ClimateThread", "查询G-Clean状态: value=$gCleanState")
                }

                // 6. 温度
                if (isICarFunctionAvailable(IdNames.HVAC_FUNC_TEMP_SET, IdNames.SEAT_ROW_1_LEFT)) {
                    val tempLeft = getFunctionValue(IdNames.HVAC_FUNC_TEMP_SET, IdNames.SEAT_ROW_1_LEFT)
                    sendMessageToUI(IdNames.HVAC_FUNC_TEMP_LEFT, tempLeft.toString())
                    Log.d("ClimateThread", "查询温度: value=$tempLeft")
                }

                // 7. 座椅状态
                if (isICarFunctionAvailable(IdNames.HVAC_FUNC_SEAT_HEATING, IdNames.SEAT_ROW_1_LEFT)) {
                    val driverHeating = getFunctionValue(IdNames.HVAC_FUNC_SEAT_HEATING, IdNames.SEAT_ROW_1_LEFT)
                    sendMessageToUI(IdNames.SEAT_HEATING_DRIVER, driverHeating.toString())
                    Log.d("ClimateThread", "查询驾驶员座椅加热: value=$driverHeating")
                }

                if (isICarFunctionAvailable(IdNames.HVAC_FUNC_SEAT_VENTILATION, IdNames.SEAT_ROW_1_LEFT)) {
                    val driverVent = getFunctionValue(IdNames.HVAC_FUNC_SEAT_VENTILATION, IdNames.SEAT_ROW_1_LEFT)
                    sendMessageToUI(IdNames.SEAT_VENTILATION_DRIVER, driverVent.toString())
                    Log.d("ClimateThread", "查询驾驶员座椅通风: value=$driverVent")
                }

                if (isICarFunctionAvailable(IdNames.HVAC_FUNC_SEAT_HEATING, IdNames.SEAT_ROW_1_RIGHT)) {
                    val passHeating = getFunctionValue(IdNames.HVAC_FUNC_SEAT_HEATING, IdNames.SEAT_ROW_1_RIGHT)
                    sendMessageToUI(IdNames.SEAT_HEATING_PASSENGER, passHeating.toString())
                    Log.d("ClimateThread", "查询副驾驶座椅加热: value=$passHeating")
                }

                if (isICarFunctionAvailable(IdNames.HVAC_FUNC_SEAT_VENTILATION, IdNames.SEAT_ROW_1_RIGHT)) {
                    val passVent = getFunctionValue(IdNames.HVAC_FUNC_SEAT_VENTILATION, IdNames.SEAT_ROW_1_RIGHT)
                    sendMessageToUI(IdNames.SEAT_VENTILATION_PASSENGER, passVent.toString())
                    Log.d("ClimateThread", "查询副驾驶座椅通风: value=$passVent")
                }

                // 8. 副驾驶屏幕状态 - 添加类型转换
                if (isICarFunctionAvailable(IdNames.FUNC_PSD_SCREEN_SWITCH.toInt(), IdNames.ZONE_ALL)) {
                    val psdStatus = getFunctionValue(IdNames.FUNC_PSD_SCREEN_SWITCH.toInt(), IdNames.ZONE_ALL)
                    sendMessageToUI(IdNames.PSD_STATUS_MSG, psdStatus.toString())
                    Log.d("ClimateThread", "查询副驾驶屏幕状态: value=$psdStatus")
                }

                // 9. 后视镜状态
                if (isICarFunctionAvailable(IdNames.SETTING_FUNC_MIRROR_DIPPING)) {
                    val mirrorDipping = getFunctionValue(IdNames.SETTING_FUNC_MIRROR_DIPPING)
                    sendMessageToUI(IdNames.MIRROR_DIPPING_MSG, mirrorDipping.toString())
                    Log.d("ClimateThread", "查询后视镜下翻模式: value=$mirrorDipping")
                }

                if (isICarFunctionAvailable(IdNames.SETTING_FUNC_MIRROR_AUTO_FOLDING)) {
                    val mirrorFolding = getFunctionValue(IdNames.SETTING_FUNC_MIRROR_AUTO_FOLDING)
                    sendMessageToUI(IdNames.MIRROR_FOLDING_MSG, mirrorFolding.toString())
                    Log.d("ClimateThread", "查询后视镜自动折叠: value=$mirrorFolding")
                }

                Log.d("ClimateThread", "所有初始状态查询完成")

            } catch (e: Exception) {
                Log.e("ClimateThread", "查询初始状态失败", e)
            }
        }

        override fun run() {
            var iCarFunctionInit = false
            var iCarSensorsInit = false
            var sensorListenerInit = false  // 新增：传感器监听器初始化标志

            Log.d("ClimateThread", "气候线程开始运行")

            while (!needToStop.get()) {
                if (!iCarFunctionInit) {
                    Log.d("ClimateThread", "正在初始化ICarFunction...")
                    if (initICarFunction() == 0) {
                        iCarFunctionInit = true
                        Log.d("ClimateThread", "ICarFunction初始化成功")
                        // 初始化成功后，立即设置监听器和查询状态
                        initListeners()
                        queryAllInitialStates()
                    } else {
                        Log.w("ClimateThread", "ICarFunction初始化失败，等待重试")
                    }
                }

                if (!iCarSensorsInit) {
                    Log.d("ClimateThread", "正在初始化ICarSensors...")
                    if (initICarSensors() == 0) {
                        iCarSensorsInit = true
                        Log.d("ClimateThread", "ICarSensors初始化成功")
                    } else {
                        Log.w("ClimateThread", "ICarSensors初始化失败，等待重试")
                    }
                }

                // 新增：初始化传感器监听器
                if (iCarSensorsInit && !sensorListenerInit) {
                    Log.d("ClimateThread", "正在初始化传感器监听器...")
                    if (initSensorListener() == 0) {
                        sensorListenerInit = true
                        Log.d("ClimateThread", "传感器监听器初始化成功")
                    } else {
                        Log.w("ClimateThread", "传感器监听器初始化失败，等待重试")
                    }
                }

                // 如果都初始化成功，就可以退出初始化循环
                if (iCarFunctionInit && iCarSensorsInit && sensorListenerInit) {
                    Log.d("ClimateThread", "所有初始化完成，进入监听模式")
                    break
                }

                // 等待一段时间后重试
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    break
                }
            }

            // 主循环：保持线程运行，监听状态变化
            while (!needToStop.get()) {
                try {
                    Thread.sleep(1000) // 保持线程运行，但不再轮询
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    break
                }
            }

            // 清理监听器
            cleanupListeners()
            cleanupSensorListeners()  // 新增：清理传感器监听器
            Log.d("ClimateThread", "气候线程结束运行")
        }

        private fun cleanupListeners() {
            try {
                Log.d("ClimateThread", "开始清理监听器")
                functionValueWatcher?.let { watcher ->
                    iCarFunction?.unregisterFunctionValueWatcher(watcher)
                    functionValueWatcher = null
                    Log.d("ClimateThread", "监听器清理完成")
                }
            } catch (e: Exception) {
                Log.e("ClimateThread", "清理监听器失败", e)
            }
        }

        // 新增：清理传感器监听器
        private fun cleanupSensorListeners() {
            try {
                Log.d("ClimateThread", "开始清理传感器监听器")
                sensorListener?.let { listener ->
                    iCarSensors?.unregisterListener(listener)
                    sensorListener = null
                    Log.d("ClimateThread", "传感器监听器清理完成")
                }
            } catch (e: Exception) {
                Log.e("ClimateThread", "清理传感器监听器失败", e)
            }
        }
    }
}