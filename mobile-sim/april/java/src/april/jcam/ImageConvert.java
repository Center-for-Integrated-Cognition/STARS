package april.jcam;

import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.imageio.plugins.jpeg.*;

import java.io.*;
import java.util.*;

import april.util.*;

/** Utilities for manipulating the type of BufferedImages, and basic
 * color-space conversion. **/
public class ImageConvert
{
    static boolean warnSlow = true;

    /** Returns an image of half the original resolution. **/
    public static BufferedImage debayerGBRGhalf(String format, int width, int height, byte d[])
    {
        int nwidth = width/2;
        int nheight = height/2;

        BufferedImage im = new BufferedImage(nwidth, nheight, BufferedImage.TYPE_INT_RGB);
        int i[] = ((DataBufferInt) (im.getRaster().getDataBuffer())).getData();

        for (int y = 0; y < nheight; y++) {
            for (int x = 0; x < nwidth; x++) {
                int ix = x*2;
                int iy = y*2;

                int g1 = d[(iy+0)*width+ix+0]&0xff;
                int b1 = d[(iy+0)*width+ix+1]&0xff;
                int r1 = d[(iy+1)*width+ix+0]&0xff;
                int g2 = d[(iy+1)*width+ix+1]&0xff;

                i[y*nwidth+x+0] = (r1<<16) | (((g1+g2)/2)<<8) | (b1);
            }
        }

        return im;
    }

    /** Returns an image of the same resolution as the input. **/
    public static BufferedImage debayerGBRG(String format, int width, int height, byte d[])
    {
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int out[] = ((DataBufferInt) (im.getRaster().getDataBuffer())).getData();

        // loop over each 2x2 bayer block and compute the pixel values for each element.
        for (int y = 0; y < height; y+=2) {
            for (int x = 0; x < width; x+=2) {

                int r = 0, g = 0, b = 0;

                // compute indices into bayer pattern for the nine 2x2 blocks we'll use.
                int X00 = (y-2)*width+(x-2);
                int X01 = (y-2)*width+(x+0);
                int X02 = (y-2)*width+(x+2);
                int X10 = (y+0)*width+(x-2);
                int X11 = (y+0)*width+(x+0);
                int X12 = (y+0)*width+(x+2);
                int X20 = (y+2)*width+(x-2);
                int X21 = (y+2)*width+(x+0);
                int X22 = (y+2)*width+(x+2);

                // handle the edges of the screen.
                if (y < 2) {
                    X00 += 2*width;
                    X01 += 2*width;
                    X02 += 2*width;
                }
                if (y+2 >= height) {
                    X20 -= 2*width;
                    X21 -= 2*width;
                    X22 -= 2*width;
                }
                if (x < 2) {
                    X00 += 2;
                    X10 += 2;
                    X20 += 2;
                }
                if (x+2 >= width) {
                    X02 -= 2;
                    X12 -= 2;
                    X22 -= 2;
                }

                // top left pixel (G)
                r = ((d[X01+width]&0xff) + (d[X11+width]&0xff)) / 2;
                g = d[X11]&0xff;
                b = ((d[X10+1]&0xff) + (d[X11+1]&0xff)) / 2;;
                out[y*width+x] = (r<<16)+(g<<8)+b;

                // top right pixel (B)
                r = ((d[X01+width]&0xff)+(d[X02+width]&0xff)+(d[X01+width]&0xff) + (d[X12+width]&0xff)) / 4;
                g = ((d[X01+width+1]&0xff)+(d[X11]&0xff)+(d[X12]&0xff)+(d[X11+width+1]&0xff)) / 4;
                b = (d[X11+1]&0xff);
                out[y*width+x+1] = (r<<16)+(g<<8)+b;

                // bottom left pixel (R)
                r = (d[X11+width]&0xff);
                g = ((d[X11]&0xff)+(d[X10+width+1]&0xff)+(d[X11+width+1]&0xff)+(d[X21]&0xff)) / 4;
                b = ((d[X10+1]&0xff)+(d[X11+1]&0xff)+(d[X20+1]&0xff)+(d[X21+1]&0xff)) / 4;
                out[y*width+width+x] = (r<<16)+(g<<8)+b;

                // bottom right pixel (G)
                r = ((d[X11+width]&0xff)+(d[X12+width]&0xff)) / 2;
                g = (d[X11+width+1]&0xff);
                b = ((d[X11+1]&0xff)+(d[X21+1]&0xff)) / 2;
                out[y*width+width+x+1] = (r<<16)+(g<<8)+b;
            }
        }

        return im;
    }

    /** Returns an image of half the original resolution. **/
    public static BufferedImage debayerRGGBhalf(String format, int width, int height, byte d[])
    {
        int nwidth = width/2;
        int nheight = height/2;

        BufferedImage im = new BufferedImage(nwidth, nheight, BufferedImage.TYPE_INT_RGB);
        int i[] = ((DataBufferInt) (im.getRaster().getDataBuffer())).getData();

        for (int y = 0; y < nheight; y++) {
            for (int x = 0; x < nwidth; x++) {
                int ix = x*2;
                int iy = y*2;

                int r1 = d[(iy+0)*width+ix+0]&0xff;
                int g1 = d[(iy+0)*width+ix+1]&0xff;
                int g2 = d[(iy+1)*width+ix+0]&0xff;
                int b1 = d[(iy+1)*width+ix+1]&0xff;

                i[y*nwidth+x+0] = (r1<<16) | (((g1+g2)/2)<<8) | (b1);
            }
        }

        return im;
    }

    /** Returns an image of the same resolution as the input. **/
    public static BufferedImage debayerGRBG(String format, int width, int height, byte d[])
    {
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int out[] = ((DataBufferInt) (im.getRaster().getDataBuffer())).getData();

        // loop over each 2x2 bayer block and compute the pixel values for each element.
        for (int y = 0; y < height; y+=2) {
            for (int x = 0; x < width; x+=2) {

                int r = 0, g = 0, b = 0;

                // compute indices into bayer pattern for the nine 2x2 blocks we'll use.
                int X00 = (y-2)*width+(x-2);
                int X01 = (y-2)*width+(x+0);
                int X02 = (y-2)*width+(x+2);
                int X10 = (y+0)*width+(x-2);
                int X11 = (y+0)*width+(x+0);
                int X12 = (y+0)*width+(x+2);
                int X20 = (y+2)*width+(x-2);
                int X21 = (y+2)*width+(x+0);
                int X22 = (y+2)*width+(x+2);

                // handle the edges of the screen.
                if (y < 2) {
                    X00 += 2*width;
                    X01 += 2*width;
                    X02 += 2*width;
                }
                if (y+2 >= height) {
                    X20 -= 2*width;
                    X21 -= 2*width;
                    X22 -= 2*width;
                }
                if (x < 2) {
                    X00 += 2;
                    X10 += 2;
                    X20 += 2;
                }
                if (x+2 >= width) {
                    X02 -= 2;
                    X12 -= 2;
                    X22 -= 2;
                }

                // top left pixel (G)
                b = ((d[X01+width]&0xff) + (d[X11+width]&0xff)) / 2;
                g = d[X11]&0xff;
                r = ((d[X10+1]&0xff) + (d[X11+1]&0xff)) / 2;;
                out[y*width+x] = (r<<16)+(g<<8)+b;

                // top right pixel (B)
                b = ((d[X01+width]&0xff)+(d[X02+width]&0xff)+(d[X01+width]&0xff) + (d[X12+width]&0xff)) / 4;
                g = ((d[X01+width+1]&0xff)+(d[X11]&0xff)+(d[X12]&0xff)+(d[X11+width+1]&0xff)) / 4;
                r = (d[X11+1]&0xff);
                out[y*width+x+1] = (r<<16)+(g<<8)+b;

                // bottom left pixel (R)
                b = (d[X11+width]&0xff);
                g = ((d[X11]&0xff)+(d[X10+width+1]&0xff)+(d[X11+width+1]&0xff)+(d[X21]&0xff)) / 4;
                r = ((d[X10+1]&0xff)+(d[X11+1]&0xff)+(d[X20+1]&0xff)+(d[X21+1]&0xff)) / 4;
                out[y*width+width+x] = (r<<16)+(g<<8)+b;

                // bottom right pixel (G)
                b = ((d[X11+width]&0xff)+(d[X12+width]&0xff)) / 2;
                g = (d[X11+width+1]&0xff);
                r = ((d[X11+1]&0xff)+(d[X21+1]&0xff)) / 2;
                out[y*width+width+x+1] = (r<<16)+(g<<8)+b;
            }
        }

        return im;
    }

