package com.mehdi.excavatorpark;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Game3DView extends GLSurfaceView {

    private final ParkRenderer renderer;

    public Game3DView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        renderer = new ParkRenderer();
        setRenderer(renderer);

        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        float w = getWidth();
        float h = getHeight();

        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {

            renderer.left = false;
            renderer.right = false;
            renderer.forward = false;
            renderer.backward = false;

            if (x < w * 0.25f && y > h * 0.60f) {
                renderer.left = true;
            } else if (x > w * 0.75f && y > h * 0.60f) {
                renderer.right = true;
            } else if (y < h * 0.55f) {
                renderer.forward = true;
            } else {
                renderer.backward = true;
            }

            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {

            renderer.left = false;
            renderer.right = false;
            renderer.forward = false;
            renderer.backward = false;

            return true;
        }

        return true;
    }

    private static class ParkRenderer implements GLSurfaceView.Renderer {

        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mv = new float[16];
        private final float[] mvp = new float[16];

        private FloatBuffer cubeBuffer;

        private int program;
        private int positionHandle;
        private int colorHandle;
        private int mvpHandle;

        private float excavatorX = 0f;
        private float excavatorZ = 3f;

        boolean left;
        boolean right;
        boolean forward;
        boolean backward;

        private final float[] cubeVertices = {

                -0.5f,-0.5f, 0.5f,
                 0.5f,-0.5f, 0.5f,
                 0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,

                -0.5f,-0.5f,-0.5f,
                -0.5f, 0.5f,-0.5f,
                 0.5f, 0.5f,-0.5f,
                 0.5f,-0.5f,-0.5f,

                -0.5f,-0.5f,-0.5f,
                -0.5f,-0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f,-0.5f,

                 0.5f,-0.5f,-0.5f,
                 0.5f, 0.5f,-0.5f,
                 0.5f, 0.5f, 0.5f,
                 0.5f,-0.5f, 0.5f,

                -0.5f, 0.5f,-0.5f,
                -0.5f, 0.5f, 0.5f,
                 0.5f, 0.5f, 0.5f,
                 0.5f, 0.5f,-0.5f,

                -0.5f,-0.5f,-0.5f,
                 0.5f,-0.5f,-0.5f,
                 0.5f,-0.5f, 0.5f,
                -0.5f,-0.5f, 0.5f
        };

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

            GLES20.glClearColor(
                    0.52f,
                    0.72f,
                    0.88f,
                    1f
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
                    "void main(){" +
                    "gl_Position=uMVP*aPosition;" +
                    "}";

            String fragmentShader =
                    "precision mediump float;" +
                    "uniform vec4 uColor;" +
                    "void main(){" +
                    "gl_FragColor=uColor;" +
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
                    GLES20.glGetAttribLocation(
                            program,
                            "aPosition"
                    );

            colorHandle =
                    GLES20.glGetUniformLocation(
                            program,
                            "uColor"
                    );

            mvpHandle =
                    GLES20.glGetUniformLocation(
                            program,
                            "uMVP"
                    );
        }

        @Override
        public void onSurfaceChanged(
                GL10 gl,
                int width,
                int height) {

            GLES20.glViewport(
                    0,
                    0,
                    width,
                    height
            );

            float ratio =
                    (float) width / (float) height;

            Matrix.frustumM(
                    projection,
                    0,
                    -ratio,
                    ratio,
                    -1f,
                    1f,
                    2f,
                    100f
            );
        }

        @Override
        public void onDrawFrame(GL10 gl) {

            GLES20.glClear(
                    GLES20.GL_COLOR_BUFFER_BIT |
                    GLES20.GL_DEPTH_BUFFER_BIT
            );

            moveExcavator();

            Matrix.setLookAtM(
                    view,
                    0,

                    excavatorX,
                    6.0f,
                    excavatorZ + 10.0f,

                    excavatorX,
                    0.5f,
                    excavatorZ,

                    0f,
                    1f,
                    0f
            );

            drawPark();
            drawRoad();
            drawDiggingArea();
            drawTrees();
            drawRocks();
            drawFence();
            drawExcavator();
        }

        private void moveExcavator() {

            float speed = 0.10f;

            if (left) {
                excavatorX -= speed;
            }

            if (right) {
                excavatorX += speed;
            }

            if (forward) {
                excavatorZ -= speed;
            }

            if (backward) {
                excavatorZ += speed;
            }

            if (excavatorX > 8.5f) {
                excavatorX = 8.5f;
            }

            if (excavatorX < -8.5f) {
                excavatorX = -8.5f;
            }

            if (excavatorZ > 8.5f) {
                excavatorZ = 8.5f;
            }

            if (excavatorZ < -8.5f) {
                excavatorZ = -8.5f;
            }
        }

        private void drawPark() {

            drawCube(
                    0f,-0.55f,0f,
                    24f,1f,24f,
                    0.22f,0.48f,0.20f
            );

            drawCube(
                    0f,-0.02f,-7.5f,
                    24f,0.08f,3f,
                    0.30f,0.20f,0.10f
            );
        }

        private void drawRoad() {

            drawCube(
                    0f,0.01f,0f,
                    5f,0.10f,22f,
                    0.28f,0.24f,0.18f
            );

            for (int z = -9; z <= 9; z += 2) {

                drawCube(
                        0f,
                        0.07f,
                        z,
                        0.18f,
                        0.02f,
                        0.8f,
                        0.90f,
                        0.78f,
                        0.20f
                );
            }
        }

        private void drawDiggingArea() {

            drawCube(
                    -6f,
                    -0.20f,
                    -4f,
                    5f,
                    0.35f,
                    4f,
                    0.38f,
                    0.22f,
                    0.08f
            );

            drawCube(
                    -6f,
                    0.02f,
                    -4f,
                    3.8f,
                    0.08f,
                    2.8f,
                    0.45f,
                    0.28f,
                    0.10f
            );

            drawCube(
                    -6f,
                    0.18f,
                    -4f,
                    2.0f,
                    0.12f,
                    1.2f,
                    0.30f,
                    0.17f,
                    0.06f
            );
        }

        private void drawTrees() {

            tree(-9f,-7f);
            tree(9f,-7f);
            tree(-9f,6f);
            tree(9f,6f);
            tree(-8f,1f);
            tree(8f,2f);
        }

        private void tree(float x, float z) {

            drawCube(
                    x,
                    1f,
                    z,
                    0.45f,
                    2f,
                    0.45f,
                    0.35f,
                    0.18f,
                    0.06f
            );

            drawCube(
                    x,
                    2.3f,
                    z,
                    2.0f,
                    2.0f,
                    2.0f,
                    0.05f,
                    0.32f,
                    0.08f
            );

            drawCube(
                    x,
                    3.2f,
                    z,
                    1.3f,
                    1.3f,
                    1.3f,
                    0.08f,
                    0.42f,
                    0.10f
            );
        }

        private void drawRocks() {

            rock(-4f,5f,1.2f);
            rock(5f,-5f,1.5f);
            rock(6f,5f,0.9f);
            rock(-7f,7f,1.0f);
        }

        private void rock(
                float x,
                float z,
                float size) {

            drawCube(
                    x,
                    size * 0.45f,
                    z,
                    size,
                    size * 0.8f,
                    size,
                    0.28f,
                    0.27f,
                    0.25f
            );
        }

        private void drawFence() {

            for (int x = -10; x <= 10; x += 2) {

                drawCube(
                        x,
                        0.8f,
                        -10f,
                        0.15f,
                        1.6f,
                        0.15f,
                        0.18f,
                        0.18f,
                        0.16f
                );

                drawCube(
                        x,
                        0.8f,
                        10f,
                        0.15f,
                        1.6f,
                        0.15f,
                        0.18f,
                        0.18f,
                        0.16f
                );
            }

            drawCube(
                    0f,
                    1.15f,
                    -10f,
                    20f,
                    0.15f,
                    0.15f,
                    0.18f,
                    0.18f,
                    0.16f
            );

            drawCube(
                    0f,
                    0.55f,
                    -10f,
                    20f,
                    0.15f,
                    0.15f,
                    0.18f,
                    0.18f,
                    0.16f
            );
        }

        private void drawExcavator() {

            // الجسم السفلي
            drawCube(
                    excavatorX,
                    0.65f,
                    excavatorZ,
                    2.3f,
                    0.7f,
                    2.5f,
                    0.95f,
                    0.55f,
                    0.03f
            );

            // الجنزير الأيسر
            drawCube(
                    excavatorX - 0.95f,
                    0.42f,
                    excavatorZ,
                    0.42f,
                    0.42f,
                    2.7f,
                    0.08f,
                    0.08f,
                    0.07f
            );

            // الجنزير الأيمن
            drawCube(
                    excavatorX + 0.95f,
                    0.42f,
                    excavatorZ,
                    0.42f,
                    0.42f,
                    2.7f,
                    0.08f,
                    0.08f,
                    0.07f
            );

            // الكابينة
            drawCube(
                    excavatorX - 0.35f,
                    1.55f,
                    excavatorZ + 0.15f,
                    1.25f,
                    1.25f,
                    1.25f,
                    0.10f,
                    0.12f,
                    0.13f
            );

            // سقف الكابينة
            drawCube(
                    excavatorX - 0.35f,
                    2.25f,
                    excavatorZ + 0.15f,
                    1.35f,
                    0.16f,
                    1.35f,
                    0.90f,
                    0.50f,
                    0.02f
            );

            // قاعدة الذراع
            drawCube(
                    excavatorX + 0.85f,
                    1.25f,
                    excavatorZ - 0.45f,
                    0.45f,
                    0.45f,
                    0.45f,
                    0.80f,
                    0.45f,
                    0.02f
            );

            // الذراع الأول
            drawCube(
                    excavatorX + 1.15f,
                    1.75f,
                    excavatorZ - 0.95f,
                    0.45f,
                    2.0f,
                    0.55f,
                    0.95f,
                    0.55f,
                    0.02f
            );

            // الذراع الثاني
            drawCube(
                    excavatorX + 1.15f,
                    1.15f,
                    excavatorZ - 2.0f,
                    0.40f,
                    1.2f,
                    0.45f,
                    0.95f,
                    0.55f,
                    0.02f
            );

            // الدلو
            drawCube(
                    excavatorX + 1.15f,
                    0.55f,
                    excavatorZ - 2.75f,
                    1.0f,
                    0.65f,
                    0.85f,
                    0.75f,
                    0.42f,
                    0.02f
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
                float b) {

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
                    1f
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
