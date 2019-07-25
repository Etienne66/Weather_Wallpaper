package com.hm.weather.engine;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import javax.microedition.khronos.opengles.GL10;

public class MeshManager {
    private static final String TAG = "GL Engine";
    private String MMPACKAGENAME;
    private Context context;
    private Mesh lastMesh = null;
    private String lastName = null;
    private HashMap meshList = new HashMap();

    public MeshManager(Context context1) {
        this.context = context1;
        this.MMPACKAGENAME = context1.getPackageName();
    }

    private void createFakeMesh(GL10 gl10, String s) {
        float[] vertexList = {-1.08213043E9f, 0.0f, -1.08213043E9f, 0.0f, 0.0f, 1.06535322E9f, 1.06535322E9f, 0.0f, -1.08213043E9f};
        float[] normalList = {1.06535322E9f, 0.0f, 0.0f, 1.06535322E9f, 0.0f, 0.0f, 1.06535322E9f, 0.0f, 0.0f};
        float[] tcList = {-1.08213043E9f, -1.08213043E9f, 0.0f, 1.06535322E9f, 1.06535322E9f, -1.08213043E9f};
        short[] indexList = {0, 1, 2, 1, 0, 2};
        Mesh mesh = new Mesh();
        mesh.createFromArrays(gl10, vertexList, normalList, tcList, indexList, 3, 1, false);
        this.meshList.put(s, mesh);
    }

    public void createMeshFromArrays(GL10 gl10, String s, float[] vertex, float[] normal, float[] tc, short[] indices, int elements, int frames, boolean willBeInterpolated) {
        Mesh mesh = new Mesh();
        mesh.createFromArrays(gl10, vertex, normal, tc, indices, elements, frames, willBeInterpolated);
        this.meshList.put(s, mesh);
    }

    public void createMeshFromFile(GL10 gl10, String s) {
        createMeshFromFile(gl10, s, false, null);
    }

    public void createMeshFromFile(GL10 gl10, String s, boolean willBeInterpolated) {
        createMeshFromFile(gl10, s, willBeInterpolated, null);
    }

    public void createMeshFromFile(GL10 gl10, String filename, boolean willBeInterpolated, Mesh container) {
        boolean fileIsBinary;
        if (isLoaded(filename)) {
            Log.v(TAG, "MeshManager: Already loaded " + filename);
            return;
        }
        Resources resources = this.context.getResources();
        int resId = resources.getIdentifier(filename, "raw", this.MMPACKAGENAME);
        byte[] firstFour = new byte[4];
        InputStream inputstream = resources.openRawResource(resId);
        try {
            inputstream.read(firstFour, 0, 4);
            inputstream.close();
        } catch (IOException e) {
        }
        if (firstFour[0] == 66 && firstFour[1] == 77 && firstFour[2] == 68 && firstFour[3] == 76) {
            fileIsBinary = true;
        } else if (firstFour[0] == 84 && firstFour[1] == 77 && firstFour[2] == 68 && firstFour[3] == 76) {
            fileIsBinary = false;
        } else {
            createFakeMesh(gl10, filename);
            try {
                inputstream.close();
                return;
            } catch (IOException e2) {
                return;
            }
        }
        if (container == null) {
            container = new Mesh();
            InputStream is = resources.openRawResource(resId);
            if (fileIsBinary) {
                container.createFromBinaryFile(gl10, is, filename, willBeInterpolated);
            } else {
                container.createFromTextFile(gl10, is, filename, willBeInterpolated);
            }
        }
        try {
            inputstream.close();
        } catch (IOException e3) {
        }
        this.meshList.put(filename, container);
    }

    public boolean fileExistsOrIsLoaded(String s) {
        if (this.context.getResources().getIdentifier(s, "raw", this.MMPACKAGENAME) != 0) {
            return true;
        }
        if (this.meshList.containsKey(s)) {
            return true;
        }
        return false;
    }

    public Mesh getMeshByName(GL10 gl10, String name) {
        if (name.equals(this.lastName)) {
            return this.lastMesh;
        }
        Mesh mesh = (Mesh) this.meshList.get(name);
        if (mesh == null) {
            createMeshFromFile(gl10, name);
            mesh = (Mesh) this.meshList.get(name);
        }
        this.lastName = name;
        this.lastMesh = mesh;
        return mesh;
    }

    public boolean isLoaded(String s) {
        if (this.meshList.containsKey(s)) {
            return true;
        }
        return false;
    }

    public void setContext(Context context1) {
        this.context = context1;
    }

    public void unload(GL10 gl10) {
        for (String name : this.meshList.keySet()) {
            ((Mesh) this.meshList.get(name)).unload(gl10);
        }
        this.meshList.clear();
        this.lastName = null;
        this.lastMesh = null;
    }
}