    /** Returns an image of the same resolution as the input. **/
    public static BufferedImage debayerRGGB(String format, int width, int height, byte d[])
    {
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int out[] = ((DataBufferInt) (im.getRaster().getDataBuffer())).getData();

        // loop over each 2x2 bayer block and compute the pixel values for each element.
        for (int y = 0; y < height; y+=2) {
            for (int x = 0; x < width; x+=2) {

                int r = 0, g = 0, b = 0;

                // compute indices into bayer pattern for the nine 2x2 blocks we'll use.
                int X00 = (y-2)*width+(x-2);
                int X01 = (y-2)*width+(x+0);
                int X02 = (y-2)*width+(x+2);
                int X10 = (y+0)*width+(x-2);
                int X11 = (y+0)*width+(x+0);
                int X12 = (y+0)*width+(x+2);
                int X20 = (y+2)*width+(x-2);
                int X21 = (y+2)*width+(x+0);
                int X22 = (y+2)*width+(x+2);

                // handle the edges of the screen.
                if (y < 2) {
                    X00 += 2*width;
                    X01 += 2*width;
                    X02 += 2*width;
                }
                if (y+2 >= height) {
                    X20 -= 2*width;
                    X21 -= 2*width;
                    X22 -= 2*width;
                }
                if (x < 2) {
                    X00 += 2;
                    X10 += 2;
                    X20 += 2;
                }
                if (x+2 >= width) {
                    X02 -= 2;
                    X12 -= 2;
                    X22 -= 2;
                }

                // top left pixel (R)
                r = (d[X11]&0xff);
                g = ((d[X01+width]&0xff)+(d[X10+1]&0xff)+(d[X11+1]&0xff)+(d[X11+width]&0xff)) / 4;
                b = ((d[X00+width+1]&0xff)+(d[X10+width+1]&0xff)+(d[X10+width+1]&0xff)+(d[X11+width+1]&0xff)) / 4;
                out[y*width+x] = (r<<16)+(g<<8)+b;

                // top right pixel (G)
                r = ((d[X11]&0xff)+(d[X12]&0xff)) / 2;
                g = (d[X11+1]&0xff);
                b = ((d[X01+width+1]&0xff)+(d[X11+width+1]&0xff)) / 2;
                out[y*width+x+1] = (r<<16)+(g<<8)+b;

                // bottom left pixel (G)
                r = ((d[X11]&0xff)+(d[X21]&0xff)) / 2;
                g = (d[X11+width]&0xff);
                b = ((d[X10+width+1]&0xff)+(d[X11+width+1]&0xff)) / 2;
                out[y*width+width+x] = (r<<16)+(g<<8)+b;

                // bottom right pixel (B)
                r = ((d[X11]&0xff)+(d[X12]&0xff)+(d[X21]&0xff)+(d[X22]&0xff)) / 4;
                g = ((d[X11+1]&0xff)+(d[X11+width]&0xff)+(d[X12+width]&0xff)+(d[X21+1]&0xff))/ 4;
                b = (d[X11+width+1]&0xff);
                out[y*width+width+x+1] = (r<<16)+(g<<8)+b;
            }
        }

        return im;
    }

    public static BufferedImage convertToImage(FrameData frmd)
    {
        return convertToImage(frmd.ifmt.format, frmd.ifmt.width, frmd.ifmt.height, frmd.data);
    }

    public static BufferedImage convertToImage(String format, int width, int height, byte d[])
    {
        if (format.equals("BAYER_GBRG"))
            return debayerGBRG(format, width, height, d);

        if (format.equals("BAYER_RGGB"))
            return debayerRGGB(format, width, height, d);

        if (format.equals("BAYER_GRBG") || format.equals("GRBG"))
            return debayerGRBG(format, width, height, d);

        if (format.equals("GRAY16")) {
            BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

            byte b[] = ((DataBufferByte) (im.getRaster().getDataBuffer())).getData();
            if (b.length == d.length/2) {
                for (int i = 0; i < b.length; i++)
                    b[i] = d[i*2];
            }
            return im;
        }

        if (format.equals("GRAY8")) {
            BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

            byte b[] = ((DataBufferByte) (im.getRaster().getDataBuffer())).getData();
            if (b.length == d.length) {
                for (int i = 0; i < b.length; i++)
                    b[i] = d[i];
            }
            return im;
        }

        if (format.equals("BE_GRAY16")) {
            BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

            byte b[] = ((DataBufferByte) (im.getRaster().getDataBuffer())).getData();
            if (b.length == d.length/2) {
                for (int i = 0; i < b.length; i++)
                    b[i] = d[i*2];
            }
            return im;
        }

        if (format.equals("Y10B"))
            return convertY10BtoGRAY(width, height, d);

        if (format.equals("YUYV"))
            return convertYUYVtoRGB(width, height, d);

        if (format.equals("RGB")) {
            BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int in[] = ((DataBufferInt) (im.getRaster().getDataBuffer())).getData();
            int dpos = 0;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int r = d[dpos++]&0xff;
                    int g = d[dpos++]&0xff;
                    int b = d[dpos++]&0xff;
                    in[y*width+x] = (r<<16) + (g<<8) + b;
                }
            }
            return im;
        }

        if (format.equals("RGBA")) {
            BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int in[] = ((DataBufferInt) (im.getRaster().getDataBuffer())).getData();
            int dpos = 0;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int r = d[dpos++]&0xff;
                    int g = d[dpos++]&0xff;
                    int b = d[dpos++]&0xff;
                    int a = d[dpos++]&0xff;
                    in[y*width+x] = (a<<24) + (r<<16) + (g<<8) + b;
                }
            }
            return im;
        }

        if (format.equals("BGRA")) {
            BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int in[] = ((DataBufferInt) (im.getRaster().getDataBuffer())).getData();
            int dpos = 0;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int b = d[dpos++]&0xff;
                    int g = d[dpos++]&0xff;
                    int r = d[dpos++]&0xff;
                    int a = d[dpos++]&0xff;

                    in[y*width+x] = (a<<24) + (r<<16) + (g<<8) + b;
                }
            }
            return im;
        }

        if (d.length == (width*height)) { //format.equals("GRAY8")) {
            BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

            byte b[] = ((DataBufferByte) (im.getRaster().getDataBuffer())).getData();
            if (b.length == d.length)
                System.arraycopy(d, 0, b, 0, d.length);
            return im;
        }

        if (format.equals("MJPG")) {
            try {
                ImageReader reader = ImageIO.getImageReadersBySuffix("jpg").next();
                reader.setInput(new MemoryCacheImageInputStream(new ByteArrayInputStream(d)));

                JPEGImageReadParam params = new JPEGImageReadParam();
                params.setDecodeTables(new JPEGQTable[] { JPEGQTable.K1Luminance},
                                       new JPEGHuffmanTable[] { JPEGHuffmanTable.StdDCLuminance,
                                                                JPEGHuffmanTable.StdDCChrominance },
                                       new JPEGHuffmanTable[] { JPEGHuffmanTable.StdACLuminance,
                                                                JPEGHuffmanTable.StdACChrominance });

                BufferedImage im = reader.read(0, params);
                reader.dispose();
                return im;
            } catch (IOException ex) {
                System.out.println("ImageConvert: MJPG decode failed: "+ex);
                return null;
            }
        }

        System.out.println("ImageConvert: Unknown type "+format);

        return null;
    }

    public static BufferedImage convertYUYVtoRGB(int width, int height, byte yuyv[])
    {
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int sstride = width*2;
        int dstride = width*3;

        int rgb[] = ((DataBufferInt) im.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < height; i++) {

            for (int j = 0; j < width / 2; j++) {
                int y1 = yuyv[i*sstride + 4*j+0]&0xff;
                int u  = yuyv[i*sstride + 4*j+1]&0xff;
                int y2 = yuyv[i*sstride + 4*j+2]&0xff;
                int v  = yuyv[i*sstride + 4*j+3]&0xff;

                int cb = ((u-128) * 454)>>8;
                int cr = ((v-128) * 359)>>8;
                int cg = ((v-128) * 183 + (u-128) * 88)>>8;
                int r, g, b;

                r = clamp(y1 + cr);
                b = clamp(y1 + cb);
                g = clamp(y1 - cg);
                rgb[i*width + 2*j+0] = (r<<16) | (g<<8) | b;

                r = clamp(y2 + cr);
                b = clamp(y2 + cb);
                g = clamp(y2 - cg);
                rgb[i*width + 2*j+1] = (r<<16) | (g<<8) | b;
            }
        }
        return im;
    }

    /** Converts a BGR buffer to HSV, returning a BGR BufferedImage
     * The values BGR correspond to HSV values scaled 0-255
     */
    public static BufferedImage convertRGBtoHSV(int width, int height, byte rgb[])
    {
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        byte hsv[] = ((DataBufferByte) im.getRaster().getDataBuffer()).getData();

        int stride = width*3;
        double hscale = 255.0/360.0;
        double sscale = 255.0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {

                int b = rgb[i*stride + 3*j+0]&0xff;
                int g = rgb[i*stride + 3*j+1]&0xff;
                int r = rgb[i*stride + 3*j+2]&0xff;

                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));

                int h, s, v;

                if (max == min)
                    h = 0;
                else if (max == r)
                    h = (int)(60.0 * ((double)(g-b)/(double)(max-min)) + 360) % 360;
                else if (max == g)
                    h = (int)(60.0 * ((double)(b-r)/(double)(max-min)) + 120);
                else // max == b
                    h = (int)(60.0 * ((double)(r-g)/(double)(max-min)) + 240);
                h = (int) ((double)(h)*hscale);

                if (max == 0)
                    s = 0;
                else
                    s = (int) (sscale * (1.0 - ((double)min/(double)max)));

                v = max;

                hsv[i*stride + 3*j+0] = (byte) h;
                hsv[i*stride + 3*j+1] = (byte) s;
                hsv[i*stride + 3*j+2] = (byte) v;
            }
        }
        return im;
    }

    /** Convert the format Y10B to USHORT_GRAY. This format is used by the kinect,
     * among other things, and packs 10 bits of grayscale data into a
     * a byte array.
     **/
    public static BufferedImage convertY10BtoGRAY(int w, int h, byte[] y10b)
    {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);

        short[] buf = ((DataBufferUShort)(im.getRaster().getDataBuffer())).getData();

        // Every 5 bytes yields 4 pixels.
        // Handle all 4 pixels at a type, discarding the last couple for now XXX
        // [p0_9:2 | p0_1:0 , p1_9:4 | p1_3:0 , p2_9:6 | p2_5:0, p3_9:8 | p3_7:0]
        int bidx = 0;
        for (int i = 0; i < y10b.length; i+=5) {
            buf[bidx+0] = (short)((y10b[i] & 0xff) << 2 |
                                  (y10b[i+1] & 0xc0) >> 6);
            buf[bidx+1] = (short)((y10b[i+1] & 0x3f) << 4 |
                                  (y10b[i+2] & 0xf0) >> 4);
            buf[bidx+2] = (short)((y10b[i+2] & 0x0f) << 6 |
                                  (y10b[i+3] & 0xfc) >> 2);
            buf[bidx+3] = (short)((y10b[i+3] & 0x03) << 8 |
                                  (y10b[i+4] & 0xff) >> 0);

            // System.out.printf("%x %x %x %x %x\n", y10b[i],y10b[i+1],y10b[i+2],y10b[i+3],y10b[i+4]);

            // Convert 10 bit gray values to 16 bit equivalents
            buf[bidx+0] =(short)((double)(buf[bidx+0])/0x3ff * 0xffff);
            buf[bidx+1] =(short)((double)(buf[bidx+1])/0x3ff * 0xffff);
            buf[bidx+2] =(short)((double)(buf[bidx+2])/0x3ff * 0xffff);
            buf[bidx+3] =(short)((double)(buf[bidx+3])/0x3ff * 0xffff);

            bidx += 4;
        }

        return im;
    }

    /**
     * Ensure an image is of the right format, converting it to the
     * format if necessary.
     * @param in The input image, in any format.
     * @param type The desired type, e.g. BufferedImage.TYPE_3BYTE_BGR
     * @return An image with analagous content as in, but of the
     * requested type. Or, if the input image was already in the
     * requested format, the input image is returned.
     **/
    public static BufferedImage convertImage(BufferedImage in, int type)
    {
        if (in.getType()==type)
            return in;

        if (warnSlow) {
            System.out.println("ImageConvert: Performing slow image type conversion");
            warnSlow = false;
        }

        int w = in.getWidth();
        int h = in.getHeight();

        BufferedImage out=new BufferedImage(w,h,type);
        Graphics g = out.getGraphics();

        g.drawImage(in, 0, 0, null);

        g.dispose();

        return out;
    }

    static final int clamp(double v)
    {
        if (v < 0)
            return 0;
        if (v > 255)
            return 255;
        return (int) v;
    }

    static final int clamp(int v)
    {
        if (v < 0)
            return 0;
        if (v > 255)
            return 255;
        return (int) v;
    }

    public static BufferedImage RGBtoHSV(BufferedImage _in)
    {
        return RGBtoHSV(_in, 0xffffff);
    }

    public static BufferedImage RGBtoHSV(BufferedImage _in, int outputmask)
    {
        int width = _in.getWidth(), height = _in.getHeight();
        _in = convertImage(_in, BufferedImage.TYPE_INT_RGB);
        int in[] = ((DataBufferInt) (_in.getRaster().getDataBuffer())).getData();

        BufferedImage _out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int out[] = ((DataBufferInt) (_out.getRaster().getDataBuffer())).getData();

        double hscale = 255.0/360.0;
        double sscale = 255.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = in[y*width + x];
                double r = ((rgb>>16)&0xff)/255.0;
                double g = ((rgb>>8)&0xff)/255.0;
                double b = ((rgb)&0xff)/255.0;

                double max = Math.max(r, Math.max(g, b));
                double min = Math.min(r, Math.min(g, b));

                double h, s, v;

                if (max == min)
                    h = 0;
                else if (max == r)
                    h = (60.0 * ((g-b)/(max-min)) + 360);
                else if (max == g)
                    h = (60.0 * ((b-r)/(max-min)) + 120);
                else // max == b
                    h = (60.0 * ((r-g)/(max-min)) + 240);

                if (max == 0)
                    s = 0;
                else
                    s = (int) (sscale * (1.0 - ((double)min/(double)max)));

                v = max*255.0;
                h = h * 255.0 / 360.0;

                int hi = (int) h;

                int si = (int) s;
                int vi = (int) v;

                out[y*width + x] = ((hi<<16) + (si<<8) + (vi)) & outputmask;
            }
        }

        return _out;
    }

    public static BufferedImage HSVtoRGB(BufferedImage _in)
    {
        int in[] = ((DataBufferInt) (_in.getRaster().getDataBuffer())).getData();

        BufferedImage _out = new BufferedImage(_in.getWidth(), _in.getHeight(), BufferedImage.TYPE_INT_RGB);
        int out[] = ((DataBufferInt) (_out.getRaster().getDataBuffer())).getData();

        for (int i=0; i < in.length; i++)
            out[i] = in[i];
        HSVtoRGB(out);

        return _out;
    }

    // Convert HSV to RGB without allocating a destination array
    public static void HSVtoRGB(int im[])
    {
        for (int j=0; j < im.length; j++) {
            int hsv = im[j];
            int hi = (hsv >> 16) & 0xFF;
            int si = (hsv >> 8) & 0xFF;
            int vi = (hsv) & 0xFF;

            double h, s, v;
            // stretch h range to [0, 360)
            h = hi * 360.0 / 255;
            h = (h + 360) % 360;

            // convert to range for math
            // hue [0, 6), saturation [0, 1), value [0, 1)
            h = h / 60;
            s = ((double) si) / 255;
            v = ((double) vi) / 255;

            int i = (int) Math.floor(h);
            double f = h - i;
            if (i%2 == 0)
                f = 1 - f;

            double m = v * (1 - s);
            double n = v * (1 - s*f);

            // scale to 255
            int vo = (int) (v * 255);
            int no = (int) (n * 255);
            int mo = (int) (m * 255);

            // apply
            switch (i)
            {
                case 0:
                    im[j] =  0xFF000000 | vo << 16 | no << 8 | mo;
                    break;
                case 1:
                    im[j] =  0xFF000000 | no << 16 | vo << 8 | mo;
                    break;
                case 2:
                    im[j] =  0xFF000000 | mo << 16 | vo << 8 | no;
                    break;
                case 3:
                    im[j] =  0xFF000000 | mo << 16 | no << 8 | vo;
                    break;
                case 4:
                    im[j] =  0xFF000000 | no << 16 | mo << 8 | vo;
                    break;
                case 5:
                    im[j] =  0xFF000000 | vo << 16 | mo << 8 | no;
                    break;
                default:
                    im[j] = 0;
                    break;
            }
        }
    }

    public static BufferedImage RGBtoYUV(BufferedImage _in)
    {
        int width = _in.getWidth(), height = _in.getHeight();
        _in = convertImage(_in, BufferedImage.TYPE_INT_RGB);
        int in[] = ((DataBufferInt) (_in.getRaster().getDataBuffer())).getData();

        BufferedImage _out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int out[] = ((DataBufferInt) (_out.getRaster().getDataBuffer())).getData();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = in[y*width + x];
                int r = (rgb>>16)&0xff;
                int g = (rgb>>8)&0xff;
                int b = (rgb)&0xff;

                /*
                  int yc = clamp(0.299*r    + 0.587*g   + 0.114*b + 128);
                  int uc = clamp(-0.14713*r - 0.28886*g + 0.436*b + 128);
                  int vc = clamp(0.615*r    - 0.51499*g - 0.10001*b + 128);
                */
                /*
                  int yc = clamp(((66*r + 129*g + 25*b + 128) >> 8) + 16);
                  int uc = clamp(((-38*r - 74*g + 112*b + 128) >> 8) + 128);
                  int vc = clamp(((112*r - 94*g - 18*b + 128) >> 8) + 128);
                */

                int yc = Math.min(Math.abs(2104*r + 4130*g + 802*b + 4096 + 131072) >> 13, 235);
                int uc = Math.min(Math.abs(-1214*r - 2384*g + 3598*b + 4096 + 1048576) >> 13, 240);
                int vc = Math.min(Math.abs(3598*r - 3013*g - 585*b + 4096 + 1048576) >> 13, 240);

                out[y*width+x] = (yc<<16) + (uc<<8) + (vc);
            }
        }

        return _out;
    }
}
