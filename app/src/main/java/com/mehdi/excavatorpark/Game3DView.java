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

            renderer.clearButtons();

            // الحركة
            if (y > h * 0.72f) {

                if (x < w * 0.30f) {
                    renderer.left = true;
                } else if (x > w * 0.70f) {
                    renderer.right = true;
                } else {
                    renderer.forward = true;
                }
            }

            // الرجوع
            if (y < h * 0.30f &&
                    x > w * 0.35f &&
                    x < w * 0.65f) {
                renderer.backward = true;
            }

            // الذراع
            if (x < w * 0.30f && y < h * 0.55f) {
                renderer.armUp = true;
            }

            if (x > w * 0.70f && y < h * 0.55f) {
                renderer.armDown = true;
            }

            // الدلو
            if (x > w * 0.35f &&
                    x < w * 0.65f &&
                    y > h * 0.45f &&
                    y < h * 0.72f) {

                if (x < w * 0.50f) {
                    renderer.bucketOpen = true;
                } else {
                    renderer.bucketClose = true;
                }
            }

            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            renderer.clearButtons();
            return true;
        }

        return true;
    }

    private static class GameRenderer implements GLSurfaceView.Renderer {

        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];

        private FloatBuffer cubeBuffer;

        private int program;
        private int positionHandle;
        private int colorHandle;
        private int mvpHandle;

        private float playerX = 0f;
        private float playerZ = 8f;

        private float rotation = 0f;

        private float boomAngle = -25f;
        private float stickAngle = 25f;
        private float bucketAngle = 20f;

        private float wheelRotation = 0f;

        boolean forward;
        boolean backward;
        boolean left;
        boolean right;

        boolean armUp;
        boolean armDown;

        boolean bucketOpen;
        boolean bucketClose;

        private final float[] cube = {

                -1,-1,-1,
                 1,-1,-1,
                 1, 1,-1,

                -1,-1,-1,
                 1, 1,-1,
                -1, 1,-1,

                -1,-1, 1,
                 1,-1, 1,
                 1, 1, 1,

                -1,-1, 1,
                 1, 1, 1,
                -1, 1, 1,

                -1,-1,-1,
                -1, 1,-1,
                -1, 1, 1,

                -1,-1,-1,
                -1, 1, 1,
                -1,-1, 1,

                 1,-1,-1,
                 1, 1,-1,
                 1, 1, 1,

                 1,-1,-1,
                 1, 1, 1,
                 1,-1, 1,

                -1,1,-1,
                 1,1,-1,
                 1,1,1,

                -1,1,-1,
                 1,1,1,
                -1,1,1,

                -1,-1,-1,
                 1,-1,-1,
                 1,-1,1,

                -1,-1,-1,
                 1,-1,1,
                -1,-1,1
        };

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

            GLES20.glClearColor(
                    0.52f,
                    0.75f,
                    0.92f,
                    1f
            );

            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            GLES20.glEnable(GLES20.GL_CULL_FACE);

            ByteBuffer bb =
                    ByteBuffer.allocateDirect(cube.length * 4);

            bb.order(ByteOrder.nativeOrder());

            cubeBuffer = bb.asFloatBuffer();

            cubeBuffer.put(cube);
            cubeBuffer.position(0);

            String vertexShader =
                    "uniform mat4 uMVP;" +
                    "attribute vec4 aPosition;" +
                    "void main(){" +
                    "gl_Position=uMVP*aPosition;" +
                    "}";

            String fragmentShader =
                    "precision mediump float;" +
                    "uniform vec4 uColor;" +
                    "void main(){" +
                    "gl_FragColor=uColor;" +
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
                int height
        ) {

            if (height == 0)
                height = 1;

            GLES20.glViewport(
                    0,
                    0,
                    width,
                    height
            );

            float ratio =
                    (float) width /
                    (float) height;

            Matrix.perspectiveM(
                    projection,
                    0,
                    60f,
                    ratio,
                    1f,
                    250f
            );
        }

        @Override
        public void onDrawFrame(GL10 gl) {

            GLES20.glClear(
                    GLES20.GL_COLOR_BUFFER_BIT |
                    GLES20.GL_DEPTH_BUFFER_BIT
            );

            update();

            // كاميرا خلف الحفارة
            float cameraDistance = 18f;

            float camX =
                    playerX -
                    (float)Math.sin(
                            Math.toRadians(rotation)
                    ) * cameraDistance;

            float camZ =
                    playerZ -
                    (float)Math.cos(
                            Math.toRadians(rotation)
                    ) * cameraDistance;

            Matrix.setLookAtM(
                    view,
                    0,
                    camX,
                    10f,
                    camZ,
                    playerX,
                    1.8f,
                    playerZ,
                    0f,
                    1f,
                    0f
            );

            drawWorld();
            drawExcavator();
        }

        private void update() {

            float speed = 0.12f;

            if (forward) {

                playerX +=
                        (float)Math.sin(
                                Math.toRadians(rotation)
                        ) * speed;

                playerZ +=
                        (float)Math.cos(
                                Math.toRadians(rotation)
                        ) * speed;

                wheelRotation += 8f;
            }

            if (backward) {

                playerX -=
                        (float)Math.sin(
                                Math.toRadians(rotation)
                        ) * speed;

                playerZ -=
                        (float)Math.cos(
                                Math.toRadians(rotation)
                        ) * speed;

                wheelRotation -= 8f;
            }

            if (left) {
                rotation -= 2.2f;
            }

            if (right) {
                rotation += 2.2f;
            }

            if (armUp) {

                boomAngle -= 0.8f;
                stickAngle += 0.5f;
            }

            if (armDown) {

                boomAngle += 0.8f;
                stickAngle -= 0.5f;
            }

            if (bucketOpen) {
                bucketAngle += 1.2f;
            }

            if (bucketClose) {
                bucketAngle -= 1.2f;
            }

            boomAngle =
                    Math.max(
                            -60f,
                            Math.min(
                                    20f,
                                    boomAngle
                            )
                    );

            stickAngle =
                    Math.max(
                            -20f,
                            Math.min(
                                    75f,
                                    stickAngle
                            )
                    );

            bucketAngle =
                    Math.max(
                            -60f,
                            Math.min(
                                    80f,
                                    bucketAngle
                            )
                    );

            playerX =
                    Math.max(
                            -45f,
                            Math.min(
                                    45f,
                                    playerX
                            )
                    );

            playerZ =
                    Math.max(
                            -45f,
                            Math.min(
                                    45f,
                                    playerZ
                            )
                    );
        }

        private void drawWorld() {

            // أرض كبيرة
            box(
                    0,
                    -1.5f,
                    0,
                    55,
                    1,
                    55,
                    0.20f,
                    0.48f,
                    0.18f
            );

            // الطريق
            box(
                    0,
                    -0.35f,
                    0,
                    7,
                    0.15f,
                    55,
                    0.12f,
                    0.12f,
                    0.12f
            );

            // خطوط الطريق
            for (int z = -45; z <= 45; z += 8) {

                box(
                        0,
                        -0.15f,
                        z,
                        0.15f,
                        0.05f,
                        3,
                        0.9f,
                        0.75f,
                        0.15f
                );
            }

            // منطقة الحفر
            box(
                    18,
                    -0.20f,
                    5,
                    13,
                    0.20f,
                    13,
                    0.35f,
                    0.20f,
                    0.08f
            );

            // أكوام التراب
            dirtPile(18, 3);
            dirtPile(22, 8);
            dirtPile(15, 10);

            // حواجز
            for (int z = -25; z <= 25; z += 10) {

                box(
                        -12,
                        0.5f,
                        z,
                        0.25f,
                        1f,
                        0.25f,
                        0.9f,
                        0.45f,
                        0.05f
                );
            }

            // أشجار
            tree(-25, -20);
            tree(-30, 5);
            tree(30, -15);
            tree(28, 25);

            // صخور
            rock(-18, 15);
            rock(25, -5);
            rock(-30, 30);
        }

        private void dirtPile(
                float x,
                float z
        ) {

            box(
                    x,
                    0.7f,
                    z,
                    3.2f,
                    1.3f,
                    2.4f,
                    0.35f,
                    0.20f,
                    0.07f
            );

            box(
                    x + 1.5f,
                    0.35f,
                    z,
                    2.0f,
                    0.7f,
                    1.7f,
                    0.30f,
                    0.16f,
                    0.05f
            );
        }

        private void tree(
                float x,
                float z
        ) {

            box(
                    x,
                    1.8f,
                    z,
                    0.45f,
                    2f,
                    0.45f,
                    0.30f,
                    0.16f,
                    0.05f
            );

            box(
                    x,
                    4f,
                    z,
                    2.2f,
                    2.2f,
                    2.2f,
                    0.05f,
                    0.38f,
                    0.08f
            );
        }

        private void rock(
                float x,
                float z
        ) {

            box(
                    x,
                    0.6f,
                    z,
                    1.5f,
                    0.8f,
                    1.3f,
                    0.30f,
                    0.30f,
                    0.30f
            );
        }

        private void drawExcavator() {

            // جسم الحفارة
            Matrix.setIdentityM(model, 0, 1);

            Matrix.translateM(
                    model,
                    0,
                    playerX,
                    0,
                    playerZ
            );

            Matrix.rotateM(
                    model,
                    0,
                    rotation,
                    0,
                    1,
                    0
            );

            drawLocal(
                    0,
                    1.2f,
                    0,
                    3.2f,
                    0.65f,
                    2.3f,
                    0.85f,
                    0.52f,
                    0.04f
            );

            // الجنزير الأيسر
            drawLocal(
                    -2.0f,
                    0.45f,
                    0,
                    1.1f,
                    0.55f,
                    2.7f,
                    0.08f,
                    0.08f,
                    0.07f
            );

            // الجنزير الأيمن
            drawLocal(
                    2.0f,
                    0.45f,
                    0,
                    1.1f,
                    0.55f,
                    2.7f,
                    0.08f,
                    0.08f,
                    0.07f
            );

            // قاعدة علوية
            drawLocal(
                    0,
                    2.0f,
                    0,
                    2.4f,
                    0.45f,
                    1.9f,
                    0.90f,
                    0.58f,
                    0.05f
            );

            // كابينة
            drawLocal(
                    0.8f,
                    3.5f,
                    0,
                    1.15f,
                    1.4f,
                    1.45f,
                    0.12f,
                    0.16f,
                    0.18f
            );

            // زجاج أمامي
            drawLocal(
                    0.8f,
                    3.7f,
                    -0.78f,
                    0.9f,
                    0.8f,
                    0.08f,
                    0.08f,
                    0.35f,
                    0.55f
            );

            // سقف
            drawLocal(
                    0.8f,
                    5.05f,
                    0,
                    1.3f,
                    0.18f,
                    1.6f,
                    0.95f,
                    0.55f,
                    0.03f
            );

            // boom
            Matrix.pushMatrix(model);

            Matrix.translateM(
                    model,
                    0,
                    -0.8f,
                    3.0f,
                    0
            );

            Matrix.rotateM(
                    model,
                    0,
                    boomAngle,
                    0,
                    0,
                    1
            );

            drawLocalMatrix(
                    2.8f,
                    0.38f,
                    0.45f,
                    0.90f,
                    0.60f,
                    0.08f,
                    0.45f,
                    0.10f,
                    0.02f
            );

            Matrix.popMatrix(model);

            // stick
            Matrix.pushMatrix(model);

            Matrix.translateM(
                    model,
                    0,
                    -3.1f,
                    2.0f,
                    0
            );

            Matrix.rotateM(
                    model,
                    0,
                    stickAngle,
                    0,
                    0,
                    1
            );

            drawLocalMatrix(
                    2.2f,
                    0.30f,
                    0.40f,
                    0.88f,
                    0.52f,
                    0.03f,
                    0.35f,
                    0.08f,
                    0.01f
            );

            Matrix.popMatrix(model);

            // الدلو
            Matrix.pushMatrix(model);

            Matrix.translateM(
                    model,
                    0,
                    -4.8f,
                    0.7f,
                    0
            );

            Matrix.rotateM(
                    model,
                    0,
                    bucketAngle,
                    0,
                    0,
                    1
            );

            drawLocalMatrix(
                    0,
                    0,
                    0,
                    1.5f,
                    0.8f,
                    1.2f,
                    0.72f,
                    0.38f,
                    0.04f
            );

            Matrix.popMatrix(model);
        }

        private void drawLocal(
                float x,
                float y,
                float z,
                float sx,
                float sy,
                float sz,
                float r,
                float g,
                float b
        ) {

            Matrix.pushMatrix(model);

            Matrix.translateM(
                    model,
                    0,
                    x,
                    y,
                    z
            );

            drawLocalMatrix(
                    0,
                    0,
                    0,
                    sx,
                    sy,
                    sz,
                    r,
                    g,
                    b
            );

            Matrix.popMatrix(model);
        }

        private void drawLocalMatrix(
                float x,
                float y,
                float z,
                float sx,
                float sy,
                float sz,
                float r,
                float g,
                float b
        ) {

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

            current(r, g, b);
        }

        private void box(
                float x,
                float y,
                float z,
                float sx,
                float sy,
                float sz,
                float r,
                float g,
                float b
        ) {

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

            current(r, g, b);
        }

        private void current(
                float r,
                float g,
                float b
        ) {

            Matrix.multiplyMM(
                    mvp,
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
                    mvp,
                    0
            );

            GLES20.glUseProgram(program);

            GLES20.glEnableVertexAttribArray(
                    positionHandle
            );

            cubeBuffer.position(0);

            GLES20.glVertexAttribPointer(
                    positionHandle,
                    3,
                    GLES20.GL_FLOAT,
                    false,
                    0,
                    cubeBuffer
            );

            GLES20.glUniform4f(
                    colorHandle,
                    r,
                    g,
                    b,
                    1f
            );

            GLES20.glUniformMatrix4fv(
                    mvpHandle,
                    1,
                    false,
                    mvp,
                    0
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

        private void clearButtons() {

            forward = false;
            backward = false;
            left = false;
            right = false;

            armUp = false;
            armDown = false;

            bucketOpen = false;
            bucketClose = false;
        }

        private int loadShader(
                int type,
                String code
        ) {

            int shader =
                    GLES20.glCreateShader(type);

            GLES20.glShaderSource(
                    shader,
                    code
            );

            GLES20.glCompileShader(shader);

            return shader;
        }

        // Matrix stack بسيط
        private final float[][] matrixStack =
                new float[16][16];

        private int stackIndex = 0;

        private void pushMatrix(float[] source) {

            if (stackIndex < 16) {

                System.arraycopy(
                        source,
                        0,
                        matrixStack[stackIndex],
                        0,
                        16
                );

                stackIndex++;
            }
        }

        private void popMatrix(float[] destination) {

            if (stackIndex > 0) {

                stackIndex--;

                System.arraycopy(
                        matrixStack[stackIndex],
                        0,
                        destination,
                        0,
                        16
                );
            }
        }
    }
        }
