package com.clearkeep.screen.auth.login

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.widget.ToggleButton
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.content.res.TypedArray
import android.util.Log
import com.clearkeep.R
import kotlin.math.log2

@SuppressLint("AppCompatCustomView")
class ToggleImageButton  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ToggleButton(context, attrs, defStyle){
    private var drawableOn: Drawable? = null
    private var drawableOff: Drawable? = null

    init {
        obtainStyledAttributes(context, attrs)
        removeText()
    }

    private fun removeText() {
        this.textOn = ""
        this.textOff = ""
        super.setChecked(!this.isChecked)
    }

    private fun obtainStyledAttributes(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ToggleImageButton, 0, 0)
        drawableOn = a.getDrawable(R.styleable.ToggleImageButton_tib_drawable_on)
        drawableOff = a.getDrawable(R.styleable.ToggleImageButton_tib_drawable_off)
        a.recycle()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        syncDrawableState()
    }

    @SuppressLint("ResourceType")
    private fun syncDrawableState() {
        val checked = isChecked

        if (checked && drawableOn != null) {

            setBackgroundDrawable(drawableOn)
            Log.e("antx","setOnClickListener 2 drawableOn ${checked}")

        } else if (!checked && drawableOff != null) {
            Log.e("antx","setOnClickListener 2 drawableOff ${checked}")

            setBackgroundDrawable(drawableOff)
        }
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        Log.e("antx","setOnClickListener 2 ${checked}")
        syncDrawableState()
    }

    fun setDrawableOn(drawableOn: Drawable?) {
        this.drawableOn = drawableOn
        syncDrawableState()
    }

    fun setDrawableOff(drawableOff: Drawable?) {
        this.drawableOff = drawableOff
        syncDrawableState()
    }
}