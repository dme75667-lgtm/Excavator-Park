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

    private final Renderer renderer;

    public Game3DView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        renderer = new Renderer();
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

            renderer.forward = false;
            renderer.backward = false;
            renderer.left = false;
            renderer.right = false;
            renderer.armUp = false;
            renderer.armDown = false;
            renderer.bucketOpen = false;
            renderer.bucketClose = false;

            // حركة الحفارة
            if (x < w * 0.25f && y > h * 0.65f) {
                renderer.left = true;
            } else if (x > w * 0.75f && y > h * 0.65f) {
                renderer.right = true;
            } else if (x > w * 0.35f && x < w * 0.65f && y > h * 0.70f) {
                renderer.forward = true;
            } else if (x > w * 0.35f && x < w * 0.65f && y < h * 0.45f) {
                renderer.backward = true;
            }

            // الذراع
            if (x < w * 0.25f && y < h * 0.45f) {
                renderer.armUp = true;
            }

            if (x > w * 0.75f && y < h * 0.45f) {
                renderer.armDown = true;
            }

            // الدلو
            if (x > w * 0.35f &&
                    x < w * 0.65f &&
                    y > h * 0.45f &&
                    y < h * 0.70f) {

                if (x < w * 0.50f) {
                    renderer.bucketOpen = true;
                } else {
                    renderer.bucketClose = true;
                }
            }

            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            renderer.stop();
            return true;
        }

        return true;
    }

    private static class Renderer implements GLSurfaceView.Renderer {

        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] temp = new float[16];
        private final float[] mvp = new float[16];

        private FloatBuffer vertices;

        private int program;
        private int positionHandle;
        private int colorHandle;
        private int mvpHandle;

        private float x = 0f;
        private float z = 5f;

        private float rotation = 0f;

        private float armAngle = -18f;
        private float secondArm = 22f;
        private float bucketAngle = 25f;

        boolean forward;
        boolean backward;
        boolean left;
        boolean right;

        boolean armUp;
        boolean armDown;

        boolean bucketOpen;
        boolean bucketClose;

        private final float speed = 0.055f;

        private final float[] cube = {

                -0.5f,-0.5f, 0.5f,
                 0.5f,-0.5f, 0.5f,
                 0.5f, 0.5f, 0.5f,

                -0.5f,-0.5f, 0.5f,
                 0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,

                 0.5f,-0.5f,-0.5f,
                -0.5f,-0.5f,-0.5f,
                -0.5f, 0.5f,-0.5f,

                 0.5f,-0.5f,-0.5f,
                -0.5f, 0.5f,-0.5f,
                 0.5f, 0.5f,-0.5f,

                -0.5f,-0.5f,-0.5f,
                -0.5f,-0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,

                -0.5f,-0.5f,-0.5f,
                -0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f,-0.5f,

                 0.5f,-0.5f, 0.5f,
                 0.5f,-0.5f,-0.5f,
                 0.5f, 0.5f,-0.5f,

                 0.5f,-0.5f, 0.5f,
                 0.5f, 0.5f,-0.5f,
                 0.5f, 0.5f, 0.5f,

                -0.5f, 0.5f, 0.5f,
                 0.5f, 0.5f, 0.5f,
                 0.5f, 0.5f,-0.5f,

                -0.5f, 0.5f, 0.5f,
                 0.5f, 0.5f,-0.5f,
                -0.5f, 0.5f,-0.5f,

                -0.5f,-0.5f,-0.5f,
                 0.5f,-0.5f,-0.5f,
                 0.5f,-0.5f, 0.5f,

                -0.5f,-0.5f,-0.5f,
                 0.5f,-0.5f, 0.5f,
                -0.5f,-0.5f, 0.5f
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

            ByteBuffer buffer =
                    ByteBuffer.allocateDirect(cube.length * 4);

            buffer.order(ByteOrder.nativeOrder());

            vertices = buffer.asFloatBuffer();
            vertices.put(cube);
            vertices.position(0);

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
                    (float) width / (float) height;

            Matrix.frustumM(
                    projection,
                    0,
                    -ratio,
                    ratio,
                    -1f,
                    1f,
                    2f,
                    180f
            );
        }

        @Override
        public void onDrawFrame(GL10 gl) {

            GLES20.glClear(
                    GLES20.GL_COLOR_BUFFER_BIT |
                    GLES20.GL_DEPTH_BUFFER_BIT
            );

            update();

            Matrix.setLookAtM(
                    view,
                    0,
                    x,
                    7.5f,
                    z + 14f,
                    x,
                    1.5f,
                    z,
                    0f,
                    1f,
                    0f
            );

            drawWorld();
            drawExcavator();
        }

        private void update() {

            if (forward)
                z -= speed;

            if (backward)
                z += speed;

            if (left) {
                x -= speed;
                rotation -= 1.2f;
            }

            if (right) {
                x += speed;
                rotation += 1.2f;
            }

            if (armUp) {
                armAngle -= 0.7f;
                secondArm += 0.45f;
            }

            if (armDown) {
                armAngle += 0.7f;
                secondArm -= 0.45f;
            }

            if (bucketOpen)
                bucketAngle += 1.0f;

            if (bucketClose)
                bucketAngle -= 1.0f;

            armAngle = Math.max(-55f, Math.min(35f, armAngle));
            secondArm = Math.max(-30f, Math.min(70f, secondArm));
            bucketAngle = Math.max(-45f, Math.min(75f, bucketAngle));

            x = Math.max(-20f, Math.min(20f, x));
            z = Math.max(-25f, Math.min(25f, z));
        }

        private void drawWorld() {

            // الأرض
            box(
                    0f,-0.8f,0f,
                    60f,1f,60f,
                    0.24f,0.52f,0.18f
            );

            // طريق
            box(
                    0f,-0.20f,0f,
                    9f,0.18f,55f,
                    0.10f,0.10f,0.10f
            );

            // خطوط الطريق
            for (int i = -25; i <= 25; i += 5) {

                box(
                        0f,-0.08f,i,
                        0.28f,0.05f,2.1f,
                        1f,0.82f,0.05f
                );
            }

            // منطقة الحفر
            box(
                    14f,-0.15f,-8f,
                    13f,0.35f,12f,
                    0.38f,0.24f,0.10f
            );

            // تراب
            box(
                    11f,0.75f,-8f,
                    3.5f,1.5f,3f,
                    0.45f,0.29f,0.10f
            );

            box(
                    18f,0.9f,-6f,
                    3.5f,1.8f,3f,
                    0.42f,0.27f,0.09f
            );

            // أشجار
            tree(-18f,-12f);
            tree(18f,-15f);
            tree(-18f,13f);
            tree(18f,15f);

            // صخور
            box(
                    -11f,0.3f,-7f,
                    3f,1.6f,2.5f,
                    0.30f,0.30f,0.28f
            );
        }

        private void tree(float tx, float tz) {

            box(
                    tx,1.2f,tz,
                    0.7f,2.5f,0.7f,
                    0.34f,0.18f,0.06f
            );

            box(
                    tx,3.0f,tz,
                    3f,3f,3f,
                    0.06f,0.42f,0.08f
            );
        }

        private void drawExcavator() {

            // الجنزير الأيسر
            box(
                    x - 1.15f,
                    0.35f,
                    z,
                    1.15f,
                    0.75f,
                    3.7f,
                    0.055f,
                    0.055f,
                    0.05f
            );

            // الجنزير الأيمن
            box(
                    x + 1.15f,
                    0.35f,
                    z,
                    1.15f,
                    0.75f,
                    3.7f,
                    0.055f,
                    0.055f,
                    0.05f
            );

            // القاعدة
            box(
                    x,
                    0.95f,
                    z,
                    3.1f,
                    0.45f,
                    2.6f,
                    0.92f,
                    0.55f,
                    0.02f
            );

            // جسم الحفارة
            box(
                    x,
                    1.5f,
                    z,
                    2.65f,
                    1.0f,
                    2.35f,
                    0.95f,
                    0.58f,
                    0.025f
            );

            // الكابينة
            box(
                    x - 0.45f,
                    2.55f,
                    z - 0.25f,
                    1.75f,
                    1.8f,
                    1.7f,
                    0.08f,
                    0.10f,
                    0.12f
            );

            // الزجاج الأمامي
            box(
                    x - 0.45f,
                    2.65f,
                    z - 1.15f,
                    1.35f,
                    1.15f,
                    0.08f,
                    0.05f,
                    0.30f,
                    0.42f
            );

            // سقف
            box(
                    x - 0.45f,
                    3.55f,
                    z - 0.25f,
                    1.95f,
                    0.18f,
                    1.9f,
                    0.88f,
                    0.52f,
                    0.02f
            );

            // الذراع الرئيسي
            Matrix.setIdentityM(model, 0);

            Matrix.translateM(
                    model,
                    0,
                    x,
                    2.45f,
                    z + 2.0f
            );

            Matrix.rotateM(
                    model,
                    0,
                    armAngle,
                    1f,
                    0f,
                    0f
            );

            Matrix.scaleM(
                    model,
                    0,
                    0.65f,
                    0.65f,
                    4.2f
            );

            current(
                    0.92f,
                    0.53f,
                    0.015f
            );

            // الذراع الثاني
            Matrix.setIdentityM(model, 0);

            Matrix.translateM(
                    model,
                    0,
                    x,
                    1.7f,
                    z + 5.3f
            );

            Matrix.rotateM(
                    model,
                    0,
                    -secondArm,
                    1f,
                    0f,
                    0f
            );

            Matrix.scaleM(
                    model,
                    0,
                    0.48f,
                    0.48f,
                    3.0f
            );

            current(
                    0.88f,
                    0.48f,
                    0.015f
            );

            // الدلو
            Matrix.setIdentityM(model, 0);

            Matrix.translateM(
                    model,
                    0,
                    x,
                    0.55f,
                    z + 8.0f
            );

            Matrix.rotateM(
                    model,
                    0,
                    bucketAngle,
                    1f,
                    0f,
                    0f
            );

            Matrix.scaleM(
                    model,
                    0,
                    2.3f,
                    1.25f,
                    1.5f
            );

            current(
                    0.78f,
                    0.40f,
                    0.015f
            );
        }

        private void box(
                float px,
                float py,
                float pz,
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
                    px,
                    py,
                    pz
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

            vertices.position(0);

            GLES20.glEnableVertexAttribArray(
                    positionHandle
            );

            GLES20.glVertexAttribPointer(
                    positionHandle,
                    3,
                    GLES20.GL_FLOAT,
                    false,
                    12,
                    vertices
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

        private void stop() {

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
