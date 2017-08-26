package me.ray.samplecamera.widgets;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v7.widget.TintTypedArray;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.widget.TextView;

import me.ray.samplecamera.R;


/**
 * Created by Asura on 2017/6/19.
 */

public class AppBar extends Toolbar {

    private TextView tvTitle;
    private int titleTextApperance;
    private int color;

    public AppBar(Context context) {
        this(context, null);
    }

    public AppBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.toolbarStyle);
    }

    public AppBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(getContext(), attrs,
                R.styleable.Toolbar, defStyleAttr, 0);
        titleTextApperance = a.getResourceId(R.styleable.Toolbar_titleTextAppearance, R.style.TextAppearance_AppCompat_Widget_ActionBar_Title);
        color = a.getColor(R.styleable.Toolbar_titleTextColor, android.R.attr.textColorPrimary);
        a.recycle();
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.appbar_title, this);
        tvTitle = (TextView) findViewById(R.id.tv_appbar_title);
        tvTitle.setTextAppearance(context, titleTextApperance);
        tvTitle.setTextColor(color);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(null);
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
    }

    @Override
    public void setTitleTextAppearance(Context context, @StyleRes int resId) {
        super.setTitleTextAppearance(context, resId);
        if (tvTitle != null) {
            tvTitle.setTextAppearance(context, resId);
        }
    }
}
