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

            renderer.left = x < w * 0.25f;
            renderer.right = x > w * 0.75f;
            renderer.forward = y < h * 0.45f;
            renderer.backward = y > h * 0.70f;

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
        private float z = 4f;

        private float arm = -12f;
        private float bucket = 18f;

        private final float speed = 0.045f;

        boolean left;
        boolean right;
        boolean forward;
        boolean backward;

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
                    0.50f,
                    0.72f,
                    0.90f,
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
                    160f
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
                    z + 12f,
                    x,
                    1.7f,
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

            if (left)
                x -= speed;

            if (right)
                x += speed;

            x = Math.max(-20f, Math.min(20f, x));
            z = Math.max(-20f, Math.min(20f, z));

            arm += 0.15f;
            if (arm > 10f)
                arm = -12f;
        }

        private void drawWorld() {

            // أرض
            box(
                    0f,-0.8f,0f,
                    50f,1f,50f,
                    0.25f,0.55f,0.20f
            );

            // طريق
            box(
                    0f,-0.20f,0f,
                    8f,0.18f,45f,
                    0.12f,0.12f,0.12f
            );

            // خطوط الطريق
            for (int i = -20; i <= 20; i += 5) {

                box(
                        0f,-0.08f,i,
                        0.30f,0.05f,2f,
                        0.95f,0.80f,0.08f
                );
            }

            // منطقة الحفر
            box(
                    15f,-0.20f,-5f,
                    11f,0.40f,10f,
                    0.38f,0.25f,0.12f
            );

            // أكوام التراب
            box(
                    11f,0.75f,-5f,
                    3f,1.5f,3f,
                    0.45f,0.30f,0.12f
            );

            box(
                    19f,0.90f,-3f,
                    3.5f,1.8f,2.8f,
                    0.40f,0.27f,0.10f
            );

            // أشجار
            tree(-16f,-10f);
            tree(16f,-14f);
            tree(-17f,12f);
            tree(17f,13f);

            // صخور
            box(
                    -10f,0.15f,-5f,
                    2.8f,1.5f,2.2f,
                    0.32f,0.32f,0.30f
            );
        }

        private void tree(float tx, float tz) {

            box(
                    tx,1.2f,tz,
                    0.7f,2.5f,0.7f,
                    0.35f,0.20f,0.08f
            );

            box(
                    tx,3.0f,tz,
                    3f,3f,3f,
                    0.08f,0.45f,0.10f
            );
        }

        private void drawExcavator() {

            // الجنزير الأيسر
            box(
                    x - 1.15f,
                    0.35f,
                    z,
                    1.10f,
                    0.75f,
                    3.5f,
                    0.07f,
                    0.07f,
                    0.06f
            );

            // الجنزير الأيمن
            box(
                    x + 1.15f,
                    0.35f,
                    z,
                    1.10f,
                    0.75f,
                    3.5f,
                    0.07f,
                    0.07f,
                    0.06f
            );

            // تفاصيل الجنزير
            for (int i = -1; i <= 1; i++) {

                box(
                        x - 1.72f,
                        0.35f,
                        z + i * 1.05f,
                        0.12f,
                        0.48f,
                        0.70f,
                        0.20f,
                        0.20f,
                        0.18f
                );

                box(
                        x + 1.72f,
                        0.35f,
                        z + i * 1.05f,
                        0.12f,
                        0.48f,
                        0.70f,
                        0.20f,
                        0.20f,
                        0.18f
                );
            }

            // القاعدة
            box(
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

            // الجسم
            box(
                    x,
                    1.45f,
                    z,
                    2.6f,
                    1.0f,
                    2.3f,
                    0.95f,
                    0.62f,
                    0.04f
            );

            // الكابينة
            box(
                    x - 0.45f,
                    2.55f,
                    z - 0.25f,
                    1.75f,
                    1.8f,
                    1.7f,
                    0.10f,
                    0.14f,
                    0.16f
            );

            // الزجاج
            box(
                    x - 0.45f,
                    2.65f,
                    z - 1.14f,
                    1.30f,
                    1.15f,
                    0.08f,
                    0.08f,
                    0.32f,
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
                    0.90f,
                    0.55f,
                    0.03f
            );

            // الذراع الأول
            Matrix.setIdentityM(model,0);

            Matrix.translateM(
                    model,
                    0,
                    x,
                    2.55f,
                    z + 2.0f
            );

            Matrix.rotateM(
                    model,
                    0,
                    arm,
                    1f,
                    0f,
                    0f
            );

            Matrix.scaleM(
                    model,
                    0,
                    0.65f,
                    0.65f,
                    4.0f
            );

            current(
                    0.92f,
                    0.55f,
                    0.02f
            );

            // الذراع الثاني
            Matrix.setIdentityM(model,0);

            Matrix.translateM(
                    model,
                    0,
                    x,
                    1.75f,
                    z + 4.8f
            );

            Matrix.rotateM(
                    model,
                    0,
                    -bucket,
                    1f,
                    0f,
                    0f
            );

            Matrix.scaleM(
                    model,
                    0,
                    0.50f,
                    0.50f,
                    2.8f
            );

            current(
                    0.90f,
                    0.52f,
                    0.02f
            );

            // الدلو
            box(
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

            Matrix.setIdentityM(model,0);

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

            current(r,g,b);
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

            left = false;
            right = false;
            forward = false;
            backward = false;
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
