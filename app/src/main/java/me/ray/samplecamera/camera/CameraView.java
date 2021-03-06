/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.ray.samplecamera.camera;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;


import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import me.ray.samplecamera.R;
import me.ray.samplecamera.utils.ExecutorHelper;
import me.ray.samplecamera.utils.TimeLogger;

public class CameraView extends FrameLayout {

    /**
     * The camera device faces the opposite direction as the device's screen.
     */
    public static final int FACING_BACK = Constants.FACING_BACK;

    /**
     * The camera device faces the same direction as the device's screen.
     */
    public static final int FACING_FRONT = Constants.FACING_FRONT;
    private int mDesiredPictureWidth;
    private int mDesiredPictureHeight;

    /**
     * Direction the camera faces relative to device screen.
     */
    @IntDef({FACING_BACK, FACING_FRONT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Facing {
    }

    /**
     * Flash will not be fired.
     */
    public static final int FLASH_OFF = Constants.FLASH_OFF;

    /**
     * Flash will always be fired during snapshot.
     */
    public static final int FLASH_ON = Constants.FLASH_ON;

    /**
     * Constant emission of light during preview, auto-focus and snapshot.
     */
    public static final int FLASH_TORCH = Constants.FLASH_TORCH;

    /**
     * Flash will be fired automatically when required.
     */
    public static final int FLASH_AUTO = Constants.FLASH_AUTO;

    /**
     * Flash will be fired in red-eye reduction mode.
     */
    public static final int FLASH_RED_EYE = Constants.FLASH_RED_EYE;

    /**
     * The mode for for the camera device's flash control
     */
    @IntDef({FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE})
    public @interface Flash {
    }

    CameraViewImpl mImpl;

    private final CallbackBridge mCallbacks;

    private boolean mAdjustViewBounds;

    private final OrientationDetector mDisplayOrientationDetector;

    private String mOutputFilePath;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("WrongConstant")
    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) {
            mCallbacks = null;
            mDisplayOrientationDetector = null;
            return;
        }
        // Internal setup
        final PreviewImpl preview = createPreviewImpl(context);
        mCallbacks = new CallbackBridge();
        mImpl = new Camera1(mCallbacks, preview);
//        if (Build.VERSION.SDK_INT < 21) {
//            mImpl = new Camera1(mCallbacks, preview);
//        } else if (Build.VERSION.SDK_INT < 23) {
//            mImpl = new Camera2(mCallbacks, preview, context);
//        } else {
//            mImpl = new Camera2Api23(mCallbacks, preview, context);
//        }
        // Attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr,
                R.style.Widget_CameraView);
        mAdjustViewBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, false);
        setFacing(a.getInt(R.styleable.CameraView_facing, FACING_BACK));
        String aspectRatio = a.getString(R.styleable.CameraView_aspectRatio);
        if (aspectRatio != null) {
            setAspectRatio(AspectRatio.parse(aspectRatio));
        } else {
            setAspectRatio(Constants.DEFAULT_ASPECT_RATIO);
        }
        setAutoFocus(a.getBoolean(R.styleable.CameraView_autoFocus, true));
        setFlash(a.getInt(R.styleable.CameraView_flash, Constants.FLASH_AUTO));
        a.recycle();
        // Display orientation detector
        mDisplayOrientationDetector = new OrientationDetector(context) {
            @Override
            public void onOrientationChanged(int displayOrientation, int sensorOrientation) {
                mImpl.setOrientation(displayOrientation, sensorOrientation);
            }
        };
    }

    @NonNull
    private PreviewImpl createPreviewImpl(Context context) {
        PreviewImpl preview;
        preview = new SurfaceViewPreview(context, this);
//        if (Build.VERSION.SDK_INT < 14) {
//            preview = new SurfaceViewPreview(context, this);
//        } else {
//            preview = new TextureViewPreview(context, this);
//        }
        return preview;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mDisplayOrientationDetector.enable(ViewCompat.getDisplay(this));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            mDisplayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isInEditMode()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        // Handle android:adjustViewBounds
        if (mAdjustViewBounds) {
            if (!isCameraOpened()) {
                mCallbacks.reserveRequestLayoutOnOpen();
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int height = (int) (MeasureSpec.getSize(widthMeasureSpec) * ratio.toFloat());
                if (heightMode == MeasureSpec.AT_MOST) {
                    height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
                }
                super.onMeasure(widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int width = (int) (MeasureSpec.getSize(heightMeasureSpec) * ratio.toFloat());
                if (widthMode == MeasureSpec.AT_MOST) {
                    width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
                }
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        heightMeasureSpec);
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        // Measure the TextureView
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        AspectRatio ratio = getAspectRatio();
        if (mDisplayOrientationDetector.getLastKnownDisplayOrientation() % 180 == 0) {
            ratio = ratio.inverse();
        }
        assert ratio != null;
        if (height < width * ratio.getY() / ratio.getX()) {
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * ratio.getY() / ratio.getX(),
                            MeasureSpec.EXACTLY));
        } else {
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(height * ratio.getX() / ratio.getY(),
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.facing = getFacing();
        state.ratio = getAspectRatio();
        state.autoFocus = getAutoFocus();
        state.flash = getFlash();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setFacing(ss.facing);
        setAspectRatio(ss.ratio);
        setAutoFocus(ss.autoFocus);
        setFlash(ss.flash);
    }

    /**
     * Open a camera device and start showing camera preview. This is typically called from
     * {@link Activity#onResume()}.
     */
    public void start() {
//        if (!mImpl.start()) {
//            //store the state ,and restore this state after fall back o Camera1
//            Parcelable state=onSaveInstanceState();
//            // Camera2 uses legacy hardware layer; fall back to Camera1
//            mImpl = new Camera1(mCallbacks, createPreviewImpl(getContext()));
//            onRestoreInstanceState(state);
//            mImpl.start();
//        }
        mImpl.start();
    }

    /**
     * Stop camera preview and close the device. This is typically called from
     * {@link Activity#onPause()}.
     */
    public void stop() {
        mImpl.stop();
    }

    /**
     * @return {@code true} if the camera is opened.
     */
    public boolean isCameraOpened() {
        return mImpl.isCameraOpened();
    }

    /**
     * Add a new callback.
     *
     * @param callback The {@link Callback} to add.
     * @see #removeCallback(Callback)
     */
    public void addCallback(@NonNull Callback callback) {
        mCallbacks.add(callback);
    }

    /**
     * Remove a callback.
     *
     * @param callback The {@link Callback} to remove.
     * @see #addCallback(Callback)
     */
    public void removeCallback(@NonNull Callback callback) {
        mCallbacks.remove(callback);
    }

    /**
     * @param adjustViewBounds {@code true} if you want the CameraView to adjust its bounds to
     *                         preserve the aspect ratio of camera.
     * @see #getAdjustViewBounds()
     */
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        if (mAdjustViewBounds != adjustViewBounds) {
            mAdjustViewBounds = adjustViewBounds;
            requestLayout();
        }
    }

    /**
     * @return True when this CameraView is adjusting its bounds to preserve the aspect ratio of
     * camera.
     * @see #setAdjustViewBounds(boolean)
     */
    public boolean getAdjustViewBounds() {
        return mAdjustViewBounds;
    }

    /**
     * Chooses camera by the direction it faces.
     *
     * @param facing The camera facing. Must be either {@link #FACING_BACK} or
     *               {@link #FACING_FRONT}.
     */
    public void setFacing(@Facing int facing) {
        mImpl.setFacing(facing);
    }

    /**
     * Gets the direction that the current camera faces.
     *
     * @return The camera facing.
     */
    @Facing
    public int getFacing() {
        //noinspection WrongConstant
        return mImpl.getFacing();
    }

    /**
     * Gets all the aspect ratios supported by the current camera.
     */
    public Set<AspectRatio> getSupportedAspectRatios() {
        return mImpl.getSupportedAspectRatios();
    }

    /**
     * Sets the aspect ratio of camera.
     *
     * @param ratio The {@link AspectRatio} to be set.
     */
    public void setAspectRatio(@NonNull AspectRatio ratio) {
        if (mImpl.setAspectRatio(ratio)) {
            requestLayout();
        }
    }

    /**
     * Gets the current aspect ratio of camera.
     *
     * @return The current {@link AspectRatio}. Can be {@code null} if no camera is opened yet.
     */
    @Nullable
    public AspectRatio getAspectRatio() {
        return mImpl.getAspectRatio();
    }

    /**
     * Enables or disables the continuous auto-focus mode. When the current camera doesn't support
     * auto-focus, calling this method will be ignored.
     *
     * @param autoFocus {@code true} to enable continuous auto-focus mode. {@code false} to
     *                  disable it.
     */
    public void setAutoFocus(boolean autoFocus) {
        mImpl.setAutoFocus(autoFocus);
    }

    /**
     * Returns whether the continuous auto-focus mode is enabled.
     *
     * @return {@code true} if the continuous auto-focus mode is enabled. {@code false} if it is
     * disabled, or if it is not supported by the current camera.
     */
    public boolean getAutoFocus() {
        return mImpl.getAutoFocus();
    }

    /**
     * Sets the flash mode.
     *
     * @param flash The desired flash mode.
     */
    public void setFlash(@Flash int flash) {
        mImpl.setFlash(flash);
    }

    /**
     * Gets the current flash mode.
     *
     * @return The current flash mode.
     */
    @Flash
    public int getFlash() {
        //noinspection WrongConstant
        return mImpl.getFlash();
    }

    public void setDesiredPictureSize(int desiredPictureWidth, int desiredPictureHeight) {
        mDesiredPictureHeight = desiredPictureHeight;
        mDesiredPictureWidth = desiredPictureWidth;
        mImpl.setDesiredPictureSize(desiredPictureWidth, desiredPictureHeight);
    }

    /**
     * Take a picture. The result will be returned to
     * .
     */
    public void takePicture() {
        mImpl.takePicture();
    }

    public void resumePreview() {
        mImpl.resumePreview();
    }

    public void setOutputFilePath(String outputFilePath) {
        this.mOutputFilePath = outputFilePath;
    }

    private String generateOutputFilePath() {
        return getContext().getExternalCacheDir() + "IMG_" + new Date().getTime() + ".jpg";
    }

    private class CallbackBridge implements CameraViewImpl.Callback {

        private final ArrayList<Callback> mCallbacks = new ArrayList<>();

        private boolean mRequestLayoutOnOpen;
        private DisposableSingleObserver<String> savePictureObserver;

        CallbackBridge() {
        }

        public void add(Callback callback) {
            mCallbacks.add(callback);
        }

        public void remove(Callback callback) {
            mCallbacks.remove(callback);
        }

        @Override
        public void onCameraOpened() {
            if (mRequestLayoutOnOpen) {
                mRequestLayoutOnOpen = false;
                requestLayout();
            }
            for (Callback callback : mCallbacks) {
                callback.onCameraOpened(CameraView.this);
            }
        }

        @Override
        public void onCameraClosed() {
            if (savePictureObserver != null && !savePictureObserver.isDisposed()) {
                savePictureObserver.dispose();
                savePictureObserver = null;
            }
            for (Callback callback : mCallbacks) {
                callback.onCameraClosed(CameraView.this);
            }
        }

        @Override
        public void onPictureTaken(final byte[] data, int orientation) {
            savePictureObserver = Single.fromCallable(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    TimeLogger.start("picture decode");

                    //prepare img
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    options.inPreferQualityOverSpeed = false;
                    BitmapFactory.decodeByteArray(data, 0, data.length, options);
                    int preHeight = options.outHeight;
                    int sampleSize = 1;
                    while (preHeight / mDesiredPictureHeight >= 2) {
                        sampleSize = sampleSize << 1;
                        preHeight = preHeight / 2;
                    }
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = sampleSize;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                    TimeLogger.stop("picture decode");

                    //rotate and flip
                    Matrix matrix = new Matrix();
                    if (getFacing() == CameraView.FACING_FRONT) {
                        float[] mirrorY = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
                        matrix.setValues(mirrorY);
                    }
                    matrix.preRotate(orientation);
                    TimeLogger.start("picture matrix flip");
                    Bitmap flipBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    TimeLogger.stop("picture matrix flip");

//                                TimeLogger.start("picture opencv rotate");
//                    Mat originMat = new Mat();
//                    Utils.bitmapToMat(bitmap, originMat);
//
////                    Mat matFlipResult = new Mat();
////                    Core.flip(originMat, matFlipResult, 1);
//
//                    Mat matrixRotate = Imgproc.getRotationMatrix2D(new Point(originMat.cols() / 2, originMat.rows() / 2), orientation, 1);
//                    Mat matRotateResult = new Mat();
//
//                    Imgproc.warpAffine(originMat, matRotateResult, matrixRotate, originMat.size());

//                    Bitmap resultBitmap = Bitmap.createBitmap(matRotateResult.width(), matRotateResult.height(), Bitmap.Config.ARGB_8888);
//                    Utils.matToBitmap(matRotateResult, resultBitmap);
//                    TimeLogger.stop("picture opencv rotate");

                    //compress and output
                    TimeLogger.start("picture compress");
                    String path = generateOutputFilePath();
                    File file = new File(path);
                    FileOutputStream fos = new FileOutputStream(file);
                    flipBitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos);
                    flipBitmap.recycle();
//                    flipBitmap.recycle();
                    bitmap.recycle();
                    fos.close();
//                    if(getFacing() == CameraView.FACING_FRONT){
                    setExifOrientation(path, orientation);
//                    }
                    TimeLogger.stop("picture compress");
                    return path;
                }
            }).subscribeOn(Schedulers.from(ExecutorHelper.LOW))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<String>() {
                        @Override
                        public void onSuccess(@io.reactivex.annotations.NonNull String s) {
                            for (Callback callback : mCallbacks) {
                                callback.onPictureTaken(CameraView.this, s);
                            }
                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                            for (Callback callback : mCallbacks) {
                                callback.onPictureTaken(CameraView.this, e);
                            }
                        }
                    });
        }

        public void reserveRequestLayoutOnOpen() {
            mRequestLayoutOnOpen = true;
        }
    }

    private void setExifOrientation(String path, int degree) {
        degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            // 修正图片的旋转角度，设置其不旋转。这里也可以设置其旋转的角度，可以传值过去，
            // 例如旋转90度，传值ExifInterface.ORIENTATION_ROTATE_90，需要将这个值转换为String类型的
            int exifOrientation = 1;
            switch (degree) {
                case 0:
                    exifOrientation = ExifInterface.ORIENTATION_NORMAL;
                    break;
                case 90:
                    exifOrientation = ExifInterface.ORIENTATION_ROTATE_90;
                    break;
                case 180:
                    exifOrientation = ExifInterface.ORIENTATION_ROTATE_180;
                    break;
                case 270:
                    exifOrientation = ExifInterface.ORIENTATION_ROTATE_270;
                    break;
            }
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(exifOrientation));
            exifInterface.saveAttributes();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected static class SavedState extends BaseSavedState {

        @Facing
        int facing;

        AspectRatio ratio;

        boolean autoFocus;

        @Flash
        int flash;

        @SuppressWarnings("WrongConstant")
        public SavedState(Parcel source, ClassLoader loader) {
            super(source);
            facing = source.readInt();
            ratio = source.readParcelable(loader);
            autoFocus = source.readByte() != 0;
            flash = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(facing);
            out.writeParcelable(ratio, 0);
            out.writeByte((byte) (autoFocus ? 1 : 0));
            out.writeInt(flash);
        }

        public static final Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }

        });

    }

    /**
     * Callback for monitoring events about {@link CameraView}.
     */
    @SuppressWarnings("UnusedParameters")
    public abstract static class Callback {

        /**
         * Called when camera is opened.
         *
         * @param cameraView The associated {@link CameraView}.
         */
        public void onCameraOpened(CameraView cameraView) {
        }

        /**
         * Called when camera is closed.
         *
         * @param cameraView The associated {@link CameraView}.
         */
        public void onCameraClosed(CameraView cameraView) {
        }

        /**
         * Called when a picture is taken.
         *
         * @param cameraView The associated {@link CameraView}.
         * @param picPath    JPEG data.
         */
        public abstract void onPictureTaken(CameraView cameraView, String picPath);

        public abstract void onPictureTaken(CameraView cameraView, Throwable throwable);
    }

}
