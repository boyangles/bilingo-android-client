package info.bilingo.bilingoclientapp

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.support.v7.app.AppCompatActivity
import android.support.design.widget.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*

abstract class BaseCameraActivity : AppCompatActivity(), View.OnClickListener {

    val sheetBehavior by lazy {
        //BottomSheetBehavior.from(layout_bottom_sheet)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //btnRetry.setOnClickListener {
        //    if (cameraView.visibility == View.VISIBLE) showPreview() else hidePreview()
        //}
        //fab.setOnClickListener(this)
        //sheetBehavior.peekHeight = 224
        //sheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        //    override fun onStateChanged(bottomSheet: View, newState: Int) {}
        //    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        //})
    }


    override fun onResume() {
        super.onResume()
        //cameraView.start()
    }

    override fun onPause() {
        //cameraView.stop()
        super.onPause()
    }

    protected fun showPreview() {
        //framePreview.visibility = View.VISIBLE
        //cameraView.visibility = View.GONE
    }

    protected fun hidePreview() {
        //framePreview.visibility = View.GONE
        //cameraView.visibility = View.VISIBLE
    }
}