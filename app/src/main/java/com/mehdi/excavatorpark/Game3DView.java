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

    private final GameRenderer renderer;

    public Game3DView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        renderer = new GameRenderer();
        setRenderer(renderer);

        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
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

            if (x < w * 0.25f && y > h * 0.65f) {
                renderer.left = true;
            } else if (x > w * 0.75f && y > h * 0.65f) {
                renderer.right = true;
            } else if (y < h * 0.45f) {
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

    private static class GameRenderer implements GLSurfaceView.Renderer {

        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];
        private final float[] mv = new float[16];

        private FloatBuffer cubeBuffer;

        private int program;
        private int positionHandle;
        private int colorHandle;
        private int mvpHandle;

        private float excavatorX = 0f;
        private float excavatorZ = 0f;

        private static final float SPEED = 0.08f;

        boolean left;
        boolean right;
        boolean forward;
        boolean backward;

        private final float[] cubeVertices = {
                -0.5f, -0.5f,  0.5f,
                 0.5f, -0.5f,  0.5f,
                 0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,

                -0.5f, -0.5f, -0.5f,
                -0.5f,  0.5f, -0.5f,
                 0.5f,  0.5f, -0.5f,
                 0.5f, -0.5f, -0.5f,

                -0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f, -0.5f,

                 0.5f, -0.5f, -0.5f,
                 0.5f,  0.5f, -0.5f,
                 0.5f,  0.5f,  0.5f,
                 0.5f, -0.5f,  0.5f,

                -0.5f,  0.5f, -0.5f,
                -0.5f,  0.5f,  0.5f,
                 0.5f,  0.5f,  0.5f,
                 0.5f,  0.5f, -0.5f,

                -0.5f, -0.5f, -0.5f,
                 0.5f, -0.5f, -0.5f,
                 0.5f, -0.5f,  0.5f,
                -0.5f, -0.5f,  0.5f
        };

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

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

            updateMovement();

            Matrix.setLookAtM(
                    view,
                    0,

                    excavatorX,
                    5.5f,
                    excavatorZ + 9f,

                    excavatorX,
                    0f,
                    excavatorZ,

                    0f,
                    1f,
                    0f
            );

            drawGround();
            drawRoad();
            drawExcavator();
        }

        private void updateMovement() {

            if (left) {
                excavatorX -= SPEED;
            }

            if (right) {
                excavatorX += SPEED;
            }

            if (forward) {
                excavatorZ -= SPEED;
            }

            if (backward) {
                excavatorZ += SPEED;
            }
        }

        private void drawGround() {

            drawCube(
                    0f, -0.5f, 0f,
                    20f, 1f, 20f,
                    0.20f, 0.45f, 0.18f
            );
        }

        private void drawRoad() {

            drawCube(
                    0f, 0.02f, 0f,
                    5f, 0.12f, 20f,
                    0.35f, 0.25f, 0.15f
            );
        }

        private void drawExcavator() {

            // الجسم
            drawCube(
                    excavatorX,
                    0.65f,
                    excavatorZ,
                    2.2f,
                    0.8f,
                    2.5f,
                    1.0f,
                    0.55f,
                    0.02f
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
                    0.13f
            );

            // الذراع
            drawCube(
                    excavatorX + 1.0f,
                    1.35f,
                    excavatorZ - 0.35f,
                    0.35f,
                    2.2f,
                    0.35f,
                    1.0f,
                    0.55f,
                    0.02f
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
                    0.01f
            );

            // الجنزير الأيسر
            drawCube(
                    excavatorX - 0.8f,
                    0.35f,
                    excavatorZ,
                    0.45f,
                    0.35f,
                    2.7f,
                    0.08f,
                    0.08f,
                    0.08f
            );

            // الجنزير الأيمن
            drawCube(
                    excavatorX + 0.8f,
                    0.35f,
                    excavatorZ,
                    0.45f,
                    0.35f,
                    2.7f,
                    0.08f,
                    0.08f,
                    0.08f
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
