package com.seat.hvac

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import androidx.fragment.app.Fragment

class ControlFragment : Fragment() {

    // 空调系统状态
    private var currentFanSpeed: Int = -1
    private var currentAutoFanSetting: Int = -1
    private var currentBlowingMode: Int = -1
    private var currentHvacAutoMode: Int = -1
    private var currentBlowingAutoMode: Int = -1
    private var currentCirculationMode: Int = -1
    private var currentACState: Int = -1
    private var currentGCleanState: Int = -1

    // 自动模式常量
    private companion object {
        private const val AUTO_MODE_ON = 1
        private const val AUTO_MODE_OFF = 0
    }

    // 座椅状态
    private var driverHeatLevel = 0
    private var driverVentLevel = 0
    private var passHeatLevel = 0
    private var passVentLevel = 0

    // 温度缓存
    private var lastInsideTemp: Float = 0f
    private var lastOutsideTemp: Float = 0f

    // 副驾驶座位占用状态
    private var isPassengerPresent = false

    // 副驾驶屏幕状态
    private var psdStatus = -1  // -1=未知, 0=关闭, 1=开启

    // 自动PSD控制状态
    private var autoPsdControlEnabled = false
    // 强制关闭PSD状态（无论有人无人）
    private var forcePsdOff = false

    // UI元素引用
    private var txtFanLevel: TextView? = null
    private var txtInsideTempValue: TextView? = null
    private var txtOutsideTempValue: TextView? = null
    private var txtWelcome: Button? = null  // 修改为Button类型
    private var btnAcToggle: Button? = null
    private var btnSeat: Button? = null
    private var btnAutoToggle: Button? = null
    private var btnCirculation: Button? = null
    private var btnPurification: Button? = null
    private var btnModeFace: Button? = null
    private var btnModeLeg: Button? = null
    private var btnModeWindow: Button? = null
    private var btnOnlyFace: Button? = null
    private var btnOnlyLeg: Button? = null
    private var btnOnlyWindow: Button? = null
    private var btnOff: Button? = null
    private var btnHome: Button? = null
    private var btnHvac: Button? = null

    // 新增：副驾驶屏幕控制按钮
    private var btnPsdToggle: Button? = null
    private var btnAutoPsdControl: Button? = null
    private var btnMore: Button? = null

    // 主驾驶按钮
    private var btnDrHeatOff: Button? = null
    private var btnDrHeat1: Button? = null
    private var btnDrHeat2: Button? = null
    private var btnDrHeat3: Button? = null
    private var btnDrVentOff: Button? = null
    private var btnDrVent1: Button? = null
    private var btnDrVent2: Button? = null
    private var btnDrVent3: Button? = null

    // 副驾驶按钮
    private var btnPassHeatOff: Button? = null
    private var btnPassHeat1: Button? = null
    private var btnPassHeat2: Button? = null
    private var btnPassHeat3: Button? = null
    private var btnPassVentOff: Button? = null
    private var btnPassVent1: Button? = null
    private var btnPassVent2: Button? = null
    private var btnPassVent3: Button? = null

    // Manager实例
    private lateinit var seatManager: SeatManager
    private lateinit var climateManager: ClimateManager
    private lateinit var fanDisplayHelper: FanDisplayHelper
    private lateinit var climateThreadManager: ClimateThreadManager
    private lateinit var psdControlManager: PsdControlManager  // 新增：副驾驶屏幕控制管理器
    private lateinit var fixSetManager: FixSetManager  // 新增：设置管理器
    private var moreSettingsController: MoreSettingsController? = null  // 新增：More按钮控制器

    // Handler用于接收ClimateThread的状态更新
    val uiHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            handleStatusMessage(msg)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 在最开始就初始化FixSetManager，不依赖ClimateThread
        context?.let {
            if (!::fixSetManager.isInitialized) {
                fixSetManager = FixSetManager(it)
            }
        }

        initUIReferences(view)
        setupSeatControls(view)
        setupACControls(view)
        setupModeControls(view)
        setupQuickModeControls(view)
        setupPsdControls(view)  // 新增：设置副驾驶屏幕控制
        setupWelcomeButtonClick(view)  // 新增：设置欢迎按钮点击事件
        setupMoreButton(view)  // 新增：设置More按钮

        // 加载保存的设置
        loadSavedSettings()

        updateOffButtonText()
    }

    override fun onResume() {
        super.onResume()
        Log.d("ControlFragment", "ControlFragment恢复")

        Handler(Looper.getMainLooper()).postDelayed({
            refreshCurrentState()
        }, 500)
    }

    override fun onPause() {
        super.onPause()
        Log.d("ControlFragment", "ControlFragment暂停")
    }

    fun setManagers(seat: SeatManager, climate: ClimateManager) {
        this.seatManager = seat
        this.climateManager = climate
        this.fanDisplayHelper = FanDisplayHelper()
    }

    fun setClimateThreadManager(manager: ClimateThreadManager) {
        this.climateThreadManager = manager
        // 初始化PsdControlManager
        this.psdControlManager = PsdControlManager(manager.getClimateThread()!!)
        // 将ClimateThread传递给FixSetManager
        if (::fixSetManager.isInitialized) {
            fixSetManager.setClimateThread(manager.getClimateThread())
        }
        // 初始化MoreSettingsController
        context?.let {
            if (::fixSetManager.isInitialized && ::psdControlManager.isInitialized) {
                moreSettingsController = MoreSettingsController(
                    it,
                    fixSetManager,
                    psdControlManager
                ) { refreshPsdStatus() }
                // 应用启动时检查并执行强制关闭PSD
                moreSettingsController?.applyForcePsdOffIfEnabled()
                forcePsdOff = moreSettingsController?.forcePsdOff ?: false
            }
        }
    }

    private fun initUIReferences(view: View) {
        txtFanLevel = view.findViewById(R.id.txtFanLevel)
        txtInsideTempValue = view.findViewById(R.id.txtInsideTempValue)
        txtOutsideTempValue = view.findViewById(R.id.txtOutsideTempValue)
        txtWelcome = view.findViewById(R.id.txtWelcome)  // 注意：现在是Button类型

        btnAcToggle = view.findViewById(R.id.btnAcToggle)
        btnSeat = view.findViewById(R.id.btnSeat)
        btnAutoToggle = view.findViewById(R.id.btnAutoToggle)
        btnCirculation = view.findViewById(R.id.btnCirculation)
        btnPurification = view.findViewById(R.id.btnPurification)
        btnModeFace = view.findViewById(R.id.btnModeFace)
        btnModeLeg = view.findViewById(R.id.btnModeLeg)
        btnModeWindow = view.findViewById(R.id.btnModeWindow)
        btnOnlyFace = view.findViewById(R.id.btnOnlyFace)
        btnOnlyLeg = view.findViewById(R.id.btnOnlyLeg)
        btnOnlyWindow = view.findViewById(R.id.btnOnlyWindow)
        btnOff = view.findViewById(R.id.btnOff)
        btnHome = view.findViewById(R.id.btnHome)
        btnHvac = view.findViewById(R.id.btnHvac)

        // 新增：副驾驶屏幕控制按钮
        btnPsdToggle = view.findViewById(R.id.btnPsdToggle)
        btnAutoPsdControl = view.findViewById(R.id.btnAutoPsdControl)
        btnMore = view.findViewById(R.id.btnMore)

        btnDrHeatOff = view.findViewById(R.id.btnDrHeatOff)
        btnDrHeat1 = view.findViewById(R.id.btnDrHeat1)
        btnDrHeat2 = view.findViewById(R.id.btnDrHeat2)
        btnDrHeat3 = view.findViewById(R.id.btnDrHeat3)
        btnDrVentOff = view.findViewById(R.id.btnDrVentOff)
        btnDrVent1 = view.findViewById(R.id.btnDrVent1)
        btnDrVent2 = view.findViewById(R.id.btnDrVent2)
        btnDrVent3 = view.findViewById(R.id.btnDrVent3)

        btnPassHeatOff = view.findViewById(R.id.btnPassHeatOff)
        btnPassHeat1 = view.findViewById(R.id.btnPassHeat1)
        btnPassHeat2 = view.findViewById(R.id.btnPassHeat2)
        btnPassHeat3 = view.findViewById(R.id.btnPassHeat3)
        btnPassVentOff = view.findViewById(R.id.btnPassVentOff)
        btnPassVent1 = view.findViewById(R.id.btnPassVent1)
        btnPassVent2 = view.findViewById(R.id.btnPassVent2)
        btnPassVent3 = view.findViewById(R.id.btnPassVent3)
    }

    /**
     * 新增：设置欢迎按钮点击事件
     * 无论欢迎按钮是否显示（有人/无人），点击时都会执行雨刮器服务位置调整
     */
    private fun setupWelcomeButtonClick(view: View) {
        txtWelcome?.setOnClickListener {
            // 无论按钮是否显示，点击都执行雨刮器服务位置调整
            if (::psdControlManager.isInitialized) {
                Log.d("ControlFragment", "欢迎按钮被点击，开始设置雨刮器服务位置")
                psdControlManager.setWiperServicePosition()

                // 根据是否有人显示不同的提示
                if (isPassengerPresent) {
                    showToast("副驾驶有人！正在调整雨刮器到服务位置...")
                } else {
                    showToast("正在调整雨刮器到服务位置...")
                }
            } else {
                showToast("雨刮器控制未初始化")
            }
        }
    }

    /**
     * 处理来自ClimateThread的状态消息
     */
    private fun handleStatusMessage(msg: Message) {
        val valueStr = msg.obj as? String ?: return

        when (msg.what) {
            IdNames.HVAC_FUNC_FAN_SPEED_MSG -> {
                val value = valueStr.toIntOrNull() ?: return
                currentFanSpeed = value
                updateFanDisplay()
                Log.d("ControlFragment", "风量状态更新: $value")
            }

            IdNames.HVAC_FUNC_AUTO_FAN_SETTING_MSG -> {
                val value = valueStr.toIntOrNull() ?: return
                currentAutoFanSetting = value
                updateFanDisplay()
                Log.d("ControlFragment", "自动风量设置更新: $value")
            }

            IdNames.HVAC_FUNC_AUTO_MSG -> {
                val value = valueStr.toIntOrNull() ?: return
                currentHvacAutoMode = value
                updateFanDisplay()
                updateBlowingModeDisplay()
                updateAutoDisplay()
                updateCirculationDisplay()
                Log.d("ControlFragment", "自动模式更新: $value")
            }

            IdNames.HVAC_FUNC_BLOWING_MODE_MSG -> {
                val value = valueStr.toIntOrNull() ?: return
                val newBlowingMode = value
                currentBlowingMode = newBlowingMode
                currentBlowingAutoMode = if (newBlowingMode == IdNames.BLOWING_MODE_AUTO_SWITCH) 1 else 0
                updateBlowingModeDisplay()
                updateFanDisplay()
                Log.d("ControlFragment", "出风模式更新: $value")
            }

            IdNames.HVAC_FUNC_CIRCULATION_MSG -> {
                val value = valueStr.toIntOrNull() ?: return
                currentCirculationMode = value
                updateCirculationDisplay()
                Log.d("ControlFragment", "循环模式更新: $value")
            }

            IdNames.HVAC_FUNC_AC_MSG -> {
                val value = valueStr.toIntOrNull() ?: return
                currentACState = value
                updateACDisplay()
                Log.d("ControlFragment", "AC状态更新: $value")
            }

            IdNames.HVAC_FUNC_G_CLEAN_MSG -> {
                val value = valueStr.toIntOrNull() ?: return
                currentGCleanState = value
                updateGCleanDisplay()
                Log.d("ControlFragment", "G-Clean状态更新: $value")
            }

            IdNames.SEAT_HEATING_DRIVER -> {
                val value = valueStr.toIntOrNull() ?: return
                driverHeatLevel = mapSeatValueToLevel(value, true)
                updateDriverHeatDisplay()
                Log.d("ControlFragment", "驾驶员座椅加热更新: $value")
            }

            IdNames.SEAT_VENTILATION_DRIVER -> {
                val value = valueStr.toIntOrNull() ?: return
                driverVentLevel = mapSeatValueToLevel(value, false)
                updateDriverVentDisplay()
                Log.d("ControlFragment", "驾驶员座椅通风更新: $value")
            }

            IdNames.SEAT_HEATING_PASSENGER -> {
                val value = valueStr.toIntOrNull() ?: return
                passHeatLevel = mapSeatValueToLevel(value, true)
                updatePassHeatDisplay()
                Log.d("ControlFragment", "副驾驶座椅加热更新: $value")
            }

            IdNames.SEAT_VENTILATION_PASSENGER -> {
                val value = valueStr.toIntOrNull() ?: return
                passVentLevel = mapSeatValueToLevel(value, false)
                updatePassVentDisplay()
                Log.d("ControlFragment", "副驾驶座椅通风更新: $value")
            }

            IdNames.INT_TEMPERATURE_MSG -> {
                activity?.runOnUiThread {
                    // 修改：只显示数值，不显示单位
                    txtInsideTempValue?.text = valueStr
                }
                Log.d("ControlFragment", "内温更新: $valueStr")
            }

            IdNames.AMBIENT_TEMPERATURE_MSG -> {
                activity?.runOnUiThread {
                    // 修改：只显示数值，不显示单位
                    txtOutsideTempValue?.text = valueStr
                }
                Log.d("ControlFragment", "外温更新: $valueStr")
            }

            IdNames.PASSENGER_SEAT_OCCUPATION_MSG -> {
                activity?.runOnUiThread {
                    val isOccupied = valueStr == "occupied"
                    isPassengerPresent = isOccupied

                    if (isOccupied && !forcePsdOff) {
                        txtWelcome?.text = "欢迎"
                        txtWelcome?.setTextColor(Color.parseColor("#FFD700")) // 金色文字
                        txtWelcome?.visibility = View.VISIBLE
                        txtWelcome?.isEnabled = true
                        Log.d("ControlFragment", "副驾驶有人，显示欢迎按钮（金色文字）")
                    } else {
                        // 无人时：按钮仍然可见，文字留空，使用默认文字颜色
                        txtWelcome?.text = ""
                        txtWelcome?.setTextColor(Color.WHITE) // 恢复白色文字
                        txtWelcome?.visibility = View.VISIBLE  // 按钮始终可见
                        txtWelcome?.isEnabled = true
                        Log.d("ControlFragment", "副驾驶无人，显示空白按钮（用于雨刮器调整）")
                    }

                    // 如果启用了强制关闭PSD，始终关闭屏幕
                    if (forcePsdOff) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (::psdControlManager.isInitialized) {
                                psdControlManager.turnOffPsdScreen()
                                refreshPsdStatus()
                            }
                        }, 500)
                    } else if (autoPsdControlEnabled && !isOccupied) {
                        // 如果启用了自动PSD控制且副驾驶无人，刷新PSD状态
                        Handler(Looper.getMainLooper()).postDelayed({
                            refreshPsdStatus()
                        }, 500)
                    }
                }
            }

            IdNames.PSD_STATUS_MSG -> {
                activity?.runOnUiThread {
                    val value = valueStr.toIntOrNull() ?: return@runOnUiThread
                    psdStatus = value
                    updatePsdToggleButton()
                    Log.d("ControlFragment", "副驾驶屏幕状态更新: $value")
                }
            }

            IdNames.LIGHT_LEFT_TURN -> {
                Log.d("ControlFragment", "左转向灯状态: $valueStr")
            }

            IdNames.LIGHT_RIGHT_TURN -> {
                Log.d("ControlFragment", "右转向灯状态: $valueStr")
            }

            IdNames.LIGHT_HAZARD_FLASHERS -> {
                Log.d("ControlFragment", "危险警告灯状态: $valueStr")
            }

            IdNames.HVAC_FUNC_TEMP_LEFT -> {
                Log.d("ControlFragment", "收到温度设置消息: $valueStr")
            }

            // 新增：挡位状态更新
            IdNames.GEAR_MSG -> {
                val gearValue = valueStr.toIntOrNull() ?: return
                Log.d("ControlFragment", "挡位状态更新: $gearValue")

                // 传递给FixSetManager处理后视镜下翻
                if (::fixSetManager.isInitialized) {
                    fixSetManager.handleGearChange(gearValue)
                }
            }

            // 新增：后视镜下翻模式更新
            IdNames.MIRROR_DIPPING_MSG -> {
                val mode = valueStr.toIntOrNull() ?: return
                Log.d("ControlFragment", "后视镜下翻模式更新: $mode")

                // 更新FixSetManager中的缓存
                if (::fixSetManager.isInitialized) {
                    fixSetManager.updateMirrorDippingMode(mode)
                }
            }

            // 新增：后视镜自动折叠状态更新
            IdNames.MIRROR_FOLDING_MSG -> {
                val foldingState = valueStr.toIntOrNull() ?: return
                Log.d("ControlFragment", "后视镜自动折叠状态更新: $foldingState")
            }
        }
    }

    // ===== 显示更新函数 =====

    /**
     * 更新风量显示 - 使用FanDisplayHelper
     */
    private fun updateFanDisplay() {
        activity?.runOnUiThread {
            if (::fanDisplayHelper.isInitialized && txtFanLevel != null) {
                val displayText = fanDisplayHelper.getFanDisplayString(
                    currentFanSpeed,
                    currentAutoFanSetting,
                    currentHvacAutoMode,
                    currentBlowingAutoMode
                )

                val isAutoMode = fanDisplayHelper.isAutoMode(
                    currentHvacAutoMode,
                    currentBlowingAutoMode
                )

                txtFanLevel?.text = displayText
                txtFanLevel?.setTextColor(fanDisplayHelper.getFanDisplayColor(isAutoMode))

                Log.d("ControlFragment", "风量显示更新: $displayText, 自动模式: $isAutoMode")
            }
        }
    }

    /**
     * 更新风向显示 - 通过文字颜色点亮按钮
     */
    private fun updateBlowingModeDisplay() {
        activity?.runOnUiThread {
            val isInAutoMode = currentHvacAutoMode == AUTO_MODE_ON || currentBlowingAutoMode == AUTO_MODE_ON

            if (isInAutoMode) {
                btnModeWindow?.setTextColor(Color.CYAN)
                btnModeLeg?.setTextColor(Color.CYAN)
                btnModeFace?.setTextColor(Color.CYAN)
                btnOnlyWindow?.setTextColor(Color.CYAN)
                btnOnlyLeg?.setTextColor(Color.CYAN)
                btnOnlyFace?.setTextColor(Color.CYAN)

                Log.d("ControlFragment", "风向显示: 自动模式，所有按钮显示青色")
            } else {
                updateModeFromValue(currentBlowingMode)
            }
        }
    }

    /**
     * 根据风向值更新按钮显示
     * 修改：处理value为-1（未知状态）的情况
     */
    private fun updateModeFromValue(value: Int) {
        activity?.runOnUiThread {
            // 修复：处理未知状态（value = -1）
            if (value == -1) {
                btnModeWindow?.setTextColor(Color.WHITE)
                btnModeLeg?.setTextColor(Color.WHITE)
                btnModeFace?.setTextColor(Color.WHITE)
                btnOnlyWindow?.setTextColor(Color.WHITE)
                btnOnlyLeg?.setTextColor(Color.WHITE)
                btnOnlyFace?.setTextColor(Color.WHITE)
                Log.d("ControlFragment", "风向未知（-1），所有按钮显示白色")
                return@runOnUiThread
            }

            val isWindow = when (value) {
                IdNames.BLOWING_MODE_FRONT_WINDOW,
                IdNames.BLOWING_MODE_FACE_AND_FRONT_WINDOW,
                IdNames.BLOWING_MODE_LEG_AND_FRONT_WINDOW,
                IdNames.BLOWING_MODE_ALL -> true
                else -> false
            }

            val isLeg = when (value) {
                IdNames.BLOWING_MODE_LEG,
                IdNames.BLOWING_MODE_FACE_AND_LEG,
                IdNames.BLOWING_MODE_LEG_AND_FRONT_WINDOW,
                IdNames.BLOWING_MODE_ALL -> true
                else -> false
            }

            val isFace = when (value) {
                IdNames.BLOWING_MODE_FACE,
                IdNames.BLOWING_MODE_FACE_AND_LEG,
                IdNames.BLOWING_MODE_FACE_AND_FRONT_WINDOW,
                IdNames.BLOWING_MODE_ALL -> true
                else -> false
            }

            btnModeWindow?.setTextColor(if (isWindow) Color.GREEN else Color.WHITE)
            btnModeLeg?.setTextColor(if (isLeg) Color.GREEN else Color.WHITE)
            btnModeFace?.setTextColor(if (isFace) Color.GREEN else Color.WHITE)

            btnOnlyWindow?.setTextColor(if (value == IdNames.BLOWING_MODE_FRONT_WINDOW) Color.GREEN else Color.WHITE)
            btnOnlyLeg?.setTextColor(if (value == IdNames.BLOWING_MODE_LEG) Color.GREEN else Color.WHITE)
            btnOnlyFace?.setTextColor(if (value == IdNames.BLOWING_MODE_FACE) Color.GREEN else Color.WHITE)

            Log.d("ControlFragment", "风向更新: 值=$value, 窗=$isWindow, 脚=$isLeg, 面=$isFace")
        }
    }

    /**
     * 更新循环显示
     */
    private fun updateCirculationDisplay() {
        activity?.runOnUiThread {
            if (currentHvacAutoMode == AUTO_MODE_ON) {
                btnCirculation?.text = "AUTO"
                btnCirculation?.setTextColor(Color.CYAN)
                return@runOnUiThread
            }

            val isInner = currentCirculationMode == IdNames.CIRCULATION_INNER
            val isOutsideOrOff = currentCirculationMode == IdNames.CIRCULATION_OUTSIDE ||
                    currentCirculationMode == IdNames.CIRCULATION_OFF ||
                    currentCirculationMode == IdNames.CIRCULATION_AUTO

            val displayText = when {
                isInner -> "内"
                isOutsideOrOff -> "外"
                else -> "外"
            }

            btnCirculation?.text = displayText
            btnCirculation?.setTextColor(if (isInner) Color.YELLOW else Color.WHITE)

            Log.d("ControlFragment", "循环模式更新: 显示=$displayText, 模式值=$currentCirculationMode, 空调自动模式=$currentHvacAutoMode")
        }
    }

    private fun updateACDisplay() {
        activity?.runOnUiThread {
            btnAcToggle?.setTextColor(if (currentACState == 1) Color.GREEN else Color.WHITE)
        }
    }

    private fun updateAutoDisplay() {
        activity?.runOnUiThread {
            btnAutoToggle?.setTextColor(if (currentHvacAutoMode == AUTO_MODE_ON) Color.GREEN else Color.WHITE)
        }
    }

    /**
     * 更新净化按钮显示
     */
    private fun updateGCleanDisplay() {
        activity?.runOnUiThread {
            if (currentGCleanState == IdNames.G_CLEAN_ON) {
                btnPurification?.setTextColor(Color.GREEN)
                btnPurification?.text = "净化中"
            } else {
                btnPurification?.setTextColor(Color.WHITE)
                btnPurification?.text = "净化"
            }
        }
    }

    /**
     * 更新OFF按钮显示文本 - 固定显示OFF
     */
    private fun updateOffButtonText() {
        activity?.runOnUiThread {
            btnOff?.text = "OFF"
            btnOff?.setTextColor(Color.RED)
        }
    }

    /**
     * 更新副驾驶屏幕切换按钮显示
     */
    private fun updatePsdToggleButton() {
        activity?.runOnUiThread {
            when (psdStatus) {
                IdNames.PSD_STATUS_ON -> {
                    btnPsdToggle?.text = "PSD ON"
                    btnPsdToggle?.setTextColor(Color.GREEN)
                }
                IdNames.PSD_STATUS_OFF -> {
                    btnPsdToggle?.text = "PSD OFF"
                    btnPsdToggle?.setTextColor(Color.WHITE)
                }
                else -> {
                    btnPsdToggle?.text = "PSD ?"
                    btnPsdToggle?.setTextColor(Color.WHITE)
                }
            }
        }
    }

    /**
     * 更新自动PSD控制按钮显示
     */
    private fun updateAutoPsdControlButton() {
        activity?.runOnUiThread {
            if (autoPsdControlEnabled) {
                btnAutoPsdControl?.text = "自动 ON"
                btnAutoPsdControl?.setTextColor(Color.GREEN)
            } else {
                btnAutoPsdControl?.text = "自动"
                btnAutoPsdControl?.setTextColor(Color.WHITE)
            }
        }
    }

    private fun updateDriverHeatDisplay() {
        updateSeatButtonGroup(listOf(btnDrHeatOff, btnDrHeat1, btnDrHeat2, btnDrHeat3), driverHeatLevel, true)
    }

    private fun updateDriverVentDisplay() {
        updateSeatButtonGroup(listOf(btnDrVentOff, btnDrVent1, btnDrVent2, btnDrVent3), driverVentLevel, false)
    }

    private fun updatePassHeatDisplay() {
        updateSeatButtonGroup(listOf(btnPassHeatOff, btnPassHeat1, btnPassHeat2, btnPassHeat3), passHeatLevel, true)
    }

    private fun updatePassVentDisplay() {
        updateSeatButtonGroup(listOf(btnPassVentOff, btnPassVent1, btnPassVent2, btnPassVent3), passVentLevel, false)
    }

    private fun updateSeatButtonGroup(buttons: List<Button?>, activeLevel: Int, isHeating: Boolean) {
        activity?.runOnUiThread {
            buttons.forEachIndexed { index, btn ->
                val isActive = index == activeLevel
                if (isActive) {
                    val color = if (isHeating) Color.parseColor("#FFA500") else Color.parseColor("#87CEEB")
                    btn?.setTextColor(color)
                } else {
                    btn?.setTextColor(Color.WHITE)
                }
            }
        }
    }

    // ===== 辅助方法 =====

    private fun mapSeatValueToLevel(value: Int, isHeating: Boolean): Int {
        return if (isHeating) {
            when (value) {
                IdNames.SEAT_HEATING_OFF -> 0
                IdNames.SEAT_HEATING_LEVEL_1 -> 1
                IdNames.SEAT_HEATING_LEVEL_2 -> 2
                IdNames.SEAT_HEATING_LEVEL_3 -> 3
                else -> 0
            }
        } else {
            when (value) {
                IdNames.SEAT_VENTILATION_OFF -> 0
                IdNames.SEAT_VENTILATION_LEVEL_1 -> 1
                IdNames.SEAT_VENTILATION_LEVEL_2 -> 2
                IdNames.SEAT_VENTILATION_LEVEL_3 -> 3
                else -> 0
            }
        }
    }

    // ===== 控制设置 =====

    private fun setupSeatControls(view: View) {
        setupSeatButtonGroupClick(
            listOf(R.id.btnDrHeatOff, R.id.btnDrHeat1, R.id.btnDrHeat2, R.id.btnDrHeat3),
            view, 1, true
        )
        setupSeatButtonGroupClick(
            listOf(R.id.btnDrVentOff, R.id.btnDrVent1, R.id.btnDrVent2, R.id.btnDrVent3),
            view, 1, false
        )
        setupSeatButtonGroupClick(
            listOf(R.id.btnPassHeatOff, R.id.btnPassHeat1, R.id.btnPassHeat2, R.id.btnPassHeat3),
            view, 4, true
        )
        setupSeatButtonGroupClick(
            listOf(R.id.btnPassVentOff, R.id.btnPassVent1, R.id.btnPassVent2, R.id.btnPassVent3),
            view, 4, false
        )
    }

    private fun setupSeatButtonGroupClick(ids: List<Int>, parent: View, zone: Int, isHeat: Boolean) {
        ids.forEachIndexed { index, id ->
            parent.findViewById<Button>(id).setOnClickListener {
                if (::seatManager.isInitialized) {
                    val level = index
                    if (isHeat) {
                        seatManager.setSeatHeating(zone, level)
                    } else {
                        seatManager.setSeatVentilation(zone, level)
                    }
                }
            }
        }
    }

    private fun setupACControls(view: View) {
        view.findViewById<Button>(R.id.btnOff).setOnClickListener {
            if (::climateManager.isInitialized) {
                climateManager.turnOff()
                showToast("关闭AC、AUTO和净化")

                Handler(Looper.getMainLooper()).postDelayed({
                    updateACDisplay()
                    updateAutoDisplay()
                    updateGCleanDisplay()
                }, 500)
            }
        }

        btnAcToggle?.setOnClickListener {
            if (::climateManager.isInitialized) {
                val newState = currentACState != 1
                climateManager.setAc(newState)
            }
        }

        btnAutoToggle?.setOnClickListener {
            if (::climateManager.isInitialized) {
                val newState = currentHvacAutoMode != 1
                climateManager.setAuto(newState)

                Handler(Looper.getMainLooper()).postDelayed({
                    updateFanDisplay()
                    updateBlowingModeDisplay()
                }, 300)
            }
        }

        btnCirculation?.setOnClickListener {
            if (::climateManager.isInitialized) {
                if (currentHvacAutoMode == AUTO_MODE_ON) {
                    climateManager.setAuto(false)
                    climateManager.setCirculation(true)
                    currentHvacAutoMode = 0
                    currentCirculationMode = IdNames.CIRCULATION_INNER
                } else {
                    val isCurrentlyInner = currentCirculationMode == IdNames.CIRCULATION_INNER

                    if (isCurrentlyInner) {
                        climateManager.setCirculation(false)
                        currentCirculationMode = IdNames.CIRCULATION_OFF
                    } else {
                        climateManager.setCirculation(true)
                        currentCirculationMode = IdNames.CIRCULATION_INNER
                    }
                }

                updateCirculationDisplay()
                Log.d("ControlFragment", "循环按钮点击: 当前空调自动模式=${currentHvacAutoMode}, 循环模式=$currentCirculationMode")
            }
        }

        btnPurification?.setOnClickListener {
            if (::climateManager.isInitialized) {
                val newState = currentGCleanState != IdNames.G_CLEAN_ON
                climateManager.setGClean(newState)

                if (newState) {
                    currentGCleanState = IdNames.G_CLEAN_ON
                } else {
                    currentGCleanState = IdNames.G_CLEAN_OFF
                }
                updateGCleanDisplay()

                Log.d("ControlFragment", "点击净化按钮，新状态: $newState")
            }
        }

        btnHome?.setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            activity?.startActivity(homeIntent)
        }

        btnHvac?.setOnClickListener {
            val context = context ?: return@setOnClickListener
            HvacJumpHelper.jumpToAirCondition(context)
        }

        btnSeat?.setOnClickListener {
            val context = context ?: return@setOnClickListener
            HvacJumpHelper.jumpToSeat(context)
        }
    }

    /**
     * 设置副驾驶屏幕控制
     */
    private fun setupPsdControls(view: View) {
        btnPsdToggle?.setOnClickListener {
            if (::psdControlManager.isInitialized) {
                psdControlManager.togglePsdScreen()
                showToast("切换副驾驶屏幕")

                // 延迟刷新状态
                Handler(Looper.getMainLooper()).postDelayed({
                    refreshPsdStatus()
                }, 300)
            } else {
                showToast("副驾驶屏幕控制未初始化")
            }
        }

        btnAutoPsdControl?.setOnClickListener {
            autoPsdControlEnabled = !autoPsdControlEnabled
            updateAutoPsdControlButton()

            // 设置ClimateThread中的自动控制状态
            climateThreadManager.getClimateThread()?.setAutoPsdControl(autoPsdControlEnabled)

            // 保存状态到SharedPreferences
            if (::fixSetManager.isInitialized) {
                fixSetManager.setAutoPsdEnabled(autoPsdControlEnabled)
            }

            if (autoPsdControlEnabled) {
                showToast("启用副驾驶屏幕自动控制")

                // 如果当前副驾驶无人，立即关闭屏幕
                if (!isPassengerPresent) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        psdControlManager.turnOffPsdScreen()
                        refreshPsdStatus()
                    }, 500)
                }
            } else {
                showToast("关闭副驾驶屏幕自动控制")
            }
        }

        // 初始化按钮状态
        updatePsdToggleButton()
        updateAutoPsdControlButton()
    }

    private fun setupModeControls(view: View) {
        btnModeFace?.setOnClickListener {
            if (::climateManager.isInitialized) {
                val isFaceOn = when (currentBlowingMode) {
                    IdNames.BLOWING_MODE_FACE,
                    IdNames.BLOWING_MODE_FACE_AND_LEG,
                    IdNames.BLOWING_MODE_FACE_AND_FRONT_WINDOW,
                    IdNames.BLOWING_MODE_ALL -> true
                    else -> false
                }

                if (isFaceOn) {
                    val newMode = when (currentBlowingMode) {
                        IdNames.BLOWING_MODE_FACE -> IdNames.BLOWING_MODE_OFF
                        IdNames.BLOWING_MODE_FACE_AND_LEG -> IdNames.BLOWING_MODE_LEG
                        IdNames.BLOWING_MODE_FACE_AND_FRONT_WINDOW -> IdNames.BLOWING_MODE_FRONT_WINDOW
                        IdNames.BLOWING_MODE_ALL -> IdNames.BLOWING_MODE_LEG_AND_FRONT_WINDOW
                        else -> currentBlowingMode
                    }
                    climateManager.setBlowingMode(newMode)
                    currentBlowingMode = newMode
                    updateModeFromValue(newMode)
                    Log.d("ControlFragment", "组合模式-面: 关闭面，新模式=$newMode")
                } else {
                    val newMode = when (currentBlowingMode) {
                        IdNames.BLOWING_MODE_OFF -> IdNames.BLOWING_MODE_FACE
                        IdNames.BLOWING_MODE_LEG -> IdNames.BLOWING_MODE_FACE_AND_LEG
                        IdNames.BLOWING_MODE_FRONT_WINDOW -> IdNames.BLOWING_MODE_FACE_AND_FRONT_WINDOW
                        IdNames.BLOWING_MODE_LEG_AND_FRONT_WINDOW -> IdNames.BLOWING_MODE_ALL
                        else -> IdNames.BLOWING_MODE_FACE
                    }
                    climateManager.setBlowingMode(newMode)
                    currentBlowingMode = newMode
                    updateModeFromValue(newMode)
                    Log.d("ControlFragment", "组合模式-面: 开启面，新模式=$newMode")
                }
            }
        }

        btnModeLeg?.setOnClickListener {
            if (::climateManager.isInitialized) {
                val isLegOn = when (currentBlowingMode) {
                    IdNames.BLOWING_MODE_LEG,
                    IdNames.BLOWING_MODE_FACE_AND_LEG,
                    IdNames.BLOWING_MODE_LEG_AND_FRONT_WINDOW,
                    IdNames.BLOWING_MODE_ALL -> true
                    else -> false
                }

                if (isLegOn) {
                    val newMode = when (currentBlowingMode) {
                        IdNames.BLOWING_MODE_LEG -> IdNames.BLOWING_MODE_OFF
                        IdNames.BLOWING_MODE_FACE_AND_LEG -> IdNames.BLOWING_MODE_FACE
                        IdNames.BLOWING_MODE_LEG_AND_FRONT_WINDOW -> IdNames.BLOWING_MODE_FRONT_WINDOW
                        IdNames.BLOWING_MODE_ALL -> IdNames.BLOWING_MODE_FACE_AND_FRONT_WINDOW
                        else -> currentBlowingMode
                    }
                    climateManager.setBlowingMode(newMode)
                    currentBlowingMode = newMode
                    updateModeFromValue(newMode)
                    Log.d("ControlFragment", "组合模式-脚: 关闭脚，新模式=$newMode")
                } else {
                    val newMode = when (currentBlowingMode) {
                        IdNames.BLOWING_MODE_OFF -> IdNames.BLOWING_MODE_LEG
                        IdNames.BLOWING_MODE_FACE -> IdNames.BLOWING_MODE_FACE_AND_LEG
                        IdNames.BLOWING_MODE_FRONT_WINDOW -> IdNames.BLOWING_MODE_LEG_AND_FRONT_WINDOW
                        IdNames.BLOWING_MODE_FACE_AND_FRONT_WINDOW -> IdNames.BLOWING_MODE_ALL
                        else -> IdNames.BLOWING_MODE_LEG
                    }
                    climateManager.setBlowingMode(newMode)
                    currentBlowingMode = newMode
                    updateModeFromValue(newMode)
                    Log.d("ControlFragment", "组合模式-脚: 开启脚，新模式=$newMode")
                }
            }
        }

        btnModeWindow?.setOnClickListener {
            if (::climateManager.isInitialized) {
                val isWindowOn = when (currentBlowingMode) {
                    IdNames.BLOWING_MODE_FRONT_WINDOW,
                    IdNames.BLOWING_MODE_FACE_AND_FRONT_WINDOW,
                    IdNames.BLOWING_MODE_LEG_AND_FRONT_WINDOW,
                    IdNames.BLOWING_MODE_ALL -> true
                    else -> false
                }

                if (isWindowOn) {
                    val newMode = when (currentBlowingMode) {
                        IdNames.BLOWING_MODE_FRONT_WINDOW -> IdNames.BLOWING_MODE_OFF
                        IdNames.BLOWING_MODE_FACE_AND_FRONT_WINDOW -> IdNames.BLOWING_MODE_FACE
                        IdNames.BLOWING_MODE_LEG_AND_FRONT_WINDOW -> IdNames.BLOWING_MODE_LEG
                        IdNames.BLOWING_MODE_ALL -> IdNames.BLOWING_MODE_FACE_AND_LEG
                        else -> currentBlowingMode
                    }
                    climateManager.setBlowingMode(newMode)
                    currentBlowingMode = newMode
                    updateModeFromValue(newMode)
                    Log.d("ControlFragment", "组合模式-窗: 关闭窗，新模式=$newMode")
                } else {
                    val newMode = when (currentBlowingMode) {
                        IdNames.BLOWING_MODE_OFF -> IdNames.BLOWING_MODE_FRONT_WINDOW
                        IdNames.BLOWING_MODE_FACE -> IdNames.BLOWING_MODE_FACE_AND_FRONT_WINDOW
                        IdNames.BLOWING_MODE_LEG -> IdNames.BLOWING_MODE_LEG_AND_FRONT_WINDOW
                        IdNames.BLOWING_MODE_FACE_AND_LEG -> IdNames.BLOWING_MODE_ALL
                        else -> IdNames.BLOWING_MODE_FRONT_WINDOW
                    }
                    climateManager.setBlowingMode(newMode)
                    currentBlowingMode = newMode
                    updateModeFromValue(newMode)
                    Log.d("ControlFragment", "组合模式-窗: 开启窗，新模式=$newMode")
                }
            }
        }
    }

    // 快捷模式按钮控制（英文按钮，单独模式）
    private fun setupQuickModeControls(view: View) {
        btnOnlyFace?.setOnClickListener {
            if (::climateManager.isInitialized) {
                climateManager.setBlowingMode(IdNames.BLOWING_MODE_FACE)
                currentBlowingMode = IdNames.BLOWING_MODE_FACE
                updateModeFromValue(IdNames.BLOWING_MODE_FACE)
                Log.d("ControlFragment", "快捷模式-仅面: 设置为BLOWING_MODE_FACE")
            }
        }

        btnOnlyLeg?.setOnClickListener {
            if (::climateManager.isInitialized) {
                climateManager.setBlowingMode(IdNames.BLOWING_MODE_LEG)
                currentBlowingMode = IdNames.BLOWING_MODE_LEG
                updateModeFromValue(IdNames.BLOWING_MODE_LEG)
                Log.d("ControlFragment", "快捷模式-仅脚: 设置为BLOWING_MODE_LEG")
            }
        }

        btnOnlyWindow?.setOnClickListener {
            if (::climateManager.isInitialized) {
                climateManager.setBlowingMode(IdNames.BLOWING_MODE_FRONT_WINDOW)
                currentBlowingMode = IdNames.BLOWING_MODE_FRONT_WINDOW
                updateModeFromValue(IdNames.BLOWING_MODE_FRONT_WINDOW)
                Log.d("ControlFragment", "快捷模式-仅窗: 设置为BLOWING_MODE_FRONT_WINDOW")
            }
        }
    }

    /**
     * 刷新当前状态 - 直接从车辆系统读取（新增方法）
     */
    private fun refreshCurrentState() {
        try {
            Log.d("ControlFragment", "开始刷新当前状态")

            if (::climateManager.isInitialized) {
                climateManager.refreshCurrentState()
                showToast("嗨起来")
            }

            climateThreadManager?.getClimateThread()?.queryAllInitialStates()

            // 刷新PSD状态
            refreshPsdStatus()

            Log.d("ControlFragment", "刷新当前状态请求已发送")

        } catch (e: Exception) {
            Log.e("ControlFragment", "刷新状态失败", e)
            showToast("刷新状态失败")
        }
    }

    /**
     * 刷新副驾驶屏幕状态
     */
    private fun refreshPsdStatus() {
        try {
            if (::psdControlManager.isInitialized) {
                val status = psdControlManager.getPsdStatus()
                if (status != -1) {
                    psdStatus = status
                    updatePsdToggleButton()
                    Log.d("ControlFragment", "刷新副驾驶屏幕状态: $status")
                }
            }
        } catch (e: Exception) {
            Log.e("ControlFragment", "刷新PSD状态失败", e)
        }
    }

    /**
     * 显示Toast消息
     */
    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 设置More按钮点击事件
     */
    private fun setupMoreButton(view: View) {
        btnMore?.setOnClickListener {
            // 优先使用MoreSettingsController（当ClimateThread已初始化时）
            if (moreSettingsController != null) {
                moreSettingsController?.showSettingsDialog()
            } else {
                // 退回到直接使用FixSetManager的对话框
                showSettingsDialog()
            }
        }
    }

    /**
     * 显示设置对话框（备用方案，当MoreSettingsController未初始化时使用）
     * 菜单顺序：强制关闭副驾驶屏幕 → 后视镜下翻设置 → 灯光控制
     */
    private fun showSettingsDialog() {
        if (!::fixSetManager.isInitialized) {
            showToast("设置管理器未初始化")
            return
        }

        val items = arrayOf(
            // === 副驾驶屏幕控制（最上方） ===
            "强制关闭副驾驶屏幕: ${if (forcePsdOff) "☑" else "☐"}",
            "---",
            // === 后视镜下翻设置（中间） ===
            "倒车后视镜下翻: ${fixSetManager.getMirrorDippingModeText()}",
            "  · 关闭下翻",
            "  · 仅驾驶侧下翻",
            "  · 仅副驾侧下翻",
            "  · 双侧同时下翻",
            "---",
            // === 灯光控制（最下方） ===
            "转向辅助灯: ${if (fixSetManager.getBendingLightStatus()) "ON" else "OFF"}",
            "迎宾灯: ${if (fixSetManager.getCourtesyLightStatus()) "ON" else "OFF"}",
            "转弯灯: ${if (fixSetManager.getCorneringLightStatus()) "ON" else "OFF"}",
            "---",
            "自动开启转向辅助灯: ${if (fixSetManager.isAutoBendingLightEnabled()) "☑" else "☐"}",
            "自动开启迎宾灯: ${if (fixSetManager.isAutoCourtesyLightEnabled()) "☑" else "☐"}",
            "自动开启转弯灯: ${if (fixSetManager.isAutoCorneringLightEnabled()) "☑" else "☐"}"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("更多设置")
            .setItems(items) { _, which ->
                when (which) {
                    // === 副驾驶屏幕控制 ===
                    0 -> { // 强制关闭副驾驶屏幕
                        forcePsdOff = !forcePsdOff
                        fixSetManager.setForcePsdOff(forcePsdOff)
                        showToast("强制关闭副驾驶屏幕: ${if (forcePsdOff) "已开启" else "已关闭"}")

                        if (forcePsdOff && ::psdControlManager.isInitialized) {
                            psdControlManager.turnOffPsdScreen()
                        }

                        Handler(Looper.getMainLooper()).postDelayed({
                            refreshPsdStatus()
                        }, 300)
                    }

                    // === 后视镜下翻设置 ===
                    2 -> { // 点击当前模式 - 循环切换
                        fixSetManager.cycleMirrorDippingMode()
                        showToast("后视镜下翻模式: ${fixSetManager.getMirrorDippingModeText()}")
                    }
                    3 -> { // 关闭下翻
                        fixSetManager.setMirrorDippingMode(IdNames.MIRROR_DIPPING_OFF)
                        showToast("后视镜下翻: 已关闭")
                    }
                    4 -> { // 仅驾驶侧下翻
                        fixSetManager.setMirrorDippingMode(IdNames.MIRROR_DIPPING_DRIVER)
                        showToast("后视镜下翻: 仅驾驶侧")
                    }
                    5 -> { // 仅副驾侧下翻
                        fixSetManager.setMirrorDippingMode(IdNames.MIRROR_DIPPING_PASSENGER)
                        showToast("后视镜下翻: 仅副驾侧")
                    }
                    6 -> { // 双侧同时下翻
                        fixSetManager.setMirrorDippingMode(IdNames.MIRROR_DIPPING_BOTH)
                        showToast("后视镜下翻: 双侧同时")
                    }

                    // === 灯光直接控制 ===
                    8 -> { // 转向辅助灯
                        fixSetManager.toggleBendingLight()
                        showToast("转向辅助灯: ${if (fixSetManager.getBendingLightStatus()) "ON" else "OFF"}")
                    }
                    9 -> { // 迎宾灯
                        fixSetManager.toggleCourtesyLight()
                        showToast("迎宾灯: ${if (fixSetManager.getCourtesyLightStatus()) "ON" else "OFF"}")
                    }
                    10 -> { // 转弯灯
                        fixSetManager.toggleCorneringLight()
                        showToast("转弯灯: ${if (fixSetManager.getCorneringLightStatus()) "ON" else "OFF"}")
                    }

                    // === 自动灯光设置 ===
                    12 -> { // 自动开启转向辅助灯
                        val newState = !fixSetManager.isAutoBendingLightEnabled()
                        fixSetManager.setAutoBendingLight(newState)
                        showToast("自动转向辅助灯: ${if (newState) "已开启" else "已关闭"}")
                    }
                    13 -> { // 自动开启迎宾灯
                        val newState = !fixSetManager.isAutoCourtesyLightEnabled()
                        fixSetManager.setAutoCourtesyLight(newState)
                        showToast("自动迎宾灯: ${if (newState) "已开启" else "已关闭"}")
                    }
                    14 -> { // 自动开启转弯灯
                        val newState = !fixSetManager.isAutoCorneringLightEnabled()
                        fixSetManager.setAutoCorneringLight(newState)
                        showToast("自动转弯灯: ${if (newState) "已开启" else "已关闭"}")
                    }
                }
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    /**
     * 加载保存的设置状态
     */
    private fun loadSavedSettings() {
        if (::fixSetManager.isInitialized) {
            // 恢复自动PSD控制状态
            autoPsdControlEnabled = fixSetManager.isAutoPsdEnabled()
            // 恢复强制关闭PSD状态
            forcePsdOff = fixSetManager.isForcePsdOffEnabled()

            // 更新按钮显示
            updateAutoPsdControlButton()

            Log.d("ControlFragment", "加载保存的设置: autoPsd=$autoPsdControlEnabled, forcePsdOff=$forcePsdOff")

            // 如果强制关闭PSD已启用，立即关闭屏幕
            if (forcePsdOff && ::psdControlManager.isInitialized) {
                Handler(Looper.getMainLooper()).postDelayed({
                    psdControlManager.turnOffPsdScreen()
                }, 1000)
            }
        }
    }
}