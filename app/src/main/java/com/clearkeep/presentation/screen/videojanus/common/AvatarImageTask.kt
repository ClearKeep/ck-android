package com.clearkeep.presentation.screen.videojanus.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.widget.ImageView
import de.hdodenhof.circleimageview.CircleImageView
import java.lang.ref.WeakReference
import java.net.URL

class AvatarImageTask(avatarView: CircleImageView) : AsyncTask<String?, Void?, Bitmap?>() {
    var avatarViewWef: WeakReference<CircleImageView> = WeakReference(avatarView)

    override fun doInBackground(vararg urls: String?): Bitmap? {
        val avatarUrl = urls[0]
        var bmp: Bitmap? = null
        try {
            val `in` = URL(avatarUrl).openStream()
            bmp = BitmapFactory.decodeStream(`in`)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bmp
    }

    override fun onPostExecute(result: Bitmap?) {
        val img: ImageView? = avatarViewWef.get()
        if (img != null && result != null) {
            img.setImageBitmap(result)
        }
    }
}