package com.mehdi.filterspark;

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

    private final Renderer3D renderer;

    public Game3DView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        renderer = new Renderer3D();
        setRenderer(renderer);

        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        float width = getWidth();
        float height = getHeight();

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:

                renderer.moveLeft = x < width * 0.25f;
                renderer.moveRight = x > width * 0.75f;

                renderer.moveForward = y < height * 0.45f;
                renderer.moveBackward = y > height * 0.70f;

                return true;

            case MotionEvent.ACTION_UP:

                renderer.stopMovement();
                return true;
        }

        return true;
    }

    private static class Renderer3D implements GLSurfaceView.Renderer {

        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];
        private final float[] temp = new float[16];

        private FloatBuffer cubeVertices;

        private int program;
        private int positionHandle;
        private int colorHandle;
        private int mvpHandle;

        private float excavatorX = 0f;
        private float excavatorZ = 0f;

        private final float speed = 0.045f;

        boolean moveLeft;
        boolean moveRight;
        boolean moveForward;
        boolean moveBackward;

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
                GL10 gl,
                EGLConfig config) {

            GLES20.glClearColor(
                    0.48f,
                    0.72f,
                    0.42f,
                    1.0f
            );

            GLES20.glEnable(
                    GLES20.GL_DEPTH_TEST
            );

            GLES20.glEnable(
                    GLES20.GL_CULL_FACE
            );

            ByteBuffer buffer =
                    ByteBuffer.allocateDirect(
                            cubeData.length * 4
                    );

            buffer.order(
                    ByteOrder.nativeOrder()
            );

            cubeVertices =
                    buffer.asFloatBuffer();

            cubeVertices.put(cubeData);
            cubeVertices.position(0);

            String vertexShader =
                    "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "gl_Position = uMVPMatrix * vPosition;" +
                    "}";

            String fragmentShader =
                    "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "gl_FragColor = vColor;" +
                    "}";

            int vertex =
                    loadShader(
                            GLES20.GL_VERTEX_SHADER,
                            vertexShader
                    );

            int fragment =
                    loadShader(
                            GLES20.GL_FRAGMENT_SHADER,
                            fragmentShader
                    );

            program =
                    GLES20.glCreateProgram();

            GLES20.glAttachShader(
                    program,
                    vertex
            );

            GLES20.glAttachShader(
                    program,
                    fragment
            );

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
                GL10 gl,
                int width,
                int height) {

            if (height == 0) {
                height = 1;
            }

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
                    120f
            );
        }

        @Override
        public void onDrawFrame(GL10 gl) {

            GLES20.glClear(
                    GLES20.GL_COLOR_BUFFER_BIT |
                    GLES20.GL_DEPTH_BUFFER_BIT
            );

            updateExcavator();

            float cameraX = excavatorX;
            float cameraY = 8f;
            float cameraZ = excavatorZ + 12f;

            Matrix.setLookAtM(
                    view,
                    0,
                    cameraX,
                    cameraY,
                    cameraZ,
                    excavatorX,
                    1.5f,
                    excavatorZ,
                    0f,
                    1f,
                    0f
            );

            drawTerrain();
            drawRoad();
            drawDiggingArea();
            drawTrees();
            drawRocks();
            drawFence();
            drawExcavator();
        }

        private void drawTerrain() {

            drawCube(
                    0f, -1f, 0f,
                    45f, 1f, 45f,
                    0.18f, 0.55f, 0.18f
            );

            drawCube(
                    0f, -0.35f, -15f,
                    42f, 0.7f, 14f,
                    0.22f, 0.62f, 0.20f
            );

            drawCube(
                    -14f, -0.10f, 2f,
                    12f, 1.2f, 18f,
                    0.25f, 0.68f, 0.22f
            );

            drawCube(
                    15f, 0.20f, 3f,
                    12f, 1.8f, 18f,
                    0.20f, 0.58f, 0.18f
            );

            drawCube(
                    -8f, 0.65f, -9f,
                    8f, 1f, 6f,
                    0.28f, 0.72f, 0.23f
            );

            drawCube(
                    8f, 0.85f, -8f,
                    9f, 1.4f, 6f,
                    0.24f, 0.65f, 0.20f
            );
        }

        private void drawRoad() {

            drawCube(
                    0f, 0.05f, 0f,
                    8f, 0.15f, 45f,
                    0.12f, 0.12f, 0.12f
            );

            for (int z = -20; z <= 20; z += 5) {

                drawCube(
                        0f,
                        0.15f,
                        z,
                        0.35f,
                        0.08f,
                        2.2f,
                        0.95f,
                        0.75f,
                        0.08f
                );
            }
        }

        private void drawDiggingArea() {

            drawCube(
                    15f, -0.20f, -4f,
                    9f, 0.5f, 8f,
                    0.35f, 0.25f, 0.12f
            );

            drawCube(
                    15f, 0.55f, -4f,
                    9f, 0.8f, 1f,
                    0.40f, 0.30f, 0.14f
            );

            drawCube(
                    15f, 0.55f, 0f,
                    9f, 0.8f, 1f,
                    0.40f, 0.30f, 0.14f
            );

            drawCube(
                    11f, 0.55f, -4f,
                    1f, 0.8f, 7f,
                    0.40f, 0.30f, 0.14f
            );

            drawCube(
                    19f, 0.55f, -4f,
                    1f, 0.8f, 7f,
                    0.40f, 0.30f, 0.14f
            );
        }

        private void drawTrees() {

            drawTree(-15f, -10f);
            drawTree(15f, -14f);
            drawTree(-16f, 12f);
            drawTree(17f, 13f);
            drawTree(-9f, 16f);
        }

        private void drawTree(float x, float z) {

            drawCube(
                    x, 1.4f, z,
                    0.7f, 2.8f, 0.7f,
                    0.32f, 0.18f, 0.08f
            );

            drawCube(
                    x, 3.2f, z,
                    2.8f, 2.8f, 2.8f,
                    0.10f, 0.48f, 0.12f
            );
        }

        private void drawRocks() {

            drawCube(
                    -10f, 0.8f, -3f,
                    2.2f, 1.4f, 2f,
                    0.35f, 0.35f, 0.32f
            );

            drawCube(
                    10f, 1f, 8f,
                    2.8f, 1.8f, 2.2f,
                    0.38f, 0.36f, 0.30f
            );
        }

        private void drawFence() {

            for (int x = -20; x <= 20; x += 4) {

                drawCube(
                        x, 1.2f, -20f,
                        0.3f, 2.4f, 0.3f,
                        0.30f, 0.30f, 0.30f
                );

                drawCube(
                        x, 1.2f, 20f,
                        0.3f, 2.4f, 0.3f,
                        0.30f, 0.30f, 0.30f
                );
            }
        }

        private void drawExcavator() {

            float x = excavatorX;
            float z = excavatorZ;

            // Left track
            drawCube(
                    x - 1.1f, 0.7f, z,
                    1.2f, 0.8f, 3.2f,
                    0.08f, 0.08f, 0.08f
            );

            // Right track
            drawCube(
                    x + 1.1f, 0.7f, z,
                    1.2f, 0.8f, 3.2f,
                    0.08f, 0.08f, 0.08f
            );

            // Body
            drawCube(
                    x, 1.35f, z,
                    3f, 1f, 2.4f,
                    0.95f, 0.62f, 0.05f
            );

            // Cabin
            drawCube(
                    x, 2.45f, z - 0.35f,
                    1.8f, 1.6f, 1.7f,
                    0.95f, 0.65f, 0.06f
            );

            // Front window
            drawCube(
                    x, 2.55f, z - 1.25f,
                    1.3f, 0.9f, 0.08f,
                    0.08f, 0.20f, 0.28f
            );

            // Roof
            drawCube(
                    x, 3.35f, z - 0.35f,
                    2f, 0.2f, 1.9f,
                    0.85f, 0.50f, 0.03f
            );

            // Main boom
            drawCube(
                    x, 3f, z + 2f,
                    0.65f, 0.65f, 3.5f,
                    0.92f, 0.58f, 0.03f
            );

            // Stick
            drawCube(
                    x, 2.2f, z + 4f,
                    0.55f, 0.55f, 2.8f,
                    0.88f, 0.52f, 0.02f
            );

            // Bucket
            drawCube(
                    x, 1.45f, z + 5.3f,
                    1.8f, 1.2f, 1.4f,
                    0.82f, 0.48f, 0.02f
            );
        }

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

            excavatorX =
                    Math.max(
                            -18f,
                            Math.min(
                                    18f,
                                    excavatorX
                            )
                    );

            excavatorZ =
                    Math.max(
                            -18f,
                            Math.min(
                                    18f,
                                    excavatorZ
                            )
                    );
        }

        private void stopMovement() {

            moveLeft = false;
            moveRight = false;
            moveForward = false;
            moveBackward = false;
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
                    1f
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
                    12,
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
