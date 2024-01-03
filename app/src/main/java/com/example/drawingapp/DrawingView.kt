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
    // A variable to hold a color of the stroke.
    private var color  = Color.BLACK

    /**
     * A variable for canvas which will be initialized later and used.
     *
     *The Canvas class holds the "draw" calls. To draw something, you need 4 basic components: A Bitmap to hold the pixels, a Canvas to host
     * the draw calls (writing into the bitmap), a drawing primitive (e.g. Rect,
     * Path, text, Bitmap), and a paint (to describe the colors and styles for the
     * drawing)
     */
    private var canvas : Canvas? = null

    // ArrayList for Paths
    private var myPath = ArrayList<CustomPath>()
    private var myUndoPath = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }
    /**
     * This function is called when the user selects the undo
     * command from the application. This function removes the
     * last stroke input by the user depending on the
     * number of times undo has been activated.
     */
    fun onClickUndo(){
        if (myPath.size > 0){
            myUndoPath.add(myPath.removeAt(myPath.size -1))
            invalidate()
        }
    }
    /**
     * This function is called when the user selects the redo
     * command from the application. This function adds the
     * last stroke input by the user depending on the
     * number of times undo has been activated.
     */
    fun onClickRedo(){
        if (myUndoPath.size > 0){
            myPath.add(myUndoPath.removeAt(myUndoPath.size -1))
            invalidate()
        }
    }
    /**
     * This method initializes the attributes of the
     * ViewForDrawing class.
     */
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

    /**
     * This method is called when a stroke is drawn on the canvas
     * as a part of the painting.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        /**
         * Draw the specified bitmap, with its top/left corner at (x,y), using the specified paint,
         * transformed by the current matrix.
         *
         *If the bitmap and canvas have different densities, this function will take care of
         * automatically scaling the bitmap to draw at the same density as the canvas.
         *
         * @param bitmap The bitmap to be drawn
         * @param left The position of the left side of the bitmap being drawn
         * @param top The position of the top side of the bitmap being drawn
         * @param paint The paint used to draw the bitmap (may be null)
         */
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

    /**
     * This method acts as an event listener when a touch
     * event is detected on the device.
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x // Touch event of X coordinate
        val touchY = event?.y // Touch event of Y coordinate
        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                myDrawPath!!.color = color
                myDrawPath!!.brushThickness = myBrushSize
                myDrawPath!!.reset()// Clear any lines and curves from the path, making it empty.
                if (touchX != null) {
                    if (touchY != null) {
                        myDrawPath!!.moveTo(touchX,touchY)// Set the beginning of the next contour to the point (x,y).
                    }
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if (touchX != null) {
                    if (touchY != null) {
                        myDrawPath!!.lineTo(touchX,touchY)// Add a line from the last point to the specified point (x,y).
                    }
                }
            }
            MotionEvent.ACTION_UP ->{
                myPath.add(myDrawPath!!) //Add when to stroke is drawn to canvas and added in the path arraylist
                myDrawPath = CustomPath(color,myBrushSize)
            }
            else -> false
        }
        invalidate()
        return true
    }

    /**
     * This method is called when either the brush or the eraser
     * sizes are to be changed. This method sets the brush/eraser
     * sizes to the new values depending on user selection.
     */
    fun setSizeOfBrush(newSize : Float){
        myBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newSize,resources.displayMetrics)
        myDrawPaint!!.strokeWidth = myBrushSize

    }

    /**
     * This function is called when the user desires a color change.
     * This functions sets the color of a store to selected color and able to draw on view using that color.
     *
     * @param newColor
     */
    fun setColor(newColor : String){
        color = Color.parseColor(newColor)
        myDrawPaint!!.color = color
    }

    internal inner class  CustomPath(var color :Int,var brushThickness:Float) : Path() {



    }


}