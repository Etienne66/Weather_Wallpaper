package com.hm.weather.engine;

import com.hm.weather.engine.Utility.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

class TGALoader {
    private static final String TAG = "GL Engine";
    private static final ByteBuffer cTGAcompare;
    private static final ByteBuffer uTGAcompare;
    private static final ByteBuffer ugTGAcompare;

    class TGA {
        int bpp;
        int bytesPerPixel;
        int commentLength;
        ByteBuffer header = ByteBuffer.allocate(15);
        int height;
        ByteBuffer imageData;
        int imageSize;
        final TGALoader loader;
        int type;
        int width;

        public TGA() {
            this.loader = TGALoader.this;
        }
    }

    TGALoader() {
    }

    static {
        byte[] uncompressedgTgaHeader = new byte[3];
        uncompressedgTgaHeader[2] = 3;
        byte[] uncompressedTgaHeader = {0, 0, 2};
        byte[] compressedTgaHeader = {0, 0, 10};
        uTGAcompare = ByteBuffer.allocate(uncompressedTgaHeader.length);
        uTGAcompare.put(uncompressedTgaHeader);
        uTGAcompare.flip();
        cTGAcompare = ByteBuffer.allocate(compressedTgaHeader.length);
        cTGAcompare.put(compressedTgaHeader);
        cTGAcompare.flip();
        ugTGAcompare = ByteBuffer.allocate(uncompressedgTgaHeader.length);
        ugTGAcompare.put(uncompressedgTgaHeader);
        ugTGAcompare.flip();
    }

    private int unsignedByteToInt(byte byte0) {
        return byte0 & 255;
    }

    private void readBuffer(ReadableByteChannel in, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            in.read(buffer);
        }
        buffer.flip();
    }

    public TGA loadTGA(InputStream inputstream) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(3);
        ReadableByteChannel in = Channels.newChannel(inputstream);
        readBuffer(in, header);
        if (uTGAcompare.equals(header) || ugTGAcompare.equals(header)) {
            return loadUncompressedTGA(in);
        }
        if (cTGAcompare.equals(header)) {
            return loadCompressedTGA(in);
        }
        in.close();
        throw new IOException("TGA file be type 2 or type 10 ");
    }

    private TGA loadUncompressedTGA(ReadableByteChannel in) throws IOException {
        Logger.v(TAG, " - reading uncompressed tga");
        TGA tga = new TGA();
        readBuffer(in, tga.header);
        tga.width = (unsignedByteToInt(tga.header.get(10)) << 8) + unsignedByteToInt(tga.header.get(9));
        tga.height = (unsignedByteToInt(tga.header.get(12)) << 8) + unsignedByteToInt(tga.header.get(11));
        tga.bpp = unsignedByteToInt(tga.header.get(13));
        if (tga.width <= 0 || tga.height <= 0 || !(tga.bpp == 24 || tga.bpp == 32 || tga.bpp == 8)) {
            throw new IOException("Invalid header data");
        }
        tga.bytesPerPixel = tga.bpp / 8;
        tga.imageSize = tga.bytesPerPixel * tga.width * tga.height;
        tga.imageData = ByteBuffer.allocate(tga.imageSize);
        Logger.v(TAG, " - " + tga.width + "x" + tga.height + "x" + tga.bpp + "(" + tga.imageSize + ")");
        readBuffer(in, tga.imageData);
        for (int cswap = 0; cswap < tga.imageSize - 2; cswap += tga.bytesPerPixel) {
            byte temp = tga.imageData.get(cswap);
            tga.imageData.put(cswap, tga.imageData.get(cswap + 2));
            tga.imageData.put(cswap + 2, temp);
        }
        return tga;
    }

    private TGA loadCompressedTGA(ReadableByteChannel fTGA) throws IOException {
        Logger.v(TAG, " - reading compressed tga");
        TGA tga = new TGA();
        readBuffer(fTGA, tga.header);
        tga.width = (unsignedByteToInt(tga.header.get(10)) << 8) + unsignedByteToInt(tga.header.get(9));
        tga.height = (unsignedByteToInt(tga.header.get(12)) << 8) + unsignedByteToInt(tga.header.get(11));
        tga.bpp = unsignedByteToInt(tga.header.get(13));
        if (tga.width <= 0 || tga.height <= 0 || !(tga.bpp == 24 || tga.bpp == 32)) {
            throw new IOException("Invalid header data");
        }
        tga.bytesPerPixel = tga.bpp / 8;
        tga.imageSize = tga.bytesPerPixel * tga.width * tga.height;
        tga.imageData = ByteBuffer.allocate(tga.imageSize);
        tga.imageData.position(0);
        tga.imageData.limit(tga.imageData.capacity());
        int pixelcount = tga.height * tga.width;
        int currentpixel = 0;
        int currentbyte = 0;
        ByteBuffer colorbuffer = ByteBuffer.allocate(tga.bytesPerPixel);
        do {
            try {
                ByteBuffer chunkHeaderBuffer = ByteBuffer.allocate(1);
                chunkHeaderBuffer.clear();
                fTGA.read(chunkHeaderBuffer);
                chunkHeaderBuffer.flip();
                int chunkheader = unsignedByteToInt(chunkHeaderBuffer.get());
                if (chunkheader < 128) {
                    int chunkheader2 = chunkheader + 1;
                    for (short counter = 0; counter < chunkheader2; counter = (short) (counter + 1)) {
                        readBuffer(fTGA, colorbuffer);
                        tga.imageData.put(currentbyte, colorbuffer.get(2));
                        tga.imageData.put(currentbyte + 1, colorbuffer.get(1));
                        tga.imageData.put(currentbyte + 2, colorbuffer.get(0));
                        if (tga.bytesPerPixel == 4) {
                            tga.imageData.put(currentbyte + 3, colorbuffer.get(3));
                        }
                        currentbyte += tga.bytesPerPixel;
                        currentpixel++;
                        if (currentpixel > pixelcount) {
                            throw new IOException("Too many pixels read");
                        }
                    }
                    continue;
                } else {
                    int chunkheader3 = chunkheader - 127;
                    readBuffer(fTGA, colorbuffer);
                    for (short counter2 = 0; counter2 < chunkheader3; counter2 = (short) (counter2 + 1)) {
                        tga.imageData.put(currentbyte, colorbuffer.get(2));
                        tga.imageData.put(currentbyte + 1, colorbuffer.get(1));
                        tga.imageData.put(currentbyte + 2, colorbuffer.get(0));
                        if (tga.bytesPerPixel == 4) {
                            tga.imageData.put(currentbyte + 3, colorbuffer.get(3));
                        }
                        currentbyte += tga.bytesPerPixel;
                        currentpixel++;
                        if (currentpixel > pixelcount) {
                            throw new IOException("Too many pixels read");
                        }
                    }
                    continue;
                }
            } catch (IOException e) {
                throw new IOException("Could not read RLE header");
            }
        } while (currentpixel < pixelcount);
        return tga;
    }
}
