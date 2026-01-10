package com.seat.hvac

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import kotlin.apply

/**
 * HVAC应用跳转帮助类
 * 采用多种方式确保跳转成功
 */
object HvacJumpHelper {

    // HVAC应用信息
    private const val HVAC_PACKAGE = "com.geely.hvac"
    private const val HVAC_MAIN_ACTIVITY = "com.geely.hvac.activity.GlyMainActivity"

    // Intent参数
    private const val PARAM_ENTER_HVAC_PAGE = "enter_hvac_page"
    private const val PARAM_ENTER_SEAT_PAGE = "enter_seat_page"

    // 界面类型
    private const val PAGE_AIR_CONDITION = 0  // 空调界面
    private const val PAGE_SEAT = 1           // 座椅界面

    /**
     * 跳转到空调界面
     */
    fun jumpToAirCondition(context: Context) {
        if (!isHvacInstalled(context)) {
            showToast(context, "未找到HVAC应用")
            return
        }

        // 尝试多种方式跳转
        if (tryDirectJump(context, PAGE_AIR_CONDITION, 0)) {
            return
        }

        if (tryAlternativeJump(context, PAGE_AIR_CONDITION, 0)) {
            return
        }

        if (tryBroadcastJump(context, PAGE_AIR_CONDITION, 0)) {
            return
        }

        showToast(context, "无法打开空调界面")
    }

    /**
     * 跳转到座椅界面（主驾驶）
     */
    fun jumpToSeat(context: Context) {
        if (!isHvacInstalled(context)) {
            showToast(context, "未找到HVAC应用")
            return
        }

        // 尝试多种方式跳转
        if (tryDirectJump(context, PAGE_SEAT, 0)) {
            return
        }

        if (tryAlternativeJump(context, PAGE_SEAT, 0)) {
            return
        }

        if (tryBroadcastJump(context, PAGE_SEAT, 0)) {
            return
        }

        showToast(context, "无法打开座椅界面")
    }

    /**
     * 方式1：直接组件跳转（主要方式）
     */
    private fun tryDirectJump(context: Context, mainPage: Int, seatTab: Int): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(HVAC_PACKAGE, HVAC_MAIN_ACTIVITY)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP

                putExtra(PARAM_ENTER_HVAC_PAGE, mainPage)
                if (mainPage == PAGE_SEAT) {
                    putExtra(PARAM_ENTER_SEAT_PAGE, seatTab)
                }
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 方式2：备用跳转方式（使用不同的参数和Flags）
     */
    private fun tryAlternativeJump(context: Context, mainPage: Int, seatTab: Int): Boolean {
        return try {
            val intent = Intent().apply {
                component = ComponentName(HVAC_PACKAGE, HVAC_MAIN_ACTIVITY)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK

                // 尝试不同的参数名
                putExtra("ENTER_HVAC_KEY", mainPage)
                if (mainPage == PAGE_SEAT) {
                    putExtra("ENTER_SEAT_KEY", seatTab)
                }

                // 同时添加原来的参数名
                putExtra(PARAM_ENTER_HVAC_PAGE, mainPage)
                if (mainPage == PAGE_SEAT) {
                    putExtra(PARAM_ENTER_SEAT_PAGE, seatTab)
                }
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 方式3：广播跳转（备用方式）
     */
    private fun tryBroadcastJump(context: Context, mainPage: Int, seatTab: Int): Boolean {
        return try {
            // 尝试发送广播
            val broadcastIntent = Intent().apply {
                action = "com.geely.hvac.ACTION_OPEN"
                setPackage(HVAC_PACKAGE)

                putExtra(PARAM_ENTER_HVAC_PAGE, mainPage)
                if (mainPage == PAGE_SEAT) {
                    putExtra(PARAM_ENTER_SEAT_PAGE, seatTab)
                }
            }

            context.sendBroadcast(broadcastIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查HVAC应用是否已安装
     */
    private fun isHvacInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(HVAC_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 显示Toast消息
     */
    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}