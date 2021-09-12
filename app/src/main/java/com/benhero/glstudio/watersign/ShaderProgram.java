package com.benhero.glstudio.watersign;

import android.content.Context;
import android.opengl.GLES20;

import com.benhero.glstudio.util.ShaderHelper;


/**
 * Created by ZZR on 2017/2/20.
 */

public class ShaderProgram {

    protected final int programId;

    public ShaderProgram(String vertexShaderResourceStr,
                         String fragmentShaderResourceStr){
        programId = ShaderHelper.buildProgram(
                vertexShaderResourceStr,
                fragmentShaderResourceStr);
    }



    public void userProgram() {
        GLES20.glUseProgram(programId);
    }

    public int getShaderProgramId() {
        return programId;
    }

    public void deleteProgram() {
        GLES20.glDeleteProgram(programId);
    }
}
