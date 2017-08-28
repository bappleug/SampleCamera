package me.ray.samplecamera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.net.Uri;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import me.ray.samplecamera.camera.CameraView;
import me.ray.samplecamera.utils.ExecutorHelper;
import me.ray.samplecamera.utils.PathManager;
import me.ray.samplecamera.widgets.AppBar;

public class CameraActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener {

    PathManager pathManager;

    @BindView(R.id.appbar)
    AppBar appBar;
    @BindView(R.id.cameraView)
    CameraView cameraView;
    @BindView(R.id.btn_take_picture)
    ImageButton btnTakePicture;
    @BindView(R.id.btn_ok)
    ImageButton btnOk;
    @BindView(R.id.btn_cancel)
    ImageButton btnCancel;
    private ValueAnimator btnAnimation;
    private boolean animating;
    private String picPath;
    private Disposable openCameraDisposable;
    private CameraView.Callback callback;

    static {
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        pathManager = new PathManager(this);
        init();
    }

    protected void init() {
        btnTakePicture.setOnClickListener(v -> {
            if (animating) {
                return;
            }
            if (cameraView.isCameraOpened()) {
                cameraView.setOutputFilePath(pathManager.generateCameraImage().getPath());
                cameraView.takePicture();
            } else {
                Toast.makeText(this, "请在权限管理选择允许访问相机", Toast.LENGTH_SHORT).show();
            }
        });
        callback = new CameraView.Callback() {
            @Override
            public void onPictureTaken(CameraView cameraView, String picPath) {
                showBottomWaitToConfirm();
                CameraActivity.this.picPath = picPath;
            }

            @Override
            public void onPictureTaken(CameraView cameraView, Throwable throwable) {
                cameraView.resumePreview();
                Toast.makeText(CameraActivity.this, "拍照失败", Toast.LENGTH_SHORT).show();
                throwable.printStackTrace();
            }
        };
        cameraView.addCallback(callback);
        appBar.setNavigationOnClickListener(v -> onBackPressed());
        appBar.inflateMenu(R.menu.menu_camera);
        appBar.setOnMenuItemClickListener(this);
        btnOk.setOnClickListener(v -> {
            if (animating) {
                return;
            }
            goToAssemblePage();
        });
        btnCancel.setOnClickListener(v -> {
            if (animating) {
                return;
            }
            cameraView.resumePreview();
            showBottomTakeCamera();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (openCameraDisposable != null) {
            openCameraDisposable.dispose();
            openCameraDisposable = null;
        }
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request("android.permission.CAMERA")
                .subscribe(granted -> {
                    if (granted) {
                        openCameraDisposable = Schedulers.from(ExecutorHelper.BALANCE).createWorker().schedule(() -> {
                            cameraView.start();
                            if (!cameraView.isCameraOpened()) {
                                Toast.makeText(this, "请到权限管理选择允许访问相机", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "请到权限管理选择允许访问相机", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (openCameraDisposable != null) {
            openCameraDisposable.dispose();
            openCameraDisposable = null;
        }
        cameraView.stop();
        showBottomTakeCamera();
    }

    @Override
    protected void onDestroy() {
        if (btnAnimation != null) {
            btnAnimation.removeAllListeners();
            btnAnimation.cancel();
        }
        cameraView.removeCallback(callback);
        super.onDestroy();
    }

    private void showBottomTakeCamera() {
        final float middle = (btnOk.getLeft() + btnCancel.getLeft()) / 2.0f;
        final float distance = Math.abs(btnOk.getLeft() - middle);
        btnAnimation = ValueAnimator.ofFloat(1.0f, 0.0f);
        btnAnimation.setDuration(175);
        btnAnimation.setInterpolator(new LinearOutSlowInInterpolator());
        btnAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                animating = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animating = false;
                btnOk.setAlpha(1.0f);
                btnOk.setTranslationX(0);
                btnCancel.setAlpha(1.0f);
                btnCancel.setTranslationX(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                btnCancel.setVisibility(View.GONE);
                btnOk.setVisibility(View.GONE);
                btnTakePicture.setVisibility(View.VISIBLE);
                btnTakePicture.setAlpha(0.0f);
                btnTakePicture.animate()
                        .setDuration(200)
                        .setInterpolator(new FastOutLinearInInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                animating = false;
                            }
                        })
                        .alpha(1.0f)
                        .start();
            }
        });
        btnAnimation.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            btnOk.setAlpha(value);
            btnCancel.setAlpha(value);
            btnOk.setTranslationX(distance * (value - 1));
            btnCancel.setTranslationX(distance * (1 - value));
        });
        btnAnimation.start();
    }

    private void showBottomWaitToConfirm() {
        final float middle = (btnOk.getLeft() + btnCancel.getLeft()) / 2.0f;
        final float distance = Math.abs(btnOk.getLeft() - middle);
        btnTakePicture.setVisibility(View.GONE);
        btnAnimation = ValueAnimator.ofFloat(0.0f, 1.0f);
        btnAnimation.setDuration(225);
        btnAnimation.setInterpolator(new LinearOutSlowInInterpolator());
        btnAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                animating = true;
                btnCancel.setVisibility(View.VISIBLE);
                btnOk.setVisibility(View.VISIBLE);
                btnCancel.setTranslationX(distance);
                btnOk.setTranslationX(-distance);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animating = false;
                btnOk.setVisibility(View.GONE);
                btnCancel.setVisibility(View.GONE);
                btnTakePicture.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animating = false;
            }
        });
        btnAnimation.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            btnOk.setAlpha(value);
            btnCancel.setAlpha(value);
            btnOk.setTranslationX(distance * (value - 1));
            btnCancel.setTranslationX(distance * (1 - value));
        });
        btnAnimation.start();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (cameraView.getFacing() == CameraView.FACING_BACK) {
            cameraView.setFacing(CameraView.FACING_FRONT);
        } else {
            cameraView.setFacing(CameraView.FACING_BACK);
        }
        return true;
    }

    private void goToAssemblePage() {
        CameraResultActivity.start(this, Uri.fromFile(new File(picPath)));
    }
}
