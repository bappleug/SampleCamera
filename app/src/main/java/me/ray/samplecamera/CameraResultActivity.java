package me.ray.samplecamera;

import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.senab.photoview.PhotoView;

/**
 * Created by Ray on 2017/8/25.
 */

public class CameraResultActivity extends AppCompatActivity {

    @BindView(R.id.iv_photo)
    PhotoView photoView;
    @BindView(R.id.tv_info)
    TextView tvInfo;

    private Uri picUri;

    public static void start(Context context, Uri uri) {
        Intent intent = new Intent(context, CameraResultActivity.class);
        intent.putExtra("uri", uri);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_result);
        ButterKnife.bind(this);
        picUri = getIntent().getParcelableExtra("uri");
        DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.NONE;
        Glide.with(this).load(picUri).listener(new RequestListener<Uri, GlideDrawable>() {
            @Override
            public boolean onException(Exception e, Uri model, Target<GlideDrawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(GlideDrawable resource, Uri model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                int height = resource.getIntrinsicHeight();
                int width = resource.getIntrinsicWidth();
                String info = tvInfo.getText().toString();
                info = info + "size = " + height + "*" + width;
                tvInfo.setText(info);
                return false;
            }
        }).diskCacheStrategy(diskCacheStrategy).into(photoView);
        loadExifInfo();
    }

    private void loadExifInfo() {
        try {
            ExifInterface exif = new ExifInterface(picUri.getPath());
            String info = "exif.orientation = "
                    + transformOrientation(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1000))
                    + "\n";
            tvInfo.setText(info);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String transformOrientation(int orientation){
        String degree;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degree = "90";
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degree = "180";
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degree = "270";
                break;
            case ExifInterface.ORIENTATION_NORMAL:
                degree = "normal";
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                degree = "flip_horizontal";
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                degree = "flip_vertical";
                break;
            case ExifInterface.ORIENTATION_UNDEFINED:
                degree = "undefined";
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                degree = "transpose";
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                degree = "transverse";
                break;
            default:
                degree = "no info";
                break;
        }
        return degree;
    }
}
