package com.hm.weather.engine;

import com.hm.weather.engine.Utility.Logger;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class Mesh {
    private static final String TAG = "GL Engine";
    static final boolean assertionsDisabled;
    private static Tag tagOrigin = null;
    private ShortBuffer bufIndex;
    private ByteBuffer bufIndexDirect;
    private int bufIndexHandle = 0;
    private FloatBuffer bufScratch = null;
    private float[] bufScratchArray = null;
    private ByteBuffer bufScratchDirect = null;
    private FloatBuffer bufTC;
    private ByteBuffer bufTCDirect;
    private int bufTCHandle = 0;
    private Frame[] frames;
    public String meshName;
    protected int numElements;
    protected int numIndices;
    protected int numTriangles;
    private float[] originalVertexArray;
    private HashMap tags = null;

    class Frame {
        public FloatBuffer bufNormal;
        public ByteBuffer bufNormalDirect;
        public int bufNormalHandle = 0;
        public FloatBuffer bufVertex;
        public ByteBuffer bufVertexDirect;
        public int bufVertexHandle = 0;

        Frame() {
        }
    }

    class Tag {
        private float[] normal;
        private float[] position;

        public Tag(int i) {
            this.position = new float[(i * 3)];
            this.normal = new float[(i * 3)];
        }

        public void addNormal(float f, float f1, float f2, int i) {
            this.normal[i * 3] = f;
            this.normal[(i * 3) + 1] = f1;
            this.normal[(i * 3) + 2] = f2;
        }

        public void addPosition(float f, float f1, float f2, int i) {
            this.position[i * 3] = f;
            this.position[(i * 3) + 1] = f1;
            this.position[(i * 3) + 2] = f2;
        }

        public void getNormal(Vector3 vector3, int i) {
            if (i * 3 >= this.position.length) {
                Logger.v(Mesh.TAG, "ERROR: Tried to get tag normal on invalid frame " + i);
                vector3.y = 0.0f;
                vector3.x = 0.0f;
                vector3.z = 1.0f;
                return;
            }
            vector3.x = this.normal[i * 3];
            vector3.y = this.normal[(i * 3) + 1];
            vector3.z = this.normal[(i * 3) + 2];
        }

        public void getPosition(Vector3 vector3, int i) {
            if (i * 3 >= this.position.length) {
                Logger.v(Mesh.TAG, "ERROR: Tried to get tag position on invalid frame " + i);
                vector3.z = 0.0f;
                vector3.y = 0.0f;
                vector3.x = 0.0f;
                return;
            }
            vector3.x = this.position[i * 3];
            vector3.y = this.position[(i * 3) + 1];
            vector3.z = this.position[(i * 3) + 2];
        }

        public String toString(int i) {
            float f = this.position[i * 3];
            float f1 = this.position[(i * 3) + 1];
            float f2 = this.position[(i * 3) + 2];
            float f3 = this.normal[i * 3];
            float f4 = this.normal[(i * 3) + 1];
            return "Tag Pos " + f + " " + f1 + " " + f2 + "   Normal: " + f3 + " " + f4 + " " + this.normal[(i * 3) + 2];
        }
    }

    static {
        if (!desiredAssertionStatus()) {
            assertionsDisabled = true;
        } else {
            assertionsDisabled = false;
        }
    }

    private static boolean desiredAssertionStatus() {
        return true;
    }

    public Mesh() {
        if (tagOrigin == null) {
            tagOrigin = new Tag(1);
            tagOrigin.addPosition(0.0f, 0.0f, 0.0f, 0);
            tagOrigin.addNormal(0.0f, 0.0f, 1.0f, 0);
        }
    }

    private void allocateScratchBuffers(GL10 gl) {
        int n = this.numElements * 3;
        this.bufScratchArray = new float[n];
        this.bufScratchDirect = ByteBuffer.allocateDirect(n * 4);
        this.bufScratchDirect.order(ByteOrder.nativeOrder());
        this.bufScratch = this.bufScratchDirect.asFloatBuffer();
    }

    public void createFromArrays(GL10 gl, float[] vertexs, float[] normals, float[] tcs, short[] indices, int num_elements, int num_frames, boolean willBeInterpolated) {
        boolean useVertexBufferObjects = gl instanceof GL11;
        if (useVertexBufferObjects) {
            Logger.v(TAG, " - using GL11 vertex buffer objects");
        }
        int iCapacity = indices.length;
        this.numTriangles = indices.length / 3;
        this.frames = new Frame[num_frames];
        this.numElements = num_elements;
        if (willBeInterpolated) {
            this.originalVertexArray = vertexs;
            Logger.v(TAG, " - preparing for interpolated animation");
        } else {
            this.originalVertexArray = null;
        }
        if (this.meshName == null) {
            this.meshName = "CreatedFromArrays";
        }
        int length = num_elements * 3;
        int vertexBufferBytes = length * 4;
        int normalBufferBytes = length * 4;
        int tcBufferBytes = num_elements * 2 * 4;
        int indexBufferBytes = iCapacity * 2;
        for (int i = 0; i < num_frames; i++) {
            Frame frame = new Frame();
            this.frames[i] = frame;
            frame.bufVertexDirect = ByteBuffer.allocateDirect(vertexBufferBytes);
            frame.bufVertexDirect.order(ByteOrder.nativeOrder());
            frame.bufVertex = frame.bufVertexDirect.asFloatBuffer();
            frame.bufVertex.clear();
            frame.bufVertex.put(vertexs, i * length, length);
            frame.bufVertex.position(0);
            frame.bufNormalDirect = ByteBuffer.allocateDirect(normalBufferBytes);
            frame.bufNormalDirect.order(ByteOrder.nativeOrder());
            frame.bufNormal = frame.bufNormalDirect.asFloatBuffer();
            frame.bufNormal.clear();
            frame.bufNormal.put(normals, i * length, length);
            frame.bufNormal.position(0);
            if (useVertexBufferObjects) {
                GL11 gl11 = (GL11) gl;
                int[] handleTemp = new int[1];
                gl11.glGenBuffers(1, handleTemp, 0);
                frame.bufVertexHandle = handleTemp[0];
                gl11.glBindBuffer(34962, frame.bufVertexHandle);
                gl11.glBufferData(34962, vertexBufferBytes, frame.bufVertex, 35044);
                gl11.glBindBuffer(34962, 0);
                gl11.glGenBuffers(1, handleTemp, 0);
                frame.bufNormalHandle = handleTemp[0];
                gl11.glBindBuffer(34962, frame.bufNormalHandle);
                gl11.glBufferData(34962, normalBufferBytes, frame.bufNormal, 35044);
                gl11.glBindBuffer(34962, 0);
            }
        }
        this.bufTCDirect = ByteBuffer.allocateDirect(tcBufferBytes);
        this.bufTCDirect.order(ByteOrder.nativeOrder());
        this.bufTC = this.bufTCDirect.asFloatBuffer();
        this.bufTC.clear();
        this.bufTC.put(tcs);
        this.bufTC.position(0);
        this.bufIndexDirect = ByteBuffer.allocateDirect(indexBufferBytes);
        this.bufIndexDirect.order(ByteOrder.nativeOrder());
        this.bufIndex = this.bufIndexDirect.asShortBuffer();
        this.bufIndex.clear();
        this.bufIndex.put(indices);
        this.bufIndex.position(0);
        this.numIndices = this.bufIndex.capacity();
        if (useVertexBufferObjects) {
            GL11 gl112 = (GL11) gl;
            int[] handleTemp2 = new int[1];
            gl112.glGenBuffers(1, handleTemp2, 0);
            this.bufIndexHandle = handleTemp2[0];
            gl112.glBindBuffer(34963, this.bufIndexHandle);
            gl112.glBufferData(34963, indexBufferBytes, this.bufIndex, 35044);
            gl112.glBindBuffer(34963, 0);
            gl112.glGenBuffers(1, handleTemp2, 0);
            this.bufTCHandle = handleTemp2[0];
            gl112.glBindBuffer(34962, this.bufTCHandle);
            gl112.glBufferData(34962, tcBufferBytes, this.bufTC, 35044);
            gl112.glBindBuffer(34962, 0);
        }
    }

    public void createFromBinaryFile(GL10 gl, InputStream inputstream, String name, boolean willBeInterpolated) {
        HashMap tagList = new HashMap();
        Logger.v(TAG, " - reading as binary");
        this.meshName = name;
        DataInputStream dataInputStream = new DataInputStream(inputstream);
        byte[] segchars = new byte[4];
        try {
            dataInputStream.read(segchars, 0, 4);
            if (segchars[0] == 66 && segchars[1] == 77 && segchars[2] == 68 && segchars[3] == 76) {
                try {
                    int fileVersion = dataInputStream.readInt();
                    int fileElements = dataInputStream.readInt();
                    int fileFrames = dataInputStream.readInt();
                    Logger.v(TAG, "version: " + fileVersion + " elements: " + fileElements + " frames: " + fileFrames);
                    dataInputStream.skip(12);
                    dataInputStream.skip(4);
                    dataInputStream.read(segchars, 0, 4);
                    if (segchars[0] == 87 && segchars[1] == 73 && segchars[2] == 78 && segchars[3] == 68) {
                        int numWindings = dataInputStream.readInt();
                        short[] indices = new short[(numWindings * 3)];
                        dataInputStream.skip(8);
                        int curReadIndex = 0;
                        for (int i = 0; i < numWindings; i++) {
                            indices[curReadIndex] = dataInputStream.readShort();
                            indices[curReadIndex + 1] = dataInputStream.readShort();
                            indices[curReadIndex + 2] = dataInputStream.readShort();
                            curReadIndex += 3;
                        }
                        try {
                            dataInputStream.skip(4);
                            dataInputStream.read(segchars, 0, 4);
                            if (segchars[0] == 84 && segchars[1] == 69 && segchars[2] == 88 && segchars[3] == 84) {
                                int numTC = dataInputStream.readInt();
                                float[] tcList = new float[(numTC * 2)];
                                dataInputStream.skip(8);
                                int curReadIndex2 = 0;
                                for (int i2 = 0; i2 < numTC; i2++) {
                                    tcList[curReadIndex2] = dataInputStream.readFloat();
                                    tcList[curReadIndex2 + 1] = dataInputStream.readFloat();
                                    curReadIndex2 += 2;
                                }
                                try {
                                    dataInputStream.skip(4);
                                    dataInputStream.read(segchars, 0, 4);
                                    if (segchars[0] == 86 && segchars[1] == 69 && segchars[2] == 82 && segchars[3] == 84) {
                                        int numVertices = dataInputStream.readInt();
                                        float[] vertexList = new float[(numVertices * 3 * fileFrames)];
                                        int vertScale = dataInputStream.readInt();
                                        if (vertScale == 0) {
                                            vertScale = 128;
                                        }
                                        Logger.i(TAG, "vertScale=" + vertScale);
                                        dataInputStream.skip(4);
                                        int n = numVertices * fileFrames;
                                        int curReadIndex3 = 0;
                                        for (int i3 = 0; i3 < n; i3++) {
                                            if (fileVersion >= 4) {
                                                vertexList[curReadIndex3] = ((float) dataInputStream.readShort()) / ((float) vertScale);
                                                vertexList[curReadIndex3 + 1] = ((float) dataInputStream.readShort()) / ((float) vertScale);
                                                vertexList[curReadIndex3 + 2] = ((float) dataInputStream.readShort()) / ((float) vertScale);
                                            } else {
                                                vertexList[curReadIndex3] = dataInputStream.readFloat();
                                                vertexList[curReadIndex3 + 1] = dataInputStream.readFloat();
                                                vertexList[curReadIndex3 + 2] = dataInputStream.readFloat();
                                            }
                                            curReadIndex3 += 3;
                                        }
                                        try {
                                            dataInputStream.skip(4);
                                            dataInputStream.read(segchars, 0, 4);
                                            if (segchars[0] == 78 && segchars[1] == 79 && segchars[2] == 82 && segchars[3] == 77) {
                                                int numNormals = dataInputStream.readInt();
                                                float[] normalList = new float[(numNormals * 3 * fileFrames)];
                                                dataInputStream.skip(8);
                                                int curReadIndex4 = 0;
                                                for (int i4 = 0; i4 < numNormals * fileFrames; i4++) {
                                                    if (fileVersion >= 3) {
                                                        normalList[curReadIndex4] = ((float) dataInputStream.readByte()) / 127.0f;
                                                        normalList[curReadIndex4 + 1] = ((float) dataInputStream.readByte()) / 127.0f;
                                                        normalList[curReadIndex4 + 2] = ((float) dataInputStream.readByte()) / 127.0f;
                                                    } else {
                                                        normalList[curReadIndex4] = dataInputStream.readFloat();
                                                        normalList[curReadIndex4 + 1] = dataInputStream.readFloat();
                                                        normalList[curReadIndex4 + 2] = dataInputStream.readFloat();
                                                    }
                                                    curReadIndex4 += 3;
                                                }
                                                if (fileVersion >= 5) {
                                                    try {
                                                        dataInputStream.skip(4);
                                                        dataInputStream.read(segchars, 0, 4);
                                                        if (segchars[0] == 84 && segchars[1] == 65 && segchars[2] == 71 && segchars[3] == 83) {
                                                            int numTags = dataInputStream.readInt();
                                                            dataInputStream.skip(8);
                                                            for (int i5 = 0; i5 < numTags; i5++) {
                                                                Tag currentTag = new Tag(fileFrames);
                                                                byte[] rawTagName = new byte[16];
                                                                dataInputStream.read(rawTagName, 0, 16);
                                                                for (int ii = 0; ii < fileFrames; ii++) {
                                                                    currentTag.addPosition(dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readFloat(), ii);
                                                                    currentTag.addNormal(dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readFloat(), ii);
                                                                }
                                                                String tagName = new String(rawTagName).trim();
                                                                tagList.put(tagName, currentTag);
                                                                StringBuilder sb = new StringBuilder(" - Reading tag \"");
                                                                Logger.v(TAG, sb.append(tagName).append("\"").toString());
                                                            }
                                                        } else {
                                                            Logger.v(TAG, " - invalid chunk tag: TAGS");
                                                            return;
                                                        }
                                                    } catch (IOException ex) {
                                                        ex.printStackTrace();
                                                        return;
                                                    }
                                                }
                                                createFromArrays(gl, vertexList, normalList, tcList, indices, fileElements, fileFrames, willBeInterpolated);
                                                if (tagList.size() > 0) {
                                                    this.tags = tagList;
                                                    return;
                                                }
                                                return;
                                            }
                                            Logger.v(TAG, " - invalid chunk tag: NORM");
                                        } catch (IOException ex2) {
                                            ex2.printStackTrace();
                                        }
                                    } else {
                                        Logger.v(TAG, " - invalid chunk tag: BVRT");
                                    }
                                } catch (IOException ex3) {
                                    ex3.printStackTrace();
                                }
                            } else {
                                Logger.v(TAG, " - invalid chunk tag: TEXT");
                            }
                        } catch (IOException ex4) {
                            ex4.printStackTrace();
                        }
                    } else {
                        Logger.v(TAG, " - invalid chunk tag: WIND");
                    }
                } catch (IOException ex5) {
                    Logger.v(TAG, " - ERROR reading model WIND!");
                    ex5.printStackTrace();
                }
            } else {
                Logger.v(TAG, " - invalid chunk tag: BMDL");
            }
        } catch (IOException ex6) {
            Logger.v(TAG, " - ERROR reading model BMDL!");
            ex6.printStackTrace();
        }
    }

    public void createFromTextFile(GL10 gl, InputStream inputstream, String s, boolean flag) {
    }

    public int getLastFrame() {
        return this.frames.length - 1;
    }

    public Tag getTag(String s) {
        return null;
    }

    public void render(GL10 gl10) {
        renderFrame(gl10, 0);
    }

    public void renderFrame(GL10 gl10, int frameNum) {
        if (frameNum >= this.frames.length || frameNum < 0) {
            Logger.v(TAG, "ERROR: Mesh.renderFrame (" + this.meshName + ") given a frame outside of frames.length: " + frameNum);
            frameNum = this.frames.length - 1;
        }
        if (gl10 instanceof GL11) {
            renderFrame_gl11((GL11) gl10, frameNum);
            return;
        }
        gl10.glVertexPointer(3, 5126, 0, this.frames[frameNum].bufVertex);
        gl10.glNormalPointer(5126, 0, this.frames[frameNum].bufNormal);
        gl10.glTexCoordPointer(2, 5126, 0, this.bufTC);
        gl10.glDrawElements(4, this.numIndices, 5123, this.bufIndex);
    }

    public void renderFrameEnvMap(GL10 gl10, int i) {
        gl10.glVertexPointer(3, 5126, 0, this.frames[i].bufVertex);
        gl10.glNormalPointer(5126, 0, this.frames[i].bufNormal);
        gl10.glTexCoordPointer(3, 5126, 0, this.frames[i].bufNormal);
        gl10.glDrawElements(4, this.numIndices, 5123, this.bufIndex);
    }

    public void renderFrameInterpolated(GL10 gl, int frameNum, int frameBlendNum, float blendAmount) {
        if (this.originalVertexArray == null) {
            renderFrame(gl, frameNum);
        } else if (((double) blendAmount) < 0.01d) {
            renderFrame(gl, frameNum);
        } else if (((double) blendAmount) > 0.99d) {
            renderFrame(gl, frameBlendNum);
        } else {
            if (frameNum >= this.frames.length || frameNum < 0) {
                StringBuilder sb = new StringBuilder("ERROR: Mesh.renderFrameInterpolated (");
                sb.append(this.meshName);
                sb.append(") given a frame outside of frames.length: ");
                sb.append(frameNum);
                Logger.v(TAG, sb.toString());
                frameNum = this.frames.length - 1;
            }
            if (frameBlendNum >= this.frames.length || frameBlendNum < 0) {
                StringBuilder sb2 = new StringBuilder("ERROR: Mesh.renderFrameInterpolated (");
                sb2.append(this.meshName);
                sb2.append(") given a blendframe outside of frames.length: ");
                sb2.append(frameBlendNum);
                Logger.v(TAG, sb2.toString());
                frameBlendNum = this.frames.length - 1;
            }
            if (this.bufScratchArray == null) {
                allocateScratchBuffers(gl);
                Logger.v(TAG, this.meshName + " allocated animation buffers");
            }
            int firstFrameOffset = this.numElements * frameNum * 3;
            int blendFrameOffset = this.numElements * frameBlendNum * 3;
            float oneminusblend = 1.0f - blendAmount;
            for (int i = 0; i < this.numElements * 3; i++) {
                this.bufScratchArray[i] = (this.originalVertexArray[firstFrameOffset + i] * oneminusblend) + (this.originalVertexArray[blendFrameOffset + i] * blendAmount);
            }
            this.bufScratch.clear();
            this.bufScratch.put(this.bufScratchArray);
            this.bufScratch.position(0);
            gl.glVertexPointer(3, 5126, 0, this.bufScratch);
            gl.glNormalPointer(5126, 0, this.frames[frameNum].bufNormal);
            gl.glTexCoordPointer(2, 5126, 0, this.bufTC);
            gl.glDrawElements(4, this.numIndices, 5123, this.bufIndex);
        }
    }

    public void renderFrameInterpolatedAgain(GL10 gl10, int i, int j, float f) {
        if (this.originalVertexArray == null) {
            renderFrame(gl10, i);
        } else if (((double) f) < 0.01d) {
            renderFrame(gl10, i);
        } else if (((double) f) > 0.99d) {
            renderFrame(gl10, j);
        } else {
            gl10.glVertexPointer(3, 5126, 0, this.bufScratch);
            gl10.glNormalPointer(5126, 0, this.frames[i].bufNormal);
            gl10.glTexCoordPointer(2, 5126, 0, this.bufTC);
            gl10.glDrawElements(4, this.numIndices, 5123, this.bufIndex);
        }
    }

    public void renderFrameMultiTexture(GL11 gl11, int frameNum, int tex1, int tex2, int combine, boolean envMap) {
        gl11.glActiveTexture(33984);
        gl11.glBindTexture(3553, tex1);
        if (!envMap) {
            gl11.glBindBuffer(34962, this.bufTCHandle);
            gl11.glTexCoordPointer(2, 5126, 0, 0);
        } else {
            gl11.glBindBuffer(34962, this.frames[frameNum].bufNormalHandle);
            gl11.glTexCoordPointer(3, 5126, 0, 0);
        }
        gl11.glTexEnvi(8960, 8704, 8448);
        gl11.glActiveTexture(33985);
        gl11.glEnable(3553);
        gl11.glClientActiveTexture(33985);
        gl11.glEnableClientState(32888);
        gl11.glBindTexture(3553, tex2);
        gl11.glBindBuffer(34962, this.bufTCHandle);
        gl11.glTexCoordPointer(2, 5126, 0, 0);
        gl11.glTexEnvi(8960, 8704, combine);
        gl11.glBindBuffer(34962, this.frames[frameNum].bufVertexHandle);
        gl11.glVertexPointer(3, 5126, 0, 0);
        gl11.glBindBuffer(34962, this.frames[frameNum].bufNormalHandle);
        gl11.glNormalPointer(5126, 0, 0);
        gl11.glBindBuffer(34963, this.bufIndexHandle);
        gl11.glDrawElements(4, this.numIndices, 5123, 0);
        gl11.glBindBuffer(34962, 0);
        gl11.glBindBuffer(34963, 0);
        gl11.glDisable(3553);
        gl11.glActiveTexture(33984);
        gl11.glClientActiveTexture(33984);
    }

    public void renderFrame_gl11(GL11 gl11, int frameNum) {
        renderFrame_gl11_setup(gl11, frameNum);
        renderFrame_gl11_render(gl11);
        renderFrame_gl11_clear(gl11);
    }

    public void renderFrame_gl11_clear(GL11 gl11) {
        gl11.glBindBuffer(34962, 0);
        gl11.glBindBuffer(34963, 0);
    }

    public void renderFrame_gl11_render(GL11 gl11) {
        gl11.glDrawElements(4, this.numIndices, 5123, 0);
    }

    public void renderFrame_gl11_setup(GL11 gl11, int frameNum) {
        if (frameNum >= this.frames.length || frameNum < 0) {
            StringBuilder sb = new StringBuilder("ERROR: Mesh.renderFrame (");
            sb.append(this.meshName).append(") given a frame outside of frames.length: ").append(frameNum);
            Logger.v(TAG, sb.toString());
            frameNum = this.frames.length - 1;
        }
        gl11.glBindBuffer(34962, this.frames[frameNum].bufVertexHandle);
        gl11.glVertexPointer(3, 5126, 0, 0);
        gl11.glBindBuffer(34962, this.frames[frameNum].bufNormalHandle);
        gl11.glNormalPointer(5126, 0, 0);
        gl11.glBindBuffer(34962, this.bufTCHandle);
        gl11.glTexCoordPointer(2, 5126, 0, 0);
        gl11.glBindBuffer(34963, this.bufIndexHandle);
    }

    public void unload(GL10 gl10) {
        boolean isGL11 = gl10 instanceof GL11;
        int[] tmpBuffer = new int[2];
        for (int i = 0; i < this.frames.length; i++) {
            this.frames[i].bufNormal = null;
            this.frames[i].bufNormalDirect = null;
            this.frames[i].bufVertex = null;
            this.frames[i].bufVertexDirect = null;
            if (isGL11) {
                GL11 gl11 = (GL11) gl10;
                tmpBuffer[0] = this.frames[i].bufNormalHandle;
                tmpBuffer[1] = this.frames[i].bufVertexHandle;
                gl11.glDeleteBuffers(2, tmpBuffer, 0);
            }
        }
        this.bufIndex = null;
        this.bufIndexDirect = null;
        this.bufTC = null;
        this.bufTCDirect = null;
        if (isGL11) {
            GL11 gl112 = (GL11) gl10;
            tmpBuffer[0] = this.bufIndexHandle;
            tmpBuffer[1] = this.bufTCHandle;
            gl112.glDeleteBuffers(2, tmpBuffer, 0);
        }
        this.bufScratch = null;
        this.bufScratchArray = null;
    }
}
