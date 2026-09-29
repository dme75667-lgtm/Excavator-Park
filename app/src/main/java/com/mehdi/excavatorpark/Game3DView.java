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

        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {

            float w = getWidth();
            float h = getHeight();

            // يسار
            if (x < w * 0.25f && y > h * 0.65f) {
                renderer.moveLeft = true;
                renderer.moveRight = false;
            }

            // يمين
            else if (x > w * 0.75f && y > h * 0.65f) {
                renderer.moveRight = true;
                renderer.moveLeft = false;
            }

            // أمام
            else if (y < h * 0.45f) {
                renderer.moveForward = true;
                renderer.moveBackward = false;
            }

            // خلف
            else {
                renderer.moveBackward = true;
                renderer.moveForward = false;
            }

            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {

            renderer.moveLeft = false;
            renderer.moveRight = false;
            renderer.moveForward = false;
            renderer.moveBackward = false;

            return true;
        }

        return true;
    }

    private static class Renderer implements GLSurfaceView.Renderer {

        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];

        private FloatBuffer cubeBuffer;

        private int program;
        private int positionHandle;
        private int colorHandle;
        private int mvpHandle;

        private float excavatorX = 0;
        private float excavatorZ = 0;

        boolean moveLeft;
        boolean moveRight;
        boolean moveForward;
        boolean moveBackward;

        private final float[] cubeVertices = {

                // أمام
                -0.5f, -0.5f,  0.5f,
                 0.5f, -0.5f,  0.5f,
                 0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,

                // خلف
                -0.5f, -0.5f, -0.5f,
                -0.5f,  0.5f, -0.5f,
                 0.5f,  0.5f, -0.5f,
                 0.5f, -0.5f, -0.5f,

                // يسار
                -0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f, -0.5f,

                // يمين
                 0.5f, -0.5f, -0.5f,
                 0.5f,  0.5f, -0.5f,
                 0.5f,  0.5f,  0.5f,
                 0.5f, -0.5f,  0.5f,

                // فوق
                -0.5f,  0.5f, -0.5f,
                -0.5f,  0.5f,  0.5f,
                 0.5f,  0.5f,  0.5f,
                 0.5f,  0.5f, -0.5f,

                // تحت
                -0.5f, -0.5f, -0.5f,
                 0.5f, -0.5f, -0.5f,
                 0.5f, -0.5f,  0.5f,
                -0.5f, -0.5f,  0.5f
        };

        @Override
        public void onSurfaceCreated(
                javax.microedition.khronos.egl.EGLConfig config) {

            GLES20.glClearColor(
                    0.45f,
                    0.70f,
                    0.85f,
                    1.0f
            );

            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            cubeBuffer = ByteBuffer
                    .allocateDirect(cubeVertices.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            cubeBuffer.put(cubeVertices);
            cubeBuffer.position(0);

            String vertexShader =
                    "attribute vec4 aPosition;" +
                    "uniform mat4 uMVP;" +
                    "void main() {" +
                    "gl_Position = uMVP * aPosition;" +
                    "}";

            String fragmentShader =
                    "precision mediump float;" +
                    "uniform vec4 uColor;" +
                    "void main() {" +
                    "gl_FragColor = uColor;" +
                    "}";

            int vertex = loadShader(
                    GLES20.GL_VERTEX_SHADER,
                    vertexShader
            );

            int fragment = loadShader(
                    GLES20.GL_FRAGMENT_SHADER,
                    fragmentShader
            );

            program = GLES20.glCreateProgram();

            GLES20.glAttachShader(program, vertex);
            GLES20.glAttachShader(program, fragment);

            GLES20.glLinkProgram(program);

            positionHandle =
                    GLES20.glGetAttribLocation(program, "aPosition");

            colorHandle =
                    GLES20.glGetUniformLocation(program, "uColor");

            mvpHandle =
                    GLES20.glGetUniformLocation(program, "uMVP");
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
                    100
            );
        }

        @Override
        public void onDrawFrame(
                javax.microedition.khronos.opengles.GL10 gl) {

            GLES20.glClear(
                    GLES20.GL_COLOR_BUFFER_BIT |
                    GLES20.GL_DEPTH_BUFFER_BIT
            );

            updateMovement();

            // الكاميرا خلف الحفارة
            Matrix.setLookAtM(
                    view,
                    0,
                    excavatorX,
                    5.5f,
                    excavatorZ + 9f,

                    excavatorX,
                    0,
                    excavatorZ,

                    0,
                    1,
                    0
            );

            drawGround();

            drawParkRoad();

            drawExcavator();

            drawCoinArea();
        }

        private void updateMovement() {

            float speed = 0.08f;

            if (moveLeft) {
                excavatorX -= speed;
            }

            if (moveRight) {
                excavatorX += speed;
            }

            if (moveForward) {
                excavatorZ -= speed;
            }

            if (moveBackward) {
                excavatorZ += speed;
            }
        }

        private void drawGround() {

            drawCube(
                    0,
                    -0.5f,
                    0,
                    20,
                    1,
                    20,
                    0.20f,
                    0.45f,
                    0.18f,
                    1
            );
        }

        private void drawParkRoad() {

            drawCube(
                    0,
                    0.02f,
                    0,
                    5,
                    0.12f,
                    20,
                    0.35f,
                    0.25f,
                    0.15f,
                    1
            );
        }

        private void drawExcavator() {

            // جسم الحفارة
            drawCube(
                    excavatorX,
                    0.65f,
                    excavatorZ,
                    2.2f,
                    0.8f,
                    2.5f,
                    1.0f,
                    0.55f,
                    0.02f,
                    1
            );

            // الكابينة
            drawCube(
                    excavatorX - 0.35f,
                    1.45f,
                    excavatorZ,
                    1.1f,
                    1.0f,
                    1.0f,
                    0.08f,
                    0.12f,
                    0.13f,
                    1
            );

            // الذراع الأول
            drawCube(
                    excavatorX + 1.0f,
                    1.35f,
                    excavatorZ - 0.35f,
                    0.35f,
                    2.2f,
                    0.35f,
                    1.0f,
                    0.55f,
                    0.02f,
                    1
            );

            // الدلو
            drawCube(
                    excavatorX + 1.0f,
                    0.35f,
                    excavatorZ - 1.5f,
                    1.0f,
                    0.7f,
                    0.9f,
                    0.75f,
                    0.40f,
                    0.01f,
                    1
            );

            // جنزير يسار
            drawCube(
                    excavatorX - 0.8f,
                    0.35f,
                    excavatorZ,
                    0.45f,
                    0.35f,
                    2.7f,
                    0.08f,
                    0.08f,
                    0.08f,
                    1
            );

            // جنزير يمين
            drawCube(
                    excavatorX + 0.8f,
                    0.35f,
                    excavatorZ,
                    0.45f,
                    0.35f,
                    2.7f,
                    0.08f,
                    0.08f,
                    0.08f,
                    1
            );
        }

        private void drawCoinArea() {

            // منطقة المكافأة
            drawCube(
                    7,
                    0.08f,
                    -5,
                    2,
                    0.15f,
                    2,
                    0.95f,
                    0.65f,
                    0.05f,
                    1
            );
        }

        private void drawCube(
                float x,
                float y,
                float z,
                float sx,
                float sy,
                float sz,
                float r,
                float g,
                float b,
                float a) {

            Matrix.setIdentityM(model, 0);

            Matrix.translateM(
                    model,
                    0,
                    x,
                    y,
                    z
            );

            Matrix.scaleM(
                    model,
                    0,
                    sx,
                    sy,
                    sz
            );

            float[] mv = new float[16];

            Matrix.multiplyMM(
                    mv,
                    0,
                    view,
                    0,
                    model,
                    0
            );

            Matrix.multiplyMM(
                    mvp,
                    0,
                    projection,
                    0,
                    mv,
                    0
            );

            GLES20.glUseProgram(program);

            GLES20.glEnableVertexAttribArray(
                    positionHandle
            );

            GLES20.glVertexAttribPointer(
                    positionHandle,
                    3,
                    GLES20.GL_FLOAT,
                    false,
                    0,
                    cubeBuffer
            );

            GLES20.glUniformMatrix4fv(
                    mvpHandle,
                    1,
                    false,
                    mvp,
                    0
            );

            GLES20.glUniform4f(
                    colorHandle,
                    r,
                    g,
                    b,
                    a
            );

            for (int i = 0; i < 6; i++) {

                GLES20.glDrawArrays(
                        GLES20.GL_TRIANGLE_FAN,
                        i * 4,
                        4
                );
            }

            GLES20.glDisableVertexAttribArray(
                    positionHandle
            );
        }

        private int loadShader(
                int type,
                String code) {

            int shader =
                    GLES20.glCreateShader(type);

            GLES20.glShaderSource(
                    shader,
                    code
            );

            GLES20.glCompileShader(shader);

            return shader;
        }
    }
}
