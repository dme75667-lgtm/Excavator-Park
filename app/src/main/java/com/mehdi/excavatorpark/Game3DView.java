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

    private final Renderer3D renderer;

    public Game3DView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        renderer = new Renderer3D();
        setRenderer(renderer);

        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {

            float x = event.getX();
            float y = event.getY();

            float w = getWidth();
            float h = getHeight();

            if (x < w * 0.25f) {
                renderer.moveLeft = true;
                renderer.moveRight = false;
            } else if (x > w * 0.75f) {
                renderer.moveRight = true;
                renderer.moveLeft = false;
            } else {
                renderer.moveLeft = false;
                renderer.moveRight = false;
            }

            if (y < h * 0.45f) {
                renderer.moveForward = true;
                renderer.moveBackward = false;
            } else if (y > h * 0.70f) {
                renderer.moveBackward = true;
                renderer.moveForward = false;
            } else {
                renderer.moveForward = false;
                renderer.moveBackward = false;
            }

            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            renderer.moveLeft = false;
            renderer.moveRight = false;
            renderer.moveForward = false;
            renderer.moveBackward = false;
            return true;
        }

        return true;
    }

    // ============================================================
    // RENDERER
    // ============================================================

    private static class Renderer3D implements GLSurfaceView.Renderer {

        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];

        private FloatBuffer cubeVertices;

        private int program;
        private int positionHandle;
        private int colorHandle;
        private int mvpHandle;

        private float excavatorX = 0f;
        private float excavatorZ = 0f;

        private float cameraX;
        private float cameraY = 8f;
        private float cameraZ;

        private final float speed = 0.045f;

        boolean moveLeft;
        boolean moveRight;
        boolean moveForward;
        boolean moveBackward;

        // --------------------------------------------------------
        // Cube vertices
        // --------------------------------------------------------

        private final float[] cubeData = {

                // Front
                -0.5f, -0.5f,  0.5f,
                 0.5f, -0.5f,  0.5f,
                 0.5f,  0.5f,  0.5f,

                -0.5f, -0.5f,  0.5f,
                 0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,

                // Back
                 0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f,
                -0.5f,  0.5f, -0.5f,

                 0.5f, -0.5f, -0.5f,
                -0.5f,  0.5f, -0.5f,
                 0.5f,  0.5f, -0.5f,

                // Left
                -0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,

                -0.5f, -0.5f, -0.5f,
                -0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f, -0.5f,

                // Right
                 0.5f, -0.5f,  0.5f,
                 0.5f, -0.5f, -0.5f,
                 0.5f,  0.5f, -0.5f,

                 0.5f, -0.5f,  0.5f,
                 0.5f,  0.5f, -0.5f,
                 0.5f,  0.5f,  0.5f,

                // Top
                -0.5f,  0.5f,  0.5f,
                 0.5f,  0.5f,  0.5f,
                 0.5f,  0.5f, -0.5f,

                -0.5f,  0.5f,  0.5f,
                 0.5f,  0.5f, -0.5f,
                -0.5f,  0.5f, -0.5f,

                // Bottom
                -0.5f, -0.5f, -0.5f,
                 0.5f, -0.5f, -0.5f,
                 0.5f, -0.5f,  0.5f,

                -0.5f, -0.5f, -0.5f,
                 0.5f, -0.5f,  0.5f,
                -0.5f, -0.5f,  0.5f
        };

        @Override
        public void onSurfaceCreated(
                javax.microedition.khronos.egl.EGLConfig config) {

            GLES20.glClearColor(
                    0.48f,
                    0.72f,
                    0.42f,
                    1f
            );

            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);

            cubeVertices = ByteBuffer
                    .allocateDirect(cubeData.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            cubeVertices.put(cubeData);
            cubeVertices.position(0);

            String vertexShaderCode =
                    "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "gl_Position = uMVPMatrix * vPosition;" +
                    "}";

            String fragmentShaderCode =
                    "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "gl_FragColor = vColor;" +
                    "}";

            int vertexShader = loadShader(
                    GLES20.GL_VERTEX_SHADER,
                    vertexShaderCode
            );

            int fragmentShader = loadShader(
                    GLES20.GL_FRAGMENT_SHADER,
                    fragmentShaderCode
            );

            program = GLES20.glCreateProgram();

            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);

            GLES20.glLinkProgram(program);

            positionHandle =
                    GLES20.glGetAttribLocation(
                            program,
                            "vPosition"
                    );

            colorHandle =
                    GLES20.glGetUniformLocation(
                            program,
                            "vColor"
                    );

            mvpHandle =
                    GLES20.glGetUniformLocation(
                            program,
                            "uMVPMatrix"
                    );
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
                    120
            );
        }

        @Override
        public void onDrawFrame(
                javax.microedition.khronos.opengles.GL10 gl) {

            GLES20.glClear(
                    GLES20.GL_COLOR_BUFFER_BIT |
                    GLES20.GL_DEPTH_BUFFER_BIT
            );

            updateExcavator();

            cameraX = excavatorX;
            cameraZ = excavatorZ + 12f;

            Matrix.setLookAtM(
                    view,
                    0,

                    cameraX,
                    cameraY,
                    cameraZ,

                    excavatorX,
                    1.5f,
                    excavatorZ,

                    0,
                    1,
                    0
            );

            drawTerrain();

            drawRoad();

            drawDiggingArea();

            drawTrees();

            drawRocks();

            drawFence();

            drawExcavator();
        }

        // ========================================================
        // TERRAIN
        // ========================================================

        private void drawTerrain() {

            // Base
            drawCube(
                    0,
                    -1.0f,
                    0,
                    45,
                    1,
                    45,
                    0.18f,
                    0.55f,
                    0.18f,
                    1
            );

            // Green terrain sections with different heights
            drawCube(
                    0,
                    -0.35f,
                    -15,
                    42,
                    0.7f,
                    14,
                    0.22f,
                    0.62f,
                    0.20f,
                    1
            );

            drawCube(
                    -14,
                    -0.10f,
                    2,
                    12,
                    1.2f,
                    18,
                    0.25f,
                    0.68f,
                    0.22f,
                    1
            );

            drawCube(
                    15,
                    0.20f,
                    3,
                    12,
                    1.8f,
                    18,
                    0.20f,
                    0.58f,
                    0.18f,
                    1
            );

            // Small grass hills
            drawCube(
                    -8,
                    0.65f,
                    -9,
                    8,
                    1.0f,
                    6,
                    0.28f,
                    0.72f,
                    0.23f,
                    1
            );

            drawCube(
                    8,
                    0.85f,
                    -8,
                    9,
                    1.4f,
                    6,
                    0.24f,
                    0.65f,
                    0.20f,
                    1
            );

            // Grass details
            for (int i = -18; i <= 18; i += 4) {

                drawCube(
                        i,
                        0.15f,
                        12,
                        2.0f,
                        0.25f,
                        2.0f,
                        0.16f,
                        0.50f,
                        0.14f,
                        1
                );
            }
        }

        // ========================================================
        // ROAD
        // ========================================================

        private void drawRoad() {

            drawCube(
                    0,
                    0.05f,
                    0,
                    8,
                    0.15f,
                    45,
                    0.12f,
                    0.12f,
                    0.12f,
                    1
            );

            for (int z = -20; z <= 20; z += 5) {

                drawCube(
                        0,
                        0.15f,
                        z,
                        0.35f,
                        0.08f,
                        2.2f,
                        0.95f,
                        0.75f,
                        0.08f,
                        1
                );
            }
        }

        // ========================================================
        // DIGGING AREA
        // ========================================================

        private void drawDiggingArea() {

            drawCube(
                    15,
                    -0.20f,
                    -4,
                    9,
                    0.5f,
                    8,
                    0.35f,
                    0.25f,
                    0.12f,
                    1
            );

            drawCube(
                    15,
                    0.55f,
                    -4,
                    9,
                    0.8f,
                    1,
                    0.40f,
                    0.30f,
                    0.14f,
                    1
            );

            drawCube(
                    15,
                    0.55f,
                    0,
                    9,
                    0.8f,
                    1,
                    0.40f,
                    0.30f,
                    0.14f,
                    1
            );

            drawCube(
                    11,
                    0.55f,
                    -4,
                    1,
                    0.8f,
                    7,
                    0.40f,
                    0.30f,
                    0.14f,
                    1
            );

            drawCube(
                    19,
                    0.55f,
                    -4,
                    1,
                    0.8f,
                    7,
                    0.40f,
                    0.30f,
                    0.14f,
                    1
            );
        }

        // ========================================================
        // TREES
        // ========================================================

        private void drawTrees() {

            drawTree(-15, -10);
            drawTree(15, -14);
            drawTree(-16, 12);
            drawTree(17, 13);
            drawTree(-9, 16);
        }

        private void drawTree(float x, float z) {

            drawCube(
                    x,
                    1.4f,
                    z,
                    0.7f,
                    2.8f,
                    0.7f,
                    0.32f,
                    0.18f,
                    0.08f,
                    1
            );

            drawCube(
                    x,
                    3.2f,
                    z,
                    2.8f,
                    2.8f,
                    2.8f,
                    0.10f,
                    0.48f,
                    0.12f,
                    1
            );
        }

        // ========================================================
        // ROCKS
        // ========================================================

        private void drawRocks() {

            drawCube(
                    -10,
                    0.8f,
                    -3,
                    2.2f,
                    1.4f,
                    2.0f,
                    0.35f,
                    0.35f,
                    0.32f,
                    1
            );

            drawCube(
                    10,
                    1.0f,
                    8,
                    2.8f,
                    1.8f,
                    2.2f,
                    0.38f,
                    0.36f,
                    0.30f,
                    1
            );
        }

        // ========================================================
        // FENCE
        // ========================================================

        private void drawFence() {

            for (int x = -20; x <= 20; x += 4) {

                drawCube(
                        x,
                        1.2f,
                        -20,
                        0.3f,
                        2.4f,
                        0.3f,
                        0.30f,
                        0.30f,
                        0.30f,
                        1
                );

                drawCube(
                        x,
                        1.2f,
                        20,
                        0.3f,
                        2.4f,
                        0.3f,
                        0.30f,
                        0.30f,
                        0.30f,
                        1
                );
            }
        }

        // ========================================================
        // EXCAVATOR
        // ========================================================

        private void drawExcavator() {

            float x = excavatorX;
            float z = excavatorZ;

            // Tracks
            drawCube(
                    x - 1.1f,
                    0.7f,
                    z,
                    1.2f,
                    0.8f,
                    3.2f,
                    0.08f,
                    0.08f,
                    0.08f,
                    1
            );

            drawCube(
                    x + 1.1f,
                    0.7f,
                    z,
                    1.2f,
                    0.8f,
                    3.2f,
                    0.08f,
                    0.08f,
                    0.08f,
                    1
            );

            // Main body
            drawCube(
                    x,
                    1.35f,
                    z,
                    3.0f,
                    1.0f,
                    2.4f,
                    0.95f,
                    0.62f,
                    0.05f,
                    1
            );

            // Cabin
            drawCube(
                    x,
                    2.45f,
                    z - 0.35f,
                    1.8f,
                    1.6f,
                    1.7f,
                    0.95f,
                    0.65f,
                    0.06f,
                    1
            );

            // Windows
            drawCube(
                    x,
                    2.55f,
                    z - 1.25f,
                    1.3f,
                    0.9f,
                    0.08f,
                    0.08f,
                    0.20f,
                    0.28f,
                    1
            );

            // Roof
            drawCube(
                    x,
                    3.35f,
                    z - 0.35f,
                    2.0f,
                    0.2f,
                    1.9f,
                    0.85f,
                    0.50f,
                    0.03f,
                    1
            );

            // Arm
            drawCube(
                    x,
                    3.0f,
                    z + 2.0f,
                    0.65f,
                    0.65f,
                    3.5f,
                    0.92f,
                    0.58f,
                    0.03f,
                    1
            );

            // Second arm
            drawCube(
                    x,
                    2.2f,
                    z + 4.0f,
                    0.55f,
                    0.55f,
                    2.8f,
                    0.88f,
                    0.52f,
                    0.02f,
                    1
            );

            // Bucket
            drawCube(
                    x,
                    1.45f,
                    z + 5.3f,
                    1.8f,
                    1.2f,
                    1.4f,
                    0.82f,
                    0.48f,
                    0.02f,
                    1
            );
        }

        // ========================================================
        // MOVEMENT
        // ========================================================

        private void updateExcavator() {

            if (moveForward) {
                excavatorZ -= speed;
            }

            if (moveBackward) {
                excavatorZ += speed;
            }

            if (moveLeft) {
                excavatorX -= speed;
            }

            if (moveRight) {
                excavatorX += speed;
            }

            if (excavatorX > 18)
                excavatorX = 18;

            if (excavatorX < -18)
                excavatorX = -18;

            if (excavatorZ > 18)
                excavatorZ = 18;

            if (excavatorZ < -18)
                excavatorZ = -18;
        }

        // ========================================================
        // DRAW CUBE
        // ========================================================

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

            float[] temp = new float[16];

            Matrix.multiplyMM(
                    temp,
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
                    temp,
                    0
            );

            GLES20.glUseProgram(program);

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

            cubeVertices.position(0);

            GLES20.glEnableVertexAttribArray(
                    positionHandle
            );

            GLES20.glVertexAttribPointer(
                    positionHandle,
                    3,
                    GLES20.GL_FLOAT,
                    false,
                    3 * 4,
                    cubeVertices
            );

            GLES20.glDrawArrays(
                    GLES20.GL_TRIANGLES,
                    0,
                    36
            );

            GLES20.glDisableVertexAttribArray(
                    positionHandle
            );
        }

        // ========================================================
        // SHADER
        // ========================================================

        private int loadShader(
                int type,
                String shaderCode) {

            int shader =
                    GLES20.glCreateShader(type);

            GLES20.glShaderSource(
                    shader,
                    shaderCode
            );

            GLES20.glCompileShader(shader);

            return shader;
        }
    }
                }
