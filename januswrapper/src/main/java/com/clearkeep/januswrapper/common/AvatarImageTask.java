package com.clearkeep.januswrapper.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import de.hdodenhof.circleimageview.CircleImageView;

public class AvatarImageTask extends AsyncTask<String, Void, Bitmap> {

    WeakReference<CircleImageView> avatarViewWef;

    public AvatarImageTask(CircleImageView avatarView) {
        this.avatarViewWef = new WeakReference<>(avatarView);
    }

    protected Bitmap doInBackground(String... urls) {
        String avatarUrl = urls[0];
        Bitmap bmp = null;
        try {
            InputStream in = new URL(avatarUrl).openStream();
            bmp = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bmp;
    }
    protected void onPostExecute(Bitmap result) {
        ImageView img = avatarViewWef.get();
        if (img != null && result != null) {
            img.setImageBitmap(result);
        }
    }
}
