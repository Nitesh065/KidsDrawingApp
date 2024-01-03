package com.example.drawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context:Context,attrs:AttributeSet) : View(context,attrs) {
    // An variable of CustomPath inner class to use it further.
    private var myDrawPath : CustomPath? = null
    // An instance of the Bitmap.
    private var myCanvasBitMap : Bitmap? = null
    // The Paint class holds the style and color information about how to draw geometries, text and bitmaps.
    private var myDrawPaint : Paint? = null
    // Instance of canvas paint view.
    private var myCanvasPaint : Paint? = null
    // A variable for stroke/brush size to draw on the canvas.
    private var myBrushSize : Float = 0.toFloat()
    private var color  = Color.BLACK
    private var canvas : Canvas? = null

    private var myPath = ArrayList<CustomPath>()
    private var myUndoPath = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

    fun onClickUndo(){
        if (myPath.size > 0){
            myUndoPath.add(myPath.removeAt(myPath.size -1))
            invalidate()
        }
    }
    fun onClickRedo(){
        if (myUndoPath.size > 0){
            myPath.add(myUndoPath.removeAt(myUndoPath.size -1))
            invalidate()
        }
    }

    private  fun setUpDrawing(){
        myDrawPaint = Paint()
        myDrawPath = CustomPath(color, myBrushSize)
        myDrawPaint!!.style = Paint.Style.STROKE
        myDrawPaint!!.strokeJoin = Paint.Join.ROUND
        myDrawPaint!!.strokeCap = Paint.Cap.ROUND
        myCanvasPaint = Paint(Paint.DITHER_FLAG)
//        myBrushSize = 20.toFloat()

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        myCanvasBitMap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(myCanvasBitMap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(myCanvasBitMap!!,0f,0f,myCanvasPaint)
        for (path in myPath){
            myDrawPaint!!.strokeWidth = path.brushThickness
            myDrawPaint!!.color = path.color
            canvas.drawPath(path,myDrawPaint!!)
        }
        if (!myDrawPath!!.isEmpty){
            myDrawPaint!!.strokeWidth = myDrawPath!!.brushThickness
            myDrawPaint!!.color = myDrawPath!!.color
            canvas.drawPath(myDrawPath!!,myDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y
        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                myDrawPath!!.color = color
                myDrawPath!!.brushThickness = myBrushSize
                myDrawPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        myDrawPath!!.moveTo(touchX,touchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if (touchX != null) {
                    if (touchY != null) {
                        myDrawPath!!.lineTo(touchX,touchY)
                    }
                }
            }
            MotionEvent.ACTION_UP ->{
                myPath.add(myDrawPath!!)
                myDrawPath = CustomPath(color,myBrushSize)
            }
            else -> false
        }
        invalidate()
        return true
    }

    fun setSizeOfBrush(newSize : Float){
        myBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newSize,resources.displayMetrics)
        myDrawPaint!!.strokeWidth = myBrushSize

    }

    fun setColor(newColor : String){
        color = Color.parseColor(newColor)
        myDrawPaint!!.color = color
    }

    internal inner class  CustomPath(var color :Int,var brushThickness:Float) : Path() {



    }


}