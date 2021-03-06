@file:Suppress("DEPRECATION")

package cn.miuihone.xiaowine.hook.app


import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import cn.miuihone.xiaowine.hook.BaseHook
import cn.miuihone.xiaowine.utils.LogUtils
import cn.miuihone.xiaowine.utils.MemoryUtils
import cn.miuihone.xiaowine.utils.Utils
import cn.miuihone.xiaowine.utils.Utils.catchNoClass
import cn.miuihone.xiaowine.utils.Utils.formatSize
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.putObject
import kotlin.math.roundToInt


@SuppressLint("StaticFieldLeak")
object MiuiHome : BaseHook() {
    private var TextViewMaps = LinkedHashMap<String, TextView>()
    private var TextViewList: List<String> = ArrayList<String>(listOf(
        "MemoryView",
        "ZarmView",
        "StorageView",
        "BootTime",
        "RunningAppTotal",
        "RunningServiceTotal",
    ))
    private lateinit var mTxtMemoryViewGroup: ViewGroup
    private lateinit var mTxtMemoryInfo1: TextView
    private lateinit var MemoryLayout: LinearLayout
    private var MaxWidth = 300
    private const val threshold = 21

    @SuppressLint("SetTextI18n") override fun init() {
//        refresh view data
        catchNoClass {
            findMethod("com.miui.home.recents.views.RecentsContainer") { name == "refreshMemoryInfo" }.hookAfter {
                LogUtils.i("refreshMemoryInfo")
                val memoryInfo = MemoryUtils().getMemoryInfo(appContext)
                val swapInfo = MemoryUtils().getPartitionInfo("SwapTotal", "SwapFree")
                val storageInfo = MemoryUtils().getStorageInfo(Environment.getExternalStorageDirectory())

//                status color
                TextViewMaps.forEach { (name, view) ->
                    run {
                        when (name) {
                            "MemoryView" -> {
                                view.text = "?????? ${memoryInfo.availMem.formatSize()} | ${memoryInfo.totalMem.formatSize()}\t?????? ${memoryInfo.percentValue}%"
                                view.setOnClickListener{
                                    val intent = Intent()
                                    intent.setClassName("com.android.settings", "com.android.settings.SubSettings")
                                    appContext.startActivity(intent)
                                }
                                if (memoryInfo.percentValue < threshold) {
                                    view.setTextColor(Color.RED)
                                } else {
                                    view.setTextColor(mTxtMemoryInfo1.textColors)
                                }
                            }
                            "ZarmView" -> {
                                view.text = "?????? ${swapInfo.availMem.formatSize()} | ${swapInfo.totalMem.formatSize()}\t?????? ${swapInfo.percentValue}%"
                                if (swapInfo.percentValue < threshold) {
                                    view.setTextColor(Color.RED)
                                } else {
                                    view.setTextColor(mTxtMemoryInfo1.textColors)
                                }
                            }
                            "StorageView" -> {
                                view.text = "?????? ${storageInfo.availMem.formatSize()} | ${storageInfo.totalMem.formatSize()}\t?????? ${storageInfo.percentValue}%"
//                                view.setOnClickListener {
//                                    val intent = Intent()
//                                    intent.setClassName("com.miui.home", "com.miui.home.activity.MainActivity")
//                                    appContext.startActivity(intent)
//                                }
                                if (storageInfo.percentValue < threshold) {
                                    view.setTextColor(Color.RED)
                                } else {
                                    view.setTextColor(mTxtMemoryInfo1.textColors)
                                }
                            }
                            "BootTime" -> {
                                view.text = "??????????????? ${Utils.BootTime.get()}"
                            }
                            "RunningAppTotal" -> {
                                view.text = "????????????????????? ${(appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses.size}"
//                                /* view.setOnClickListener{
//                                     val intent = Intent()
//                                     intent.setClassName("com.android.settings", "com.android.settings.SubSettings")
//                                     appContext.startActivity(intent)
//                                 }*/
                            }
                            "RunningServiceTotal" -> {
                                view.text = "????????????????????? ${(appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getRunningServices(999).size}"
//                                view.setOnClickListener{
//                                    val intent = Intent()
//                                    intent.setClassName("com.android.settings", "com.android.settings.SubSettings")
//                                    intent.putExtra("settings:source_metrics",39)
//                                    intent.putExtra("android:show_fragment_title",-1)
//                                    intent.putExtra("show_fragment_title_resid",-1)
//                                    intent.putExtra("settings:show_fragment_title","?????????????????????/??????")
//                                    intent.putExtra("settings:show_fragment","com.android.settings.applications.RunningServices")
//                                    appContext.startActivity(intent)
//                                }
                            }
                        }
                        view.width = view.paint.measureText("${view.text}").roundToInt() + 10
                        if (view.width > MaxWidth) MaxWidth = view.width
                        LogUtils.i("$name: ${view.width}")
                    }
                }

                LogUtils.i("MaxWidth: $MaxWidth")
                LogUtils.i("Set mTxtMemoryViewGroup width to: $MaxWidth")
                mTxtMemoryViewGroup.layoutParams.width = MaxWidth + 10
            }
        }
//        hide the original view
        catchNoClass {
            findMethod("com.miui.home.recents.views.RecentsContainer") { name == "onFinishInflate" }.hookAfter {
                LogUtils.i("onFinishInflate")
                mTxtMemoryViewGroup = it.thisObject.getObjectAs("mTxtMemoryContainer")
//                mTxtMemoryViewGroup.setBackgroundColor(Color.BLUE)
                it.thisObject.putObject("mSeparatorForMemoryInfo", View(appContext))
                for (i in 0 until mTxtMemoryViewGroup.childCount) {
                    mTxtMemoryViewGroup.getChildAt(i).visibility = View.GONE
                }
                mTxtMemoryInfo1 = it.thisObject.getObjectAs("mTxtMemoryInfo1")
                TextViewMaps.apply {
                    TextViewList.forEach { name ->
                        run {
                            this[name] = TextView(appContext).apply {
                                LogUtils.i("Init view $name")
                                setTextColor(mTxtMemoryInfo1.textColors)
                                gravity = Gravity.START
                                textSize = 12f
                                marqueeRepeatLimit = -1
                                isSingleLine = true
                                maxLines = 1
                            }
                        }
                    }
                }
                MemoryLayout = LinearLayout(appContext).apply {
//                    setBackgroundColor(Color.RED)
//                    layoutParams.width = mTxtMemoryViewGroup.width
                    orientation = LinearLayout.VERTICAL
                }
                TextViewMaps.forEach { (name, view) ->
                    run {
                        LogUtils.i("Add view $name")
                        MemoryLayout.addView(view)
                    }
                }
                mTxtMemoryViewGroup.addView(MemoryLayout)
            }
        }
    }
//    #dc143c


}