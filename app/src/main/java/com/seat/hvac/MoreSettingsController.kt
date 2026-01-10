package com.seat.hvac

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast

/**
 * MoreSettingsController - 管理More按钮的设置菜单功能
 * 从ControlFragment中拆分出来，负责处理灯光控制、后视镜下翻和PSD强制关闭等功能
 */
class MoreSettingsController(
    private val context: Context,
    private val fixSetManager: FixSetManager,
    private val psdControlManager: PsdControlManager?,
    private val onRefreshPsdStatus: () -> Unit
) {
    companion object {
        private const val TAG = "MoreSettingsController"
    }

    // 强制关闭PSD状态
    var forcePsdOff: Boolean = false
        private set

    init {
        // 从保存的设置中恢复状态
        forcePsdOff = fixSetManager.isForcePsdOffEnabled()
        Log.d(TAG, "初始化完成，forcePsdOff=$forcePsdOff")
    }

    /**
     * 设置More按钮点击事件
     */
    fun setupMoreButton(btnMore: Button?) {
        btnMore?.setOnClickListener {
            showSettingsDialog()
        }
    }

    /**
     * 显示设置对话框
     * 菜单顺序：强制关闭副驾驶屏幕 → 后视镜下翻设置 → 灯光控制
     */
    fun showSettingsDialog() {
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
            "自动开启转弯灯: ${if (fixSetManager.isAutoCorneringLightEnabled()) "☑" else "☐"}",
            "---",
            // === 后视镜下翻测试按钮（新增） ===
            "[测试] 左侧后视镜立即下翻 (设为驾驶侧)",
            "[测试] 右侧后视镜立即下翻 (设为副驾侧)",
            "[测试] 双侧后视镜立即下翻 (设为双侧)"
        )

        AlertDialog.Builder(context)
            .setTitle("更多设置")
            .setItems(items) { _, which ->
                handleMenuItemClick(which)
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun handleMenuItemClick(which: Int) {
        when (which) {
            // === 副驾驶屏幕控制 ===
            0 -> { // 强制关闭副驾驶屏幕
                toggleForcePsdOff()
            }

            // === 后视镜下翻设置 ===
            2 -> { // 点击当前模式显示 - 循环切换模式
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

            // === 后视镜下翻测试按钮 ===
            16 -> { // 左侧下翻
                fixSetManager.setMirrorDippingMode(IdNames.MIRROR_DIPPING_DRIVER)
                showToast("测试: 设置为仅驾驶侧下翻")
            }
            17 -> { // 右侧下翻
                fixSetManager.setMirrorDippingMode(IdNames.MIRROR_DIPPING_PASSENGER)
                showToast("测试: 设置为仅副驾侧下翻")
            }
            18 -> { // 双侧下翻
                fixSetManager.setMirrorDippingMode(IdNames.MIRROR_DIPPING_BOTH)
                showToast("测试: 设置为双侧下翻")
            }
        }
    }

    /**
     * 切换强制关闭副驾驶屏幕状态
     */
    private fun toggleForcePsdOff() {
        forcePsdOff = !forcePsdOff
        fixSetManager.setForcePsdOff(forcePsdOff)

        if (forcePsdOff) {
            // 立即关闭副驾驶屏幕
            psdControlManager?.turnOffPsdScreen()
            showToast("强制关闭副驾驶屏幕: 已开启")
        } else {
            showToast("强制关闭副驾驶屏幕: 已关闭")
        }

        Handler(Looper.getMainLooper()).postDelayed({
            onRefreshPsdStatus()
        }, 300)
    }

    /**
     * 处理副驾驶座位状态变化 - 检查是否需要强制关闭PSD
     */
    fun handlePassengerOccupationChange(isOccupied: Boolean): Boolean {
        if (forcePsdOff) {
            // 如果启用了强制关闭PSD，始终关闭屏幕
            Handler(Looper.getMainLooper()).postDelayed({
                psdControlManager?.turnOffPsdScreen()
                onRefreshPsdStatus()
            }, 500)
            return true // 返回true表示已处理
        }
        return false // 返回false表示未处理，需要继续其他逻辑
    }

    /**
     * 应用启动时检查并执行强制关闭PSD
     */
    fun applyForcePsdOffIfEnabled() {
        if (forcePsdOff) {
            Handler(Looper.getMainLooper()).postDelayed({
                psdControlManager?.turnOffPsdScreen()
            }, 1000)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}