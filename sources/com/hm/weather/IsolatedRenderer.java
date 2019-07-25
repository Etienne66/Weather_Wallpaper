package com.hm.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.opengl.GLU;
import android.preference.PreferenceManager;
import com.hm.weather.engine.GlobalRand;
import com.hm.weather.engine.GlobalTime;
import com.hm.weather.engine.Scene;
import com.hm.weather.engine.Utility;
import com.hm.weather.engine.Vector3;
import com.hm.weather.sky_manager.TimeOfDay;
import com.hm.weather.sky_manager.WeatherSettingsUtil;
import java.util.Calendar;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class IsolatedRenderer implements OnSharedPreferenceChangeListener {
    static final float BACKGROUND_DISTANCE = 300.0f;
    static final float CALENDAR_UPDATE_INTERVAL = 10.0f;
    static final float CAMERA_X_POSITION = 0.0f;
    static final float CAMERA_X_RANGE = 14.0f;
    static final float CAMERA_Y_POSITION = 0.0f;
    static final float CAMERA_Z_POSITION = 0.0f;
    static final float CAMERA_Z_RANGE = 10.0f;
    private static final boolean DBG = false;
    static final float POSITION_UPDATE_INTERVAL = 300.0f;
    static final int SCENE_CLEAR = 2000;
    static final int SCENE_CLOUDY = 2001;
    static final int SCENE_FOG = 2004;
    static final int SCENE_RAIN = 2005;
    static final int SCENE_SNOW = 2003;
    static final int SCENE_STORM = 2002;
    private static final String TAG = "IsolatedRenderer";
    public static int currentSceneId;
    public static float homeOffsetPercentage = 0.5f;
    public static float horizontalFOV = 45.0f;
    boolean IS_LANDSCAPE = DBG;
    private TimeOfDay _tod = new TimeOfDay();
    private Calendar calendarInstance;
    private Vector3 cameraDir = new Vector3();
    private float cameraFOV = 65.0f;
    private Vector3 cameraPos;
    Context context;
    private Scene currentScene;
    private Vector3 desiredCameraPos;
    private GlobalTime globalTime;
    boolean isPaused;
    private float lastCalendarUpdate;
    private float lastPositionUpdate;
    private GL10 oldGL = null;
    float pref_cameraSpeed = 1.0f;
    boolean pref_superFastDay = DBG;
    SharedPreferences prefs;
    private float screenHeight;
    private float screenRatio = 1.0f;
    private float screenWidth;

    public IsolatedRenderer(Context ctx) {
        homeOffsetPercentage = 0.5f;
        this.isPaused = DBG;
        this.calendarInstance = null;
        this.lastCalendarUpdate = 10.0f;
        this.lastPositionUpdate = 300.0f;
        this.globalTime = new GlobalTime();
        this.cameraPos = new Vector3(0.0f, 0.0f, 0.0f);
        this.desiredCameraPos = new Vector3(0.0f, 0.0f, 0.0f);
        this.currentScene = new SceneClear(ctx);
        currentSceneId = SCENE_CLEAR;
        setContext(ctx);
    }

    private void setContext(Context ctx) {
        this.context = ctx;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        onSharedPreferenceChanged(this.prefs, null);
        this.prefs.registerOnSharedPreferenceChangeListener(this);
    }

    public Context getContext() {
        return this.context;
    }

    public synchronized void onPause() {
        this.isPaused = true;
    }

    public synchronized void onResume() {
        this.lastCalendarUpdate = 10.0f;
        this.lastPositionUpdate = 300.0f;
        this.isPaused = DBG;
    }

    public void precacheAssets(GL10 gl10) {
        this.currentScene.precacheAssets(gl10);
    }

    public void cameraSpeedFromPrefs(SharedPreferences prefs2) {
        this.pref_cameraSpeed = (Float.valueOf(prefs2.getString("pref_cameraspeed", "1")).floatValue() * 0.5f) + 0.5f;
    }

    private void todFromPrefs(SharedPreferences prefs2) {
        this.pref_superFastDay = prefs2.getBoolean("pref_superfastday", DBG);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs2, String key) {
        cameraSpeedFromPrefs(prefs2);
        todFromPrefs(prefs2);
        this.currentScene.updateSharedPrefs(prefs2, key);
    }

    public synchronized void onSceneChanged(int newSceneId) {
        if (newSceneId != currentSceneId) {
            this.currentScene.unload(this.oldGL);
            switch (newSceneId) {
                case SCENE_CLEAR /*2000*/:
                    this.currentScene = new SceneClear(this.context);
                    currentSceneId = SCENE_CLEAR;
                    break;
                case SCENE_CLOUDY /*2001*/:
                    this.currentScene = new SceneCloudy(this.context);
                    currentSceneId = SCENE_CLOUDY;
                    break;
                case SCENE_STORM /*2002*/:
                    this.currentScene = new SceneStorm(this.context);
                    currentSceneId = SCENE_STORM;
                    break;
                case SCENE_SNOW /*2003*/:
                    this.currentScene = new SceneSnow(this.context);
                    currentSceneId = SCENE_SNOW;
                    break;
                case SCENE_FOG /*2004*/:
                    this.currentScene = new SceneFog(this.context);
                    currentSceneId = SCENE_FOG;
                    break;
                case SCENE_RAIN /*2005*/:
                    this.currentScene = new SceneRain(this.context);
                    currentSceneId = SCENE_RAIN;
                    break;
            }
            this.currentScene.load(this.oldGL);
        }
        this.currentScene.setScreenMode(this.IS_LANDSCAPE);
        onSharedPreferenceChanged(this.prefs, null);
    }

    public void onSurfaceChanged(GL10 gl, int w, int h) {
        gl.glViewport(0, 0, w, h);
        this.screenWidth = (float) w;
        this.screenHeight = (float) h;
        this.screenRatio = this.screenWidth / this.screenHeight;
        if (this.screenRatio > 1.0f) {
            this.IS_LANDSCAPE = true;
        } else {
            this.IS_LANDSCAPE = DBG;
        }
        setRenderDefaults(gl);
        gl.glMatrixMode(5889);
        gl.glLoadIdentity();
        if (gl != this.oldGL) {
            this.oldGL = gl;
            this.currentScene.unload(gl);
            this.currentScene.precacheAssets(gl);
        }
        this.currentScene.setScreenMode(this.IS_LANDSCAPE);
        this.currentScene.load(gl);
    }

    public void onSurfaceCreated(GL10 gl10, EGLConfig eglconfig) {
    }

    public void setRenderDefaults(GL10 gl) {
        gl.glHint(3152, 4353);
        gl.glShadeModel(7425);
        gl.glEnable(3553);
        gl.glEnable(3042);
        gl.glAlphaFunc(518, 0.02f);
        gl.glDepthMask(DBG);
        gl.glDepthFunc(515);
        gl.glTexEnvx(8960, 8704, 8448);
        gl.glEnableClientState(32884);
        gl.glEnableClientState(32888);
        gl.glBlendFunc(1, 771);
        gl.glCullFace(1029);
        gl.glActiveTexture(33984);
        gl.glEnable(2903);
        gl.glMatrixMode(5890);
        gl.glPopMatrix();
        gl.glPopMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(5889);
        gl.glPopMatrix();
        gl.glPopMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(5888);
        gl.glPopMatrix();
        gl.glPopMatrix();
        gl.glLoadIdentity();
    }

    public synchronized void drawFrame(GL10 gl) {
        if (!this.isPaused) {
            this.globalTime.updateTime();
            updateCalendar(this.globalTime.sTimeDelta);
            updateCameraPosition(gl, this.globalTime.sTimeDelta);
            gl.glClear(256);
            gl.glMatrixMode(5889);
            gl.glLoadIdentity();
            if (this.IS_LANDSCAPE) {
                GLU.gluPerspective(gl, this.cameraFOV, this.screenRatio, 1.0f, 400.0f);
            } else {
                GLU.gluPerspective(gl, this.cameraFOV, this.screenRatio, 1.0f, 400.0f);
            }
            GLU.gluLookAt(gl, this.cameraPos.x, this.cameraPos.y, this.cameraPos.z, this.cameraPos.x, 400.0f, this.cameraPos.z, 0.0f, 0.0f, 1.0f);
            this.currentScene.draw(gl, this.globalTime);
        }
    }

    public void setTouchPos(float x, float y) {
        Vector3 vPos = new Vector3();
        Utility.adjustScreenPosForDepth(vPos, this.cameraFOV, this.screenWidth, this.screenHeight, x, y, GlobalRand.floatRange(35.0f, 68.0f) - this.cameraPos.y);
        vPos.x += this.cameraPos.x;
    }

    public void updateOffset(float offset) {
        homeOffsetPercentage = offset;
    }

    private void updateCalendar(float timeDelta) {
        this.lastCalendarUpdate += timeDelta;
        if (this.lastCalendarUpdate >= 10.0f || this.calendarInstance == null) {
            this.calendarInstance = Calendar.getInstance();
            this.lastCalendarUpdate = 0.0f;
        }
        if (this.lastPositionUpdate >= 300.0f) {
            float longitude = WeatherSettingsUtil.getLongitude(this.context);
            this._tod.calculateTimeTable(WeatherSettingsUtil.getLatitude(this.context), longitude);
            this.lastPositionUpdate = 0.0f;
        }
        calculateTimeOfDay(timeDelta);
    }

    private void calculateTimeOfDay(float timeDelta) {
        int minutes = (this.calendarInstance.get(11) * 60) + this.calendarInstance.get(12);
        if (this.pref_superFastDay) {
            minutes = (int) ((this.globalTime.msTimeCurrent / 36) % 1440);
        }
        this._tod.update(minutes, true);
        this.currentScene.updateTimeOfDay(this._tod);
    }

    private void updateCameraPosition(GL10 gl, float timeDelta) {
        this.desiredCameraPos.set((28.0f * homeOffsetPercentage) - CAMERA_X_RANGE, 0.0f, 0.0f);
        float rate = 3.5f * timeDelta * this.pref_cameraSpeed;
        float dx = (this.desiredCameraPos.x - this.cameraPos.x) * rate;
        float dy = (this.desiredCameraPos.y - this.cameraPos.y) * rate;
        float dz = (this.desiredCameraPos.z - this.cameraPos.z) * rate;
        this.cameraPos.x += dx;
        this.cameraPos.y += dy;
        this.cameraPos.z += dz;
        this.cameraDir.x = this.cameraPos.x - this.cameraPos.x;
        this.cameraDir.y = 100.0f - this.cameraPos.y;
        if (this.IS_LANDSCAPE) {
            this.cameraFOV = 45.0f;
        } else {
            this.cameraFOV = 70.0f;
        }
        horizontalFOV = this.cameraFOV * this.screenRatio;
    }
}
