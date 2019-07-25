package com.hm.weather;

import android.content.SharedPreferences;
import com.hm.weather.engine.AnimPlayer;
import com.hm.weather.engine.GlobalRand;
import com.hm.weather.engine.GlobalTime;
import com.hm.weather.engine.Mesh;
import com.hm.weather.engine.Scene;
import com.hm.weather.engine.Vector4;
import com.hm.weather.sky_manager.TimeOfDay;
import javax.microedition.khronos.opengles.GL10;

public abstract class SceneBase extends Scene {
    public static boolean pref_useTimeOfDay;
    public static float pref_windSpeed = 3.0f;
    public static Vector4 todColorFinal;
    public static float todSunPosition = 0.0f;
    protected float BG_PADDING = 20.0f;
    protected final boolean DBG = false;
    protected float TREE_ANIMATE_DELAY_MIN = 3.0f;
    protected float TREE_ANIMATE_DELAY_RANGE = 7.0f;
    protected GlobalTime mGlobalTime;
    protected String pref_background;
    protected int pref_numClouds;
    protected int pref_numWisps;
    public Vector4[] pref_todColors;
    protected boolean pref_treeAnim = true;
    protected boolean reloadAssets;
    protected AnimPlayer treesAnim = new AnimPlayer(0, 19, 5.0f, false);
    protected float treesAnimateDelay = 5.0f;

    /* access modifiers changed from: protected */
    public void checkAssetReload(GL10 gl10) {
        if (this.reloadAssets) {
            synchronized (this) {
                this.mMeshManager.unload(gl10);
                this.mTextureManager.unload(gl10);
                precacheAssets(gl10);
                this.reloadAssets = false;
            }
        }
    }

    public void unload(GL10 gl) {
        this.mTextureManager.unload(gl);
        this.mMeshManager.unload(gl);
        this.mThingManager.clear();
    }

    public void numCloudsFromPrefs(SharedPreferences prefs) {
        this.pref_numClouds = prefs.getInt(BaseWallpaperSettings.PREF_NUM_CLOUDS, 10);
    }

    public void numWispsFromPrefs(SharedPreferences prefs) {
        this.pref_numWisps = prefs.getInt(BaseWallpaperSettings.PREF_NUM_WISPS, 5);
    }

    public void windSpeedFromPrefs(SharedPreferences prefs) {
        pref_windSpeed = Float.valueOf(prefs.getString(BaseWallpaperSettings.PREF_WIND_SPEED, "3")).floatValue() * 0.5f;
    }

    public void update(GlobalTime globalTime) {
        this.mGlobalTime = globalTime;
    }

    public void updateTimeOfDay(TimeOfDay tod) {
        if (!pref_useTimeOfDay) {
            todColorFinal.set(1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }
        int iMain = tod.getMainIndex();
        int iBlend = tod.getBlendIndex();
        Vector4.mix(todColorFinal, this.pref_todColors[iMain], this.pref_todColors[iBlend], tod.getBlendAmount());
    }

    /* access modifiers changed from: protected */
    public void drawTree(GL10 gl, float timeDelta) {
        if (this.pref_treeAnim && this.treesAnim.getCount() > 0) {
            this.treesAnimateDelay -= timeDelta;
            if (this.treesAnimateDelay <= 0.0f) {
                this.treesAnimateDelay = this.TREE_ANIMATE_DELAY_MIN + (this.TREE_ANIMATE_DELAY_RANGE * GlobalRand.rand.nextFloat());
                this.treesAnim.reset();
            }
        }
        Mesh tree_terrain = this.mMeshManager.getMeshByName(gl, "trees_overlay_terrain");
        gl.glBindTexture(3553, this.mTextureManager.getTextureID(gl, "trees_overlay"));
        gl.glMatrixMode(5888);
        gl.glPushMatrix();
        if (this.mLandscape) {
            gl.glTranslatef(2.0f, 70.0f, -65.0f);
        } else {
            gl.glTranslatef(-8.0f, 70.0f, -70.0f);
        }
        gl.glScalef(5.0f, 5.0f, 5.0f);
        gl.glBlendFunc(770, 771);
        tree_terrain.render(gl);
        Mesh grass = this.mMeshManager.getMeshByName(gl, "grass_overlay");
        Mesh tree = this.mMeshManager.getMeshByName(gl, "trees_overlay");
        if (!this.pref_treeAnim || this.treesAnim.getCount() >= 1) {
            tree.render(gl);
            grass.render(gl);
        } else {
            this.treesAnim.update(timeDelta);
            tree.renderFrameInterpolated(gl, this.treesAnim.getCurrentFrame(), this.treesAnim.getBlendFrame(), this.treesAnim.getBlendFrameAmount());
            grass.renderFrameInterpolated(gl, this.treesAnim.getCurrentFrame(), this.treesAnim.getBlendFrame(), this.treesAnim.getBlendFrameAmount());
        }
        gl.glPopMatrix();
    }
}
