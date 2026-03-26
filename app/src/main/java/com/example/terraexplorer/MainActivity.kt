package com.example.terraexplorer


import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.skyline.core.CoreServices
import com.skyline.teapi81.ApiException
import com.skyline.teapi81.ISGWorld
import com.skyline.teapi81.ITerrain3DArrow
import com.skyline.teapi81.ITerrainPolygon
import com.skyline.terraexplorer.TEApp
import com.skyline.terraexplorer.models.LocalBroadcastManager
import com.skyline.terraexplorer.models.UI
import com.skyline.terraexplorer.views.TEGLRenderer
import com.skyline.terraexplorer.views.TEView



class MainActivity : Activity() {

    private class ValueHolder {
        var ex: ApiException? = null
        var listener: ISGWorld.OnLoadFinishedListener? = null
        var OnLButtonUp: ISGWorld.OnLButtonUpListener? = null
        var OnRButtonUp: ISGWorld.OnRButtonUpListener? = null
        var OnLButtonDown: ISGWorld.OnLButtonDownListener? = null
        var OnRButtonDown: ISGWorld.OnRButtonDownListener? = null
        var OnMButtonUp: ISGWorld.OnMButtonUpListener? = null
        var OnMButtonDown: ISGWorld.OnMButtonDownListener? = null
        var OnFrame: ISGWorld.OnFrameListener? = null

        var OnObjectAction: ISGWorld.OnObjectActionListener? = null
        var OnLoadFinished: ISGWorld.OnLoadFinishedListener? = null
        //var OnObjectAction: ISGWorld.OnMButtonUpListener? = null

        var pol: ITerrainPolygon? = null
        var arrow: ITerrain3DArrow? = null;

    }

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            // 👉 Your repeated code here
            //ISGWorld.getInstance().command.Execute(2345);
            Log.d("SkylineSupport", ISGWorld.getInstance().project.name)
            Log.d("SkylineSupport", "Timer: Triggered every 5 seconds")

            // Schedule again in 10 seconds (10,000 ms)
            timerHandler.postDelayed(this, 5_000)
        }
    }

//
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Create a FrameLayout to hold both TEView and the button
    val frameLayout = android.widget.FrameLayout(this)
    
    // Add TEView to the FrameLayout
    val teView = TEView(baseContext)
    frameLayout.addView(teView)
    
    // Create and configure the button
    val button = android.widget.Button(this)
    button.text = "Load Project"
    
    // Set button layout parameters (positioned at top-center)
    val buttonParams = android.widget.FrameLayout.LayoutParams(
        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
    )
    buttonParams.gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
    buttonParams.topMargin = 40 // Add some margin from the top
    button.layoutParams = buttonParams
    
    // Set button click listener
    button.setOnClickListener { // button
        UI.runOnRenderThreadAsync {
            try {
                //ISGWorld.getInstance().getProject().Open("https://dev.skylinesoft.com/sg/Default/projects/8861645")
                ISGWorld.getInstance().command.Execute(2345);
                Log.d("MainActivity", "Project opened from button click")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error opening project: ${e.message}")
            }
        }
    }
    
    // Add button to the FrameLayout
    frameLayout.addView(button)
    
    // Set the FrameLayout as content view
    setContentView(frameLayout)
    
    TEApp.setMainActivityContext(this)
    TEApp.setApplicationContext(applicationContext)
    CoreServices.Init(this)

    LocalBroadcastManager.getInstance(this).registerReceiver(
        engineInitializedReceiver,
        IntentFilter(TEGLRenderer.ENGINE_INITIALIZED)
    )

    // 👉 Start 10-second repeating task
    //timerHandler.postDelayed(timerRunnable, 5_000)
}


    private val engineInitializedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onEngineInitialized()
            LocalBroadcastManager.getInstance(this@MainActivity).unregisterReceiver(this)
        }
    }
    private fun onEngineInitialized() {
        UI.runOnRenderThreadAsync {
            try {
                val density = resources.displayMetrics.density
                ISGWorld.getInstance().SetParam(8350, density)
                val scaleFactor = 0.75f
                ISGWorld.getInstance().SetParam(8370, scaleFactor)
                val vh = ValueHolder()



                //OnLoadFinishedListener
                vh.listener = ISGWorld.OnLoadFinishedListener { bSuccess ->
                    //ISGWorld.getInstance().removeOnLoadFinishedListener(vh.listener)
                    Log.d("SkylineSupport", ISGWorld.getInstance().project.name);

                }
                ISGWorld.getInstance().addOnLoadFinishedListener(vh.listener)



                var count = 0;

                //var pol: ITerrainPolygon? = null
                var pol: ITerrainPolygon
                //OnLButtonUpListener
                vh.OnLButtonUp = ISGWorld.OnLButtonUpListener { flags, x, y ->

                    /////edit
                    Log.d("NavonKotlin", "OnLButtonUp");
                    val wpi = ISGWorld.getInstance().window.PixelToWorld(x, y)
                    if (wpi.type == 4) {

                        pol = ISGWorld.getInstance().projectTree.GetObject(wpi.objectID)
                            .CastTo<ITerrainPolygon>(
                              ITerrainPolygon::class.java
                        )

                        ISGWorld.getInstance().projectTree.EndEdit();
                        ISGWorld.getInstance().projectTree.EditItemEx(pol.id, 4)

                    }
                    false;
                }

                //OnRButtonUpListener
                vh.OnRButtonUp = ISGWorld.OnRButtonUpListener { flags, x, y ->
                    //val wpi: IWorldPointInfo = ISGWorld.getInstance().window.PixelToWorld(x, y)
                    Log.d("NavonKotlin", "OnRButtonUp");
                    /*if (pol != null) {
                        val obj: Any? = 0
                        Log.d("NavonKotlin", "pol != null");
                        //ISGWorld.getInstance().command.Execute(2345, obj);

                    }*/

                    true;
                }

                //OnRButtonDownListener
                vh.OnRButtonDown = ISGWorld.OnRButtonDownListener { flags, x, y ->
                    //val wpi: IWorldPointInfo = ISGWorld.getInstance().window.PixelToWorld(x, y)
                    Log.d("NavonKotlin", "OnRButtonDown");

                    val wpi = ISGWorld.getInstance().window.PixelToWorld(x, y)

                    var arrow =
                        ISGWorld.getInstance().creator.Create3DArrow(wpi.position, 1500.0);
                    //arrow.lineStyle.pattern = 0xFF0C30FF.toInt()
                    ISGWorld.getInstance().navigate.FlyTo(arrow.id);


                    true;
                }

                //OnLButtonDownListener
                vh.OnLButtonDown = ISGWorld.OnLButtonDownListener { flags, x, y ->
                    //val wpi: IWorldPointInfo = ISGWorld.getInstance().window.PixelToWorld(x, y)
                    //ISGWorld.getInstance().projectTree.EndEdit();
                    Log.d("NavonKotlin", "OnLButtonDown");
                    /*if (pol != null) {
                        val obj: Any? = 0
                        Log.d("NavonKotlin", "pol != null");
                        ISGWorld.getInstance().command.Execute(2345, obj);

                    }*/

                    false;
                }

                //OnMButtonUpListener
                vh.OnMButtonUp = ISGWorld.OnMButtonUpListener { flags, x, y ->
                    //val wpi: IWorldPointInfo = ISGWorld.getInstance().window.PixelToWorld(x, y)
                    //ISGWorld.getInstance().projectTree.EndEdit();
                    Log.d("NavonKotlin", "OnMButtonUp");
                    /*if (pol != null) {
                        val obj: Any? = 0
                        Log.d("NavonKotlin", "pol != null");
                        ISGWorld.getInstance().command.Execute(2345, obj);

                    }*/

                    true;
                }

                //OnMButtonDownListener
                vh.OnMButtonDown = ISGWorld.OnMButtonDownListener { flags, x, y ->
                    //val wpi: IWorldPointInfo = ISGWorld.getInstance().window.PixelToWorld(x, y)
                    //ISGWorld.getInstance().projectTree.EndEdit();
                    Log.d("NavonKotlin", "OnMButtonDown");
                    /*if (pol != null) {
                        val obj: Any? = 0
                        Log.d("NavonKotlin", "pol != null");
                        ISGWorld.getInstance().command.Execute(2345, obj);

                    }*/

                    true;
                }

                //OnObjectActionListener
                vh.OnObjectAction = ISGWorld.OnObjectActionListener { ObjectID, Action ->
                    Log.d("action123", ObjectID.toString() +": " + Action.code.toString());



                }

                ISGWorld.getInstance().addOnLButtonUpListener(vh.OnLButtonUp) // tap up
                ISGWorld.getInstance().addOnRButtonUpListener(vh.OnRButtonUp)
                ISGWorld.getInstance().addOnLButtonDownListener(vh.OnLButtonDown) //4444 - tap down
                ISGWorld.getInstance().addOnRButtonDownListener(vh.OnRButtonDown) //long press
                ISGWorld.getInstance().addOnMButtonUpListener(vh.OnMButtonUp)
                ISGWorld.getInstance().addOnMButtonDownListener(vh.OnMButtonDown)
                //data class Params(val x: Int)
                val obj: Any? = 0


                //OnFrameListener
                vh.OnFrame = ISGWorld.OnFrameListener {

                    try {

                        //ISGWorld.getInstance().command.Execute(2345, obj);
                        Log.d("SkylineSupport", "2345");
                        //if (pol != null && count > 1) {
                        // do something
                        //   }
                    }
                    catch (e: Exception) {
                        Log.d("SkylineSupport", "onframe catch");
                    }
                }
                ISGWorld.getInstance().addOnFrameListener(vh.OnFrame)

                ISGWorld.getInstance().getProject().Open("https://dev.skylinesoft.com/sg/Default/projects/8861645");



            } catch (e: Exception) {
                // Handle exception
            }
        }
    }

}
