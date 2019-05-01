package csci576;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Processor {
    public static final int WIDTH = 480;
    public static final int HEIGHT = 270;

    public static int getRed(int color) {
        int red = color & 0x00ff0000;
        red >>= 16;
        return red;
    }
    
    public static int getGreen(int color) {
        int green = color & 0x0000ff00;
        green >>= 8;
        return green;
    }
    
    public static int getBlue(int color) {
        int blue = color & 0x000000ff;
        return blue;
    }

    
    /**
     * Floods a pic with pink. Modifies the pic matrix
     * @param pic The pic matrix of colors
     * @param x X starting point
     * @param y y starting point
     * @param c The color key
     */
    public static void iterativeFloodFill(int[][] pic, int x, int y, int c) {
        Queue<Point> queue = new LinkedList<Point>();
        queue.add(new Point(x, y));
        
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int color = pic[p.x][p.y];
            
            if (color == c) {
                pic[p.x][p.y] = 0xffff00ff;
                
                if (p.x != 0) {
                    queue.add(new Point(p.x-1, p.y));
                }
                if (p.y != 0) {
                    queue.add(new Point(p.x, p.y-1));
                }
                if (p.x != WIDTH-1) {
                    queue.add(new Point(p.x+1, p.y));
                }
                if (p.y != HEIGHT-1) {
                    queue.add(new Point(p.x, p.y+1));
                }
            }
        }
    }
    
    /**
     * Converts a rgb file into a 2D int array of colors.
     * Assumes image to be of 480x720 because no real reason to assume otherwise
     * @param filename The file to read
     * @return A 2d array of pixels 
     * @throws IOException
     */
    public static int[][] rgbToByteMatrix(String filename) throws IOException {
        FileInputStream f = new FileInputStream(filename);
        
        int img[][] = new int[WIDTH][HEIGHT];
        
        byte bytes[] = new byte[WIDTH*HEIGHT*3];
        
        int offset = 0, numRead = 0;
        while (offset < bytes.length && (numRead = f.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        
        int ind = 0;
        for (int y = 0; y < HEIGHT; ++y) {
            for (int x = 0; x < WIDTH; ++x) {
                int r = bytes[ind] & 0xff;
                int g = bytes[ind+HEIGHT*WIDTH] & 0xff;
                int b = bytes[ind+HEIGHT*WIDTH*2] & 0xff;
                
                float hsv[] = new float[3];
                Color.RGBtoHSB(r, g, b, hsv);
                //High Brightness/Saturation
                if (hsv[1] > 0.8 || hsv[2] > 0.8) {
                    float h = hsv[0] * 240;
                    //Green
                    if (h > 70 && h < 120) {
                        g = 255;
                        r = 0;
                        b = 70;
                    }
                    //Yellow
                    else if (h > 30 && h < 50) {
                        r = 255;
                        g = 255;
                        b = 0;
                    }
                    //Blue
                    else if (h >= 120 && h < 165 && hsv[1] > .7) {
                        r = 0;
                        g = 30;
                        b = 200;
                    }
                    //Red
                    else if ((h > 220 || h < 10) && hsv[1] > .7) {
                        r = 255;
                        g = 0;
                        b = 20;
                    }
                    //White
                    else if (hsv[1] < .1) {
                        r = 255;
                        g = 255;
                        b = 255;
                    }
                    //Everything Else
                    else {
                        r = 255;
                        g = 0;
                        b = 255;
                    }
                }
                //Not enough saturation/brightness
                //So leave as white or as black
                else if (g > 200 && b > 200 && r > 200) {
                    r = 255;
                    g = 255;
                    b = 255;
                }
                else {
                    g = 0;
                    b = 255;
                    r = 255;
                }
                img[x][y] = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                ++ind;
            }
        }
        
        f.close();
        
        return img;
    }
    /**
     * @param filename
     * @return
     * @throws IOException
     */
    public static int[][] mcdToByteArray(String filename) throws IOException {
        FileInputStream f = new FileInputStream(filename);
        
        int img[][] = new int[WIDTH][HEIGHT];
        
        byte bytes[] = new byte[WIDTH*HEIGHT*3];
        
        int offset = 0, numRead = 0;
        while (offset < bytes.length && (numRead = f.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        
        int ind = 0;
        for (int y = 0; y < HEIGHT; ++y) {
            for (int x = 0; x < WIDTH; ++x) {
                int r = bytes[ind] & 0xff;
                int g = bytes[ind+HEIGHT*WIDTH] & 0xff;
                int b = bytes[ind+HEIGHT*WIDTH*2] & 0xff;
                //Only want these yellows/oranges
                if (!(r > 150 && g > 150 && b < 128)) {
                    r = 255;
                    g = 0;
                    b = 255;
                }
                else {
                    r = 255;
                    g = 255;
                    b = 0;
                }
                
                img[x][y] = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                ++ind;
            }
        }
        
        f.close();
        
        return img;
    }
    
    /**
     * Return an array of matrices of the logo images
     * 0 is starbucks, 1 is subway, 2 is nfl, 3 is mcdonalds
     * @return
     * @throws IOException
     */
    public static int[][][] readLogos() throws IOException{
      
        int[][] stb = rgbToByteMatrix(AudioVideoProcessor.adLogos[0]);
        int[][] sbw = rgbToByteMatrix(AudioVideoProcessor.adLogos[1]);
        int[][] nfl = rgbToByteMatrix(AudioVideoProcessor.adLogos[2]);
        int[][] mac = mcdToByteArray(AudioVideoProcessor.adLogos[3]);
        
        //iterativeFloodFill(stb, 0, 0, stb[0][0]);
        iterativeFloodFill(sbw, 0, 0, sbw[0][0]);
        iterativeFloodFill(nfl, 0, 0, nfl[0][0]);
        
        int[][][] logos = {stb, sbw, nfl, mac};
        return logos;
    }
    
    /**
     * Calcualte MSE between two frames. Ignores pink masked pixels in logo
     * @param logo The logo to compare against
     * @param frame The frame to compare against
     * @return The MSE between the two
     */
    public static double calcMSE(int[][] logo, int[][] frame) {
        double sum = 0;
        int h = Math.min(logo.length, frame.length);
        int w = Math.min(logo[0].length, frame[0].length);
        
        for (int i = 0; i < h; ++i) {
            for (int j = 0; j < w; ++j) {
                //Mask out the pink
                if (logo[i][j] != 0xffff00ff) {
                    
                    int rdif = getRed(logo[i][j]) - getRed(frame[i][j]);
                    int gdif = getGreen(logo[i][j]) - getGreen(frame[i][j]);
                    int bdif = getBlue(logo[i][j]) - getBlue(frame[i][j]);
                    
                    sum += rdif*rdif + gdif*gdif + bdif*bdif;
                }
            }
        }
        
        return sum / (h*w);
    }

    public static double calcSubMSE(int[][] logo, int[][] frame, int scale, double offX, double offY) {
        double sum = 0;
        int h = logo.length / scale;
        int w = logo[0].length / scale;
        
        for (int i = 0; i < h; ++i) {
            for (int j = 0; j < w; ++j) {
                if (logo[i*scale][j*scale] != 0xffff00ff) {
                    
                    int rdif = getRed(logo[i*scale][j*scale]) - getRed(frame[(int)(i+offX*h)][(int)(j+offY*w)]);
                    int gdif = getGreen(logo[i*scale][j*scale]) - getGreen(frame[(int)(i+offX*h)][(int)(j+offY*w)]);
                    int bdif = getBlue(logo[i*scale][j*scale]) - getBlue(frame[(int)(i+offX*h)][(int)(j+offY*w)]);
                    
                    sum += rdif*rdif + gdif*gdif + bdif*bdif;
                }
            }
        }
        
        return sum / (h*w);
    }
    
    public static void analyzeVideo(String videopath, int[][][] logos, AudioVideoProcessor.Shot[] shots) throws IOException {
        //Turn file into path
        File f = new File(videopath);
        InputStream videoStream = new FileInputStream(f);
        
        //Some initial data for the for loop
        int numRead = 0;
        int curFrame = 1;
        //I just don't want to allocate more space each time
        byte bytes[] = new byte[3*WIDTH*HEIGHT];
        int[][] frame = new int[WIDTH][HEIGHT];
        
        int curShot = -1;
        
        //Count frames each time and go until end of file (numRead == -1)
        //Note: I start on frame 1, not frame 0
        for (curFrame = 1; numRead != -1; ++curFrame) {
            
            //Ensure we read to the full buffer
            int offset = 0;
            while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }

            //curShot starts at -1 to ensure this block gets called whenever a scene starts
            if (curShot == -1 || shots[curShot].end < curFrame) {
                ++curShot;
                //If we somehow go past the end
                if (curShot == shots.length) {
                    break;
                }
            }
            
            //For checking Subway
            int countSubY[] = new int[5], countSubW[] = new int[5];
            //For checking Starbucks
            int countStarG[] = new int[5], countStarW[] = new int[5];
            //For checking NFL
            int countNFLB[] = new int[5], countNFLR[] = new int[5], countNFLW[] = new int[5];
            //For checking McDonalds
            double countMcdY[] = new double[5];
            int countMcdR[] = new int[5];
            int ind = 0;
            for(int y = 0; y < HEIGHT; y++){
                for(int x = 0; x < WIDTH; x++){
                    //Read RGB from buffer for frame
                    int r = bytes[ind] & 0xff;
                    int g = bytes[ind+HEIGHT*WIDTH] & 0xff;
                    int b = bytes[ind+HEIGHT*WIDTH*2] & 0xff;
                    //Only look at center half
                    if (x < WIDTH / 4 || x > 3 * WIDTH / 4) {
                        g = 0;
                        b = 0;
                        r = 0;
                    }

                    float hsv[] = new float[3];
                    Color.RGBtoHSB(r, g, b, hsv);
                    //High Brightness/Saturation
                    if (hsv[1] > 0.8 || hsv[2] > 0.8) {
                        float h = hsv[0] * 240;
                        //Green
                        if (h > 70 && h < 120 && hsv[1] > .7) {
                            g = 255;
                            r = 0;
                            b = 70;
                            ++countStarG[2*(3*y / HEIGHT)];
                            if (y > HEIGHT / 6 && y < HEIGHT / 2) {
                                ++countStarG[1];
                            }
                            if (y > HEIGHT / 2 && y < 5 * HEIGHT / 6) {
                                ++countStarG[3];
                            }
                        }
                        //Yellow
                        else if (h > 30 && h < 50) {
                            r = 255;
                            g = 255;
                            b = 0;
                            if (x > WIDTH / 2) {
                                ++countSubY[2*(3*y / HEIGHT)];
                                if (y > HEIGHT / 6 && y < HEIGHT / 2) {
                                    ++countSubY[1];
                                }
                                if (y > HEIGHT / 2 && y < 5 * HEIGHT / 6) {
                                    ++countSubY[3];
                                }
                            }

                            if (x > WIDTH / 3 && x < 2 * WIDTH / 3) {
                                ++countMcdY[2*(3*y / HEIGHT)];
                                if (y > HEIGHT / 6 && y < HEIGHT / 2) {
                                    ++countMcdY[1];
                                }
                                if (y > HEIGHT / 2 && y < 5 * HEIGHT / 6) {
                                    ++countMcdY[3];
                                }
                            }
                        }
                        //Orange because McDonalds includes that sometimes too
                        else if (h > 20 && h < 30) {
                            if (x > WIDTH / 3 && x < 2 * WIDTH / 3) {
                                countMcdY[2*(3*y / HEIGHT)] += 1;
                                if (y > HEIGHT / 6 && y < HEIGHT / 2) {
                                    countMcdY[1] += 1;
                                }
                                if (y > HEIGHT / 2 && y < 5 * HEIGHT / 6) {
                                    countMcdY[3] += 1;
                                }
                            }
                        }
                        //Blue
                        else if (h >= 120 && h < 165 && hsv[1] > .7) {
                            r = 0;
                            g = 30;
                            b = 200;
                            ++countNFLB[2*(3*y / HEIGHT)];
                            if (y > HEIGHT / 6 && y < HEIGHT / 2) {
                                ++countNFLB[1];
                            }
                            if (y > HEIGHT / 2 && y < 5 * HEIGHT / 6) {
                                ++countNFLB[3];
                            }
                        }
                        //Red
                        else if ((h > 220 || h < 10) && hsv[1] > .7) {
                            r = 255;
                            g = 0;
                            b = 20;
                            if (x > 2 * WIDTH / 5 && x < 3 * WIDTH / 5) {
                                ++countNFLR[2*(3*y / HEIGHT)];
                                if (y > HEIGHT / 6 && y < HEIGHT / 2) {
                                    ++countNFLR[1];
                                }
                                if (y > HEIGHT / 2 && y < 5 * HEIGHT / 6) {
                                    ++countNFLR[3];
                                }
                            }

                            if (x > WIDTH / 3 && x < 2 * WIDTH / 3) {
                                ++countMcdR[2*(3*y / HEIGHT)];
                                if (y > HEIGHT / 6 && y < HEIGHT / 2) {
                                    ++countMcdR[1];
                                }
                                if (y > HEIGHT / 2 && y < 5 * HEIGHT / 6) {
                                    ++countMcdR[3];
                                }
                            }
                        }
                        //White
                        else if (hsv[1] < .1) {
                            r = 255;
                            g = 255;
                            b = 255;
                            if (x < WIDTH / 2) {
                                ++countSubW[2*(3*y / HEIGHT)];
                                if (y > HEIGHT / 6 && y < HEIGHT / 2) {
                                    ++countSubW[1];
                                }
                                if (y > HEIGHT / 2 && y < 5 * HEIGHT / 6) {
                                    ++countSubY[3];
                                }
                            }
                            if (x > 2 * WIDTH / 5 && x < 3 * WIDTH / 5) {
                                ++countStarW[2*(3*y / HEIGHT)];
                                ++countNFLW[2*(3*y / HEIGHT)];
                                if (y > HEIGHT / 6 && y < HEIGHT / 2) {
                                    ++countStarW[1];
                                    ++countNFLW[1];
                                }
                                if (y > HEIGHT / 2 && y < 5 * HEIGHT / 6) {
                                    ++countStarW[3];
                                    ++countNFLW[3];
                                }
                            }
                        }
                        //Everything Else
                        else {
                            r = 0;
                            g = 0;
                            b = 0;
                        }
                    }
                    //Not enough saturation/brightness
                    //So leave as white or as black
                    else if (g > 200 && b > 200 && r > 200) {
                        r = 255;
                        g = 255;
                        b = 255;
                    }
                    else {
                        g = 0;
                        b = 0;
                        r = 0;
                    }

                    frame[x][y] = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    
                    ind++;
                }
            }
            /*if (curFrame >= 5551 && curFrame <= 6000 || curFrame >= 2401 && curFrame <= 2850) {
                continue;
            }*/
            
            for (int i = 0; i < countStarG.length; ++i) {
                if (countStarG[i] > 2000 && countStarW[i] > 800 && countStarW[i] < 4000) {
                    ++shots[curShot].logos[AudioVideoProcessor.Logo.STARBUCKS.key];
                    //Simple break;
                    i = countStarG.length;
                    //System.out.println(curFrame + ": Starbucks " + i);
                    /*if (curFrame > 000 && !open) {
                        testImage(frame);
                        open = true;
                    }*/
                }
            }
            
            for (int i = 0; i < countSubY.length; ++i) {
                if (countSubY[i] >= 2200 && countSubW[i] >= 2500 && countSubW[i] < 4500) {
                    double mse = calcSubMSE(logos[1], frame, 2, 0.5, i/4.0);
                    if (mse < 20000) {
                        //System.out.println(curFrame + ": Subway " + i);
                        ++shots[curShot].logos[AudioVideoProcessor.Logo.SUBWAY.key];
                        //Simple break;
                        i = countSubY.length;
                    }
                }
            }
            
            for (int i = 0; i < countNFLB.length; ++i) {
                if (countNFLB[i] > 3000 && countNFLR[i] > 1000 && countNFLW[i] > 1000) {
                    //System.out.println(curFrame + ": NFL " + i);
                    ++shots[curShot].logos[AudioVideoProcessor.Logo.NFL.key];
                    //Simple break;
                    i = countNFLB.length;
                }
            }
            
            for (int i = 0; i < countMcdY.length; ++i) {
                if (countMcdY[i] > 1500 && countMcdR[i] > 1000) {
                    //System.out.println(curFrame + ": McDonalds " + i);
                    ++shots[curShot].logos[AudioVideoProcessor.Logo.MCDONALDS.key];
                    //Simple break;
                    i = countMcdY.length;
                }
            }
            
        }
        //out.close();
        //Close the input stream like a responsible adult
        videoStream.close();
    }
    static boolean open = false;
    public static void testImage(int[][] image) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < WIDTH; ++i) {
            for (int j = 0; j < HEIGHT; ++j) {
                img.setRGB(i, j, image[i][j]);
            }
        }
        
        JLabel j = new JLabel(new ImageIcon(img));
        JFrame f = new JFrame("aaa");
        f.add(j);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
    }
 
}
