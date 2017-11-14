package com.benhero.glstudio;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.benhero.glstudio.base.BaseRenderer;
import com.benhero.glstudio.base.GLImageView;
import com.benhero.glstudio.l5.Architecture7;


public class MainActivity extends AppCompatActivity {

    private GLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setEGLContextClientVersion(2);
        BaseRenderer renderer = new Architecture7(this);
        GLImageView imageView = new GLImageView();
        imageView.setResId(R.drawable.tuzki);
        imageView.setX(400);
        imageView.setY(800);
        renderer.addGLImageView(imageView);

        GLImageView imageView2 = new GLImageView();
        imageView2.setResId(R.mipmap.ic_launcher);
        imageView2.setX(300);
        imageView2.setY(300);
        renderer.addGLImageView(imageView2);

        mGLSurfaceView.setRenderer(renderer);
        setContentView(mGLSurfaceView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }
}
