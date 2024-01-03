package com.example.drawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private var drawingView :DrawingView? = null
    private var myImageButtonCurrentPaint :ImageButton? = null
    var customProgressDialog: Dialog? = null

    private var openGalleryLaucher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if (result.resultCode == RESULT_OK && result.data!=null){
                val imageBackground: ImageView = findViewById(R.id.backgroundImage)
                imageBackground.setImageURI(result.data?.data)
            }
        }
    private var requestGalleryPermission :ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permession ->
        permession.entries.forEach{
            val permissionsName = it.key
            val isGranted = it.value

            if (isGranted){
                    Toast.makeText(this,"Permission granted for storage",Toast.LENGTH_SHORT).show()
                val pickIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLaucher.launch(pickIntent)
                }
            else{
                if(permissionsName == Manifest.permission.READ_MEDIA_IMAGES){
                    Toast.makeText(this,"Permission denied for Storage",Toast.LENGTH_SHORT).show()
                }
            }
        }


    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawingView)
        drawingView!!.setSizeOfBrush(20.toFloat())


        val linerLayoutPaintColor : LinearLayout= findViewById(R.id.colors_pallet)

            myImageButtonCurrentPaint = linerLayoutPaintColor[4] as ImageButton
        myImageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable( this,R.drawable.pallet_selected))

        val brush :ImageButton = findViewById(R.id.brush)
        brush.setOnClickListener {showBrushSizeChooserDialog() }

        val undo :ImageButton = findViewById(R.id.btnUndo)
        undo.setOnClickListener {drawingView?.onClickUndo() }

        val redo :ImageButton = findViewById(R.id.btnRedo)
        redo.setOnClickListener {drawingView?.onClickRedo() }

        val btnGallery :ImageButton = findViewById(R.id.galleryButton)
        btnGallery.setOnClickListener {
            galleryPermission()
        }

        val btnSave: ImageButton = findViewById(R.id.save)
        btnSave.setOnClickListener {
            if(isReadStorageAllowed()){
                showProgressBar()
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.drawingViewContainer)
                    saveBitMapFile(getBitMapFromView(flDrawingView))
                }
            }
        }

    }
    private fun showBrushSizeChooserDialog(){
       var  brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size:")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.small_brush)
        val medium : ImageButton = brushDialog.findViewById(R.id.medium_brush)
        val large : ImageButton = brushDialog.findViewById(R.id.large_brush)
        smallBtn.setOnClickListener {
            drawingView!!.setSizeOfBrush(10.toFloat())
            brushDialog.dismiss()
        }
        medium.setOnClickListener{
            drawingView?.setSizeOfBrush(20.toFloat())
            brushDialog.dismiss()
        }
        large.setOnClickListener{
            drawingView?.setSizeOfBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }
    fun paintClicked(view : View){
        if(view !== myImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView!!.setColor(colorTag)
            imageButton.setImageDrawable(ContextCompat.getDrawable( this,R.drawable.pallet_selected))

           myImageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable( this,R.drawable.pallet_normal))

            myImageButtonCurrentPaint = view

        }
    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun galleryPermission(){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)){

                showRationDialog(
                    title = "Drawing App",
                    message = "Kids drawing app needs permission for storage"
                )

            }
            else{

                requestGalleryPermission.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }
        }
        catch (e:Exception){
            e.printStackTrace()
            Log.e("error","$e")
        }

    }
    private fun showRationDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Cancle") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun getBitMapFromView(view: View): Bitmap{
        val returnedBitMap = Bitmap.createBitmap(view.width,
            view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitMap)
        val bgDrawable = view.background

        if (bgDrawable != null){
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitMap

    }
    private suspend fun saveBitMapFile(mBitmap: Bitmap?): String{
        var result = ""

        withContext(Dispatchers.IO){
            if (mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    val file = File(externalCacheDir?.absoluteFile.toString()
                                + File.separator + "DrawingApp_" + System.currentTimeMillis()/1000 + ".png")

                    val fileOutput = FileOutputStream(file)
                    fileOutput.write(bytes.toByteArray())
                    fileOutput.close()

                    result = file.absolutePath

                    runOnUiThread {
                        cancelProgressBar()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,
                                "File saved successfully: " +
                                        "$result",Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        }
                        else{
                            Toast.makeText(this@MainActivity,
                                "Something went wrong while saving the file",
                                        Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e:Exception){
                    result = ""
                    e.printStackTrace()

                }
            }
        }
        return result
    }

    private fun showProgressBar(){
        customProgressDialog = Dialog(this)

        customProgressDialog?.setContentView(R.layout.dialog_coustom_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressBar(){
        if (customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }

    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))
        }
    }
}