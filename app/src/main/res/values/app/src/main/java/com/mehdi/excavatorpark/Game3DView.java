package com.mehdi.excavatorpark;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Game3DView extends GLSurfaceView {

    private final Renderer renderer;

    public Game3DView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        renderer = new Renderer();
        setRenderer(renderer);

        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            renderer.angleY += 0.5f;
        }
        return true;
    }

    private static class Renderer implements GLSurfaceView.Renderer {

        private float angleY = 0;

        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];

        private FloatBuffer ground;
        private FloatBuffer cabin;
        private FloatBuffer arm;

        private int program;

        @Override
        public void onSurfaceCreated(
                javax.microedition.khronos.egl.EGLConfig config) {

            GLES20.glClearColor(0.55f, 0.75f, 0.90f, 1.0f);

            String vertexShaderCode =
                    "attribute vec4 vPosition;" +
                    "uniform mat4 uMVPMatrix;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

            String fragmentShaderCode =
                    "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

            int vertexShader = loadShader(
                    GLES20.GL_VERTEX_SHADER,
                    vertexShaderCode);

            int fragmentShader = loadShader(
                    GLES20.GL_FRAGMENT_SHADER,
                    fragmentShaderCode);

            program = GLES20.glCreateProgram();

            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);

            GLES20.glLinkProgram(program);

            ground = createBuffer(new float[]{
                    -6,0,-6,
                     6,0,-6,
                     6,0, 6,
                    -6,0, 6
            });

            cabin = createBuffer(new float[]{
                    -1,0,-1,
                     1,0,-1,
                     1,2,-1,
                    -1,2,-1,

                    -1,0, 1,
                     1,0, 1,
                     1,2, 1,
                    -1,2, 1
            });

            arm = createBuffer(new float[]{
                    0,1,0,
                    0.7f,1.5f,0,
                    1.8f,0.8f,0
            });
        }

        @Override
        public void onSurfaceChanged(
                javax.microedition.khronos.opengles.GL10 gl,
                int width,
                int height) {

            GLES20.glViewport(0, 0, width, height);

            float ratio = (float) width / height;

            Matrix.frustumM(
                    projection,
                    0,
                    -ratio,
                    ratio,
                    -1,
                    1,
                    2,
                    20);
        }

        @Override
        public void onDrawFrame(
                javax.microedition.khronos.opengles.GL10 gl) {

            GLES20.glClear(
                    GLES20.GL_COLOR_BUFFER_BIT |
                    GLES20.GL_DEPTH_BUFFER_BIT);

            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            Matrix.setLookAtM(
                    view,
                    0,
                    7,5,8,
                    0,1,0,
                    0,1,0);

            drawObject(
                    ground,
                    GLES20.GL_TRIANGLE_FAN,
                    new float[]{0.25f,0.55f,0.18f,1});

            Matrix.setIdentityM(model,0);

            Matrix.rotateM(
                    model,
                    0,
                    angleY,
                    0,
                    1,
                    0);

            drawObject(
                    cabin,
                    GLES20.GL_TRIANGLE_STRIP,
                    new float[]{1.0f,0.65f,0.05f,1});

            drawObject(
                    arm,
                    GLES20.GL_LINE_STRIP,
                    new float[]{0.9f,0.45f,0.05f,1});
        }

        private void drawObject(
                FloatBuffer buffer,
                int mode,
                float[] color) {

            int positionHandle =
                    GLES20.glGetAttribLocation(
                            program,
                            "vPosition");

            int colorHandle =
                    GLES20.glGetUniformLocation(
                            program,
                            "vColor");

            int matrixHandle =
                    GLES20.glGetUniformLocation(
                            program,
                            "uMVPMatrix");

            Matrix.multiplyMM(
                    mvp,
                    0,
                    view,
                    0,
                    model,
                    0);

            Matrix.multiplyMM(
                    mvp,
                    0,
                    projection,
                    0,
                    mvp,
                    0);

            GLES20.glUseProgram(program);

            GLES20.glEnableVertexAttribArray(positionHandle);

            buffer.position(0);

            GLES20.glVertexAttribPointer(
                    positionHandle,
                    3,
                    GLES20.GL_FLOAT,
                    false,
                    12,
                    buffer);

            GLES20.glUniform4fv(
                    colorHandle,
                    1,
                    color,
                    0);

            GLES20.glUniformMatrix4fv(
                    matrixHandle,
                    1,
                    false,
                    mvp,
                    0);

            GLES20.glDrawArrays(
                    mode,
                    0,
                    buffer.capacity() / 3);

            GLES20.glDisableVertexAttribArray(positionHandle);
        }

        private FloatBuffer createBuffer(float[] data) {

            ByteBuffer bb =
                    ByteBuffer.allocateDirect(
                            data.length * 4);

            bb.order(
                    ByteOrder.nativeOrder());

            FloatBuffer buffer =
                    bb.asFloatBuffer();

            buffer.put(data);
            buffer.position(0);

            return buffer;
        }

        private int loadShader(
                int type,
                String code) {

            int shader =
                    GLES20.glCreateShader(type);

            GLES20.glShaderSource(
                    shader,
                    code);

            GLES20.glCompileShader(shader);

            return shader;
        }
    }
}
