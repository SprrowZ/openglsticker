package com.benhero.glstudio.renderer

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import com.benhero.glstudio.R
import com.benhero.glstudio.base.BaseRenderer
import com.benhero.glstudio.camera.ICamera
import com.benhero.glstudio.util.BufferUtil
import com.benhero.glstudio.util.TextureHelper
import com.benhero.glstudio.util.VertexRotationUtil
import com.benhero.glstudio.watersign.*
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * 相机绘制
 *
 * @author Benhero
 */
class L11_1_CameraRenderer(context: Context, private val camera: ICamera) : BaseRenderer(context), SurfaceTexture.OnFrameAvailableListener {
    companion object {
        private const val VERTEX_SHADER = """
                attribute vec4 a_Position;
                attribute vec2 a_TexCoord;
                varying vec2 v_TexCoord;
                void main() {
                    v_TexCoord = a_TexCoord;
                    gl_Position = a_Position;
                }
                """
        private const val FRAGMENT_SHADER = """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                varying vec2 v_TexCoord;
                uniform samplerExternalOES u_TextureUnit;
                void main() {
                    gl_FragColor = texture2D(u_TextureUnit, v_TexCoord);
                }
                """

        private const val POSITION_COMPONENT_COUNT = 2

        private val POINT_DATA = floatArrayOf(
                -1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, -1.0f)

        /**
         * 纹理坐标
         */
        private val TEX_VERTEX = floatArrayOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f)

        /**
         * 纹理坐标中每个点占的向量个数
         */
        private const val TEX_VERTEX_COMPONENT_COUNT = 2
    }

    private var mVertexData: FloatBuffer

    private var aPositionLocation: Int = 0
    private var aTexCoordLocation:Int = 0
    private var uTextureUnitLocation: Int = 0
    private val mTexVertexBuffer: FloatBuffer
    private var textureId: Int = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var updateSurface = false
    private var isSwitchCamera = false

    //水印
    private var mWaterSign: WaterSignature? = null
    private var mSignTexId = 0
    //FBO
    private var fbo: FrameBuffer? = null
    private var mFBOFrameRect: FBOFrameRect? = null



    init {
        mVertexData = BufferUtil.createFloatBuffer(POINT_DATA)
        mTexVertexBuffer = BufferUtil.createFloatBuffer(TEX_VERTEX)
    }

    override fun onSurfaceCreated(glUnused: GL10, config: EGLConfig) {
        makeProgram(VERTEX_SHADER, FRAGMENT_SHADER)

        aPositionLocation = getAttrib("a_Position")
        // 纹理坐标索引
        aTexCoordLocation = getAttrib("a_TexCoord")
        uTextureUnitLocation = getUniform("u_TextureUnit")
        // 纹理数据
        createOESTextureId()
        //水印
        mWaterSign = WaterSignature()
        mWaterSign?.setShaderProgram(WaterSignSProgram())
        mSignTexId = TextureHelper.loadTexture(context, R.drawable.pikachu).textureId
        //fbo
        mFBOFrameRect = FBOFrameRect()
        fbo = FrameBuffer()

        mFBOFrameRect!!.setShaderProgram(WaterSignSProgram())

        mVertexData.position(0)
        GLES20.glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT,
                GLES20.GL_FLOAT, false, 0, mVertexData)
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        // 加载纹理坐标
        mTexVertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aTexCoordLocation, TEX_VERTEX_COMPONENT_COUNT, GLES20.GL_FLOAT, false, 0, mTexVertexBuffer)
        GLES20.glEnableVertexAttribArray(aTexCoordLocation)

        GLES20.glClearColor(0f, 0f, 0f, 1f)
        // 开启纹理透明混合，这样才能绘制透明图片
        GLES20.glEnable(GL10.GL_BLEND)
        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun createOESTextureId() {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)

        if (textureIds[0] == 0) {
            return
        }
        textureId = textureIds[0]

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureIds[0])

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

        // 创建SurfaceTexture、Surface，并绑定到Camera上，接收画面驱动回调
        surfaceTexture = SurfaceTexture(textureIds[0])
        surfaceTexture!!.setOnFrameAvailableListener(this)
        camera.setPreviewTexture(surfaceTexture)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        super.onSurfaceChanged(gl, width, height)
        GLES20.glViewport(0, 0, width, height)
        //updateVertex()
        fbo!!.setup(outputWidth, outputHeight)
    }

    private fun enableOESParams(){
        mVertexData.position(0)
        GLES20.glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT,
            GLES20.GL_FLOAT, false, 0, mVertexData)
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        // 加载纹理坐标
        mTexVertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aTexCoordLocation, TEX_VERTEX_COMPONENT_COUNT, GLES20.GL_FLOAT, false, 0, mTexVertexBuffer)
        GLES20.glEnableVertexAttribArray(aTexCoordLocation)
    }
    private fun disableOESParams(){
        GLES20.glDisableVertexAttribArray(aPositionLocation)
        GLES20.glDisableVertexAttribArray(aTexCoordLocation)
    }

    override fun onDrawFrame(glUnused: GL10) {
        //fbo start
        fbo!!.begin()

        GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glViewport(0, 0, outputWidth, outputHeight)
        surfaceTexture!!.updateTexImage()
        //使用項目
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        //十分重要，必不可少
        enableOESParams()
        GLES20.glUniform1i(uTextureUnitLocation, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, POINT_DATA.size / POSITION_COMPONENT_COUNT)
        //關掉
        disableOESParams()
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUseProgram(0)
        Log.e("Rye", "programe:$program")
        //设置水印
        GLES20.glViewport(0, 0, 288, 144)
        mWaterSign?.drawFrame(mSignTexId)
        GLES20.glViewport(0, 0, outputWidth, outputHeight)
        //fbo over
        fbo!!.end()
        GLES20.glViewport(0, 0, outputWidth, outputHeight)
        mFBOFrameRect!!.drawFrame(fbo!!.textureId)
    }

    /**
     * 根据相机方向更新顶点坐标
     */
    private fun updateVertex() {
        var array = VertexRotationUtil.rotate(POINT_DATA, camera.rotation)
        array = VertexRotationUtil.flip(array, camera.isFront)
        mVertexData = BufferUtil.createFloatBuffer(array)
        mVertexData.position(0)
        GLES20.glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT,
                GLES20.GL_FLOAT, false, 0, mVertexData)
        GLES20.glEnableVertexAttribArray(aPositionLocation)
    }

    /**
     * 有新的画面帧刷新时，通过SurfaceTexture的onFrameAvailable接口进行回调
     */
    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        updateSurface = true
    }

    fun switchCamera() {
        isSwitchCamera = true
    }
}
