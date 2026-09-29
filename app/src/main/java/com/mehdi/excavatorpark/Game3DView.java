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

        float w = getWidth();
        float h = getHeight();

        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {

            renderer.left  = x < w * 0.25f;
            renderer.right = x > w * 0.75f;

            renderer.forward  = y < h * 0.45f;
            renderer.backward = y > h * 0.70f;

            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            renderer.stop();
            return true;
        }

        return true;
    }

    private static class Renderer3D implements GLSurfaceView.Renderer {

        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] temp = new float[16];
        private final float[] mvp = new float[16];

        private FloatBuffer cube;

        private int program;
        private int positionHandle;
        private int colorHandle;
        private int mvpHandle;

        private float machineX = 0f;
        private float machineZ = 4f;

        private float armAngle = -15f;
        private float bucketAngle = 20f;

        private final float speed = 0.045f;

        boolean left;
        boolean right;
        boolean forward;
        boolean backward;

        private final float[] vertices = {

                // Front
                -0.5f,-0.5f, 0.5f,
                 0.5f,-0.5f, 0.5f,
                 0.5f, 0.5f, 0.5f,

                -0.5f,-0.5f, 0.5f,
                 0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,

                // Back
                 0.5f,-0.5f,-0.5f,
                -0.5f,-0.5f,-0.5f,
                -0.5f, 0.5f,-0.5f,

                 0.5f,-0.5f,-0.5f,
                -0.5f, 0.5f,-0.5f,
                 0.5f, 0.5f,-0.5f,

                // Left
                -0.5f,-0.5f,-0.5f,
                -0.5f,-0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,

                -0.5f,-0.5f,-0.5f,
                -0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f,-0.5f,

                // Right
                 0.5f,-0.5f, 0.5f,
                 0.5f,-0.5f,-0.5f,
                 0.5f, 0.5f,-0.5f,

                 0.5f,-0.5f, 0.5f,
                 0.5f, 0.5f,-0.5f,
                 0.5f, 0.5f, 0.5f,

                // Top
                -0.5f,0.5f,0.5f,
                 0.5f,0.5f,0.5f,
                 0.5f,0.5f,-0.5f,

                -0.5f,0.5f,0.5f,
                 0.5f,0.5f,-0.5f,
                -0.5f,0.5f,-0.5f,

                // Bottom
                -0.5f,-0.5f,-0.5f,
                 0.5f,-0.5f,-0.5f,
                 0.5f,-0.5f,0.5f,

                -0.5f,-0.5f,-0.5f,
                 0.5f,-0.5f,0.5f,
                -0.5f,-0.5f,0.5f
        };

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

            GLES20.glClearColor(
                    0.48f,
                    0.70f,
                    0.88f,
                    1f
            );

            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);

            ByteBuffer bb =
                    ByteBuffer.allocateDirect(vertices.length * 4);

            bb.order(ByteOrder.nativeOrder());

            cube = bb.asFloatBuffer();
            cube.put(vertices);
            cube.position(0);

            String vertexShader =
                    "uniform mat4 uMVP;" +
                    "attribute vec4 position;" +
                    "void main(){" +
                    "gl_Position=uMVP*position;" +
                    "}";

            String fragmentShader =
                    "precision mediump float;" +
                    "uniform vec4 color;" +
                    "void main(){" +
                    "gl_FragColor=color;" +
                    "}";

            int vs = loadShader(
                    GLES20.GL_VERTEX_SHADER,
                    vertexShader
            );

            int fs = loadShader(
                    GLES20.GL_FRAGMENT_SHADER,
                    fragmentShader
            );

            program = GLES20.glCreateProgram();

            GLES20.glAttachShader(program, vs);
            GLES20.glAttachShader(program, fs);

            GLES20.glLinkProgram(program);

            positionHandle =
                    GLES20.glGetAttribLocation(
                            program,
                            "position"
                    );

            colorHandle =
                    GLES20.glGetUniformLocation(
                            program,
                            "color"
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

            if (height == 0)
                height = 1;

            GLES20.glViewport(
                    0,
                    0,
                    width,
                    height
            );

            float ratio =
                    (float) width / height;

            Matrix.frustumM(
                    projection,
                    0,
                    -ratio,
                    ratio,
                    -1,
                    1,
                    2,
                    150
            );
        }

        @Override
        public void onDrawFrame(GL10 gl) {

            GLES20.glClear(
                    GLES20.GL_COLOR_BUFFER_BIT |
                    GLES20.GL_DEPTH_BUFFER_BIT
            );

            updateMachine();

            Matrix.setLookAtM(
                    view,
                    0,

                    machineX,
                    7.5f,
                    machineZ + 11f,

                    machineX,
                    1.7f,
                    machineZ,

                    0,
                    1,
                    0
            );

            drawWorld();
            drawExcavator();
        }

        private void drawWorld() {

            // الأرض
            cube(
                    0,
                    -0.8f,
                    0,
                    50,
                    1,
                    50,
                    0.25f,
                    0.55f,
                    0.20f
            );

            // الطريق
            cube(
                    0,
                    -0.25f,
                    0,
                    8,
                    0.18f,
                    45,
                    0.13f,
                    0.13f,
                    0.13f
            );

            // خطوط الطريق
            for (int z = -20; z <= 20; z += 5) {

                cube(
                        0,
                        -0.13f,
                        z,
                        0.3f,
                        0.05f,
                        2,
                        1f,
                        0.85f,
                        0.15f
                );
            }

            // منطقة الحفر
            cube(
                    15,
                    -0.15f,
                    -5,
                    10,
                    0.35f,
                    9,
                    0.38f,
                    0.25f,
                    0.12f
            );

            // أكوام تراب
            cube(
                    11,
                    0.7f,
                    -5,
                    2.5f,
                    1.5f,
                    2.5f,
                    0.42f,
                    0.29f,
                    0.12f
            );

            cube(
                    19,
                    0.8f,
                    -3,
                    3,
                    1.7f,
                    2.4f,
                    0.40f,
                    0.27f,
                    0.10f
            );

            // أشجار
            tree(-16, -10);
            tree(16, -14);
            tree(-17, 12);
            tree(17, 13);

            // صخور
            cube(
                    -10,
                    0.2f,
                    -5,
                    2.5f,
                    1.5f,
                    2,
                    0.32f,
                    0.32f,
                    0.30f
            );
        }

        private void tree(float x, float z) {

            cube(
                    x,
                    1.2f,
                    z,
                    0.7f,
                    2.4f,
                    0.7f,
                    0.35f,
                    0.20f,
                    0.08f
            );

            cube(
                    x,
                    3f,
                    z,
                    3f,
                    3f,
                    3f,
                    0.08f,
                    0.45f,
                    0.10f
            );
        }

        private void drawExcavator() {

            float x = machineX;
            float z = machineZ;

            // =====================
            // الجنزير الأيسر
            // =====================

            cube(
                    x - 1.15f,
                    0.35f,
                    z,
                    1.05f,
                    0.75f,
                    3.4f,
                    0.08f,
                    0.08f,
                    0.07f
            );

            // =====================
            // الجنزير الأيمن
            // =====================

            cube(
                    x + 1.15f,
                    0.35f,
                    z,
                    1.05f,
                    0.75f,
                    3.4f,
                    0.08f,
                    0.08f,
                    0.07f
            );

            // عجلات الجنزير
            for (int i = -1; i <= 1; i++) {

                cube(
                        x - 1.7f,
                        0.35f,
                        z + i * 1.0f,
                        0.15f,
                        0.45f,
                        0.65f,
                        0.20f,
                        0.20f,
                        0.18f
                );

                cube(
                        x + 1.7f,
                        0.35f,
                        z + i * 1.0f,
                        0.15f,
                        0.45f,
                        0.65f,
                        0.20f,
                        0.20f,
                        0.18f
                );
            }

            // قاعدة الدوران
            cube(
                    x,
                    0.95f,
                    z,
                    3.0f,
                    0.45f,
                    2.5f,
                    0.95f,
                    0.58f,
                    0.03f
            );

            // جسم الحفارة
            cube(
                    x,
                    1.45f,
                    z,
                    2.5f,
                    1.0f,
                    2.3f,
                    0.95f,
                    0.62f,
                    0.04f
            );

            // الكابينة
            cube(
                    x - 0.45f,
                    2.55f,
                    z - 0.25f,
                    1.7f,
                    1.8f,
                    1.7f,
                    0.12f,
                    0.16f,
                    0.18f
            );

            // زجاج أمامي
            cube(
                    x - 0.45f,
                    2.65f,
                    z - 1.12f,
                    1.25f,
                    1.15f,
                    0.08f,
                    0.08f,
                    0.30f,
                    0.38f
            );

            // سقف
            cube(
                    x - 0.45f,
                    3.55f,
                    z - 0.25f,
                    1.9f,
                    0.18f,
                    1.9f,
                    0.90f,
                    0.55f,
                    0.03f
            );

            // =====================
            // الذراع
            // =====================

            Matrix.setIdentityM(model, 0);

            Matrix.translateM(
                    model,
                    0,
                    x,
                    2.5f,
                    z + 2.0f
            );

            Matrix.rotateM(
                    model,
                    0,
                    armAngle,
                    1,
                    0,
                    0
            );

            Matrix.scaleM(
                    model,
                    0,
                    0.65f,
                    0.65f,
                    4.0f
            );

            drawCurrentModel(
                    0.92f,
                    0.55f,
                    0.02f
            );

            // =====================
            // الذراع الثاني
            // =====================

            Matrix.setIdentityM(model, 0);

            Matrix.translateM(
                    model,
                    0,
                    x,
                    1.7f,
                    z + 4.8f
            );

            Matrix.rotateM(
                    model,
                    0,
                    -bucketAngle,
                    1,
                    0,
                    0
            );

            Matrix.scaleM(
                    model,
                    0,
                    0.50f,
                    0.50f,
                    2.8f
            );

            drawCurrentModel(
                    0.90f,
                    0.52f,
                    0.02f
            );

            // =====================
            // الدلو
            // =====================

            cube(
                    x,
                    0.75f,
                    z + 7.0f,
                    2.2f,
                    1.3f,
                    1.6f,
                    0.82f,
                    0.46f,
                    0.02f
            );
        }

        private void updateMachine() {

            if (forward)
                machineZ -= speed;

            if (backward)
                machineZ += speed;

            if (left)
                machineX -= speed;

            if (right)
                machineX += speed;

            machineX =
                    Math.max(
                            -20f,
                            Math.min(20f, machineX)
                    );

            machineZ =
                    Math.max(
                            -20f,
                            Math.min(20f, machineZ)
                    );
        }

        private void stop() {

            left = false;
            right = false;
            forward = false;
            backward = false;
        }

        private void cube(
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

            drawCurrentModel(r, g, b);
        }

        private void drawCurrentModel(
                float r,
                float g,
                float b) {

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

            cube.position(0);

            GLES20.glEnableVertexAttribArray(
                    positionHandle
            );

            GLES20.glVertexAttribPointer(
                    positionHandle,
                    3,
                    GLES20.GL_FLOAT,
                    false,
                    12,
                    cube
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
