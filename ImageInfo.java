package csci576;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ImageInfo {
    private int height;
    private int width;
    private int colorModel;
    private int length;
    private byte[] data;
    private BufferedImage bufferedImage;

    public ImageInfo(String filename, int width, int height, int colorModel) {

        this.colorModel = colorModel;
        this.height = height;
        this.width = width;
        this.bufferedImage = new BufferedImage(this.width, this.height, this.colorModel);
        this.data = new byte[this.width * this.height * 3];
        setBufferedImage(filename);
    }

    public void setBufferedImage(String filename) {
        getDataFromFile(filename);
        setBufferedImage(this.data);
    }

    public void setBufferedImage(byte[] data) {
        this.data = data;
        int idx = 0;
        for (int ydx = 0; ydx < this.height; ydx++) {
            for (int xdx = 0; xdx < this.width; xdx++) {
                this.bufferedImage.setRGB(xdx, ydx, 0xff000000 | ((this.data[idx] & 0xff) << 16) | ((this.data[idx + this.height * this.width] & 0xff) << 8) | (this.data[idx + this.height * (this.width << 1)] & 0xff));
                idx++;
            }
        }
    }

    public ImageInfo(int width, int height, int colorModel) {
        this.colorModel = colorModel;
        this.width = width;
        this.height = height;
        this.data = new byte[this.width * this.height * 3];
        this.bufferedImage = new BufferedImage(this.width, this.height, this.colorModel);
    }

    public static void resize(ImageInfo imageInfo, ImageInfo expectedImageInfo, float scaleWidth, float scaleHeight, int antiAliasing, int analysis) {
        float FAC_TWO = 2.0f;
        if (analysis == 0) {
            for (int ydx = 0; ydx < expectedImageInfo.getHeight(); ydx++) {
                for (int xdx = 0; xdx < expectedImageInfo.width(); xdx++) {
                    int[] rgbArray = antiAliasing == 0 ? imageInfo.getRGB(xdx, ydx) : imageInfo.getAverageRGBPixel((int) Math.floor(xdx / scaleWidth), (int) Math.floor(ydx / scaleHeight));
                    expectedImageInfo.setRGB(xdx, ydx, 0xff000000 | ((rgbArray[0] & 0xff) << 16) | ((rgbArray[1] & 0xff) << 8) | (rgbArray[2] & 0xff));
                }
            }
        } else if (analysis == 1) {
            if (antiAliasing == 0) {
                int resizeType = -1;
                int squareSide = 0;
                int side1 = 0;
                int side2 = 0;
                if (expectedImageInfo.getHeight() > expectedImageInfo.width()) {
                    resizeType = 0;
                    int squareSideSrcImage = imageInfo.width();
                    squareSide = expectedImageInfo.width();
                    float ratio = (float) squareSide / squareSideSrcImage;

                    side1 = (int) Math.floor((expectedImageInfo.getHeight() - squareSide) / FAC_TWO);
                    side2 = (int) Math.ceil((expectedImageInfo.getHeight() - squareSide) / FAC_TWO);

                    int side1src = (int) Math.floor((imageInfo.getHeight() - squareSideSrcImage) / FAC_TWO);
                    int side2src = (int) Math.ceil((imageInfo.getHeight() - squareSideSrcImage) / FAC_TWO);

                    for (int y = 0; y < expectedImageInfo.getHeight(); y++) {
                        for (int x = 0; x < expectedImageInfo.width(); x++) {
                            if (y < side1) {
                                int[] rgb = imageInfo.getRGB((int) Math.floor(x / scaleWidth), (int) Math.floor(y / ((float) side1 / side1src)));
                                expectedImageInfo.setRGB(x, y, 0xff000000 | ((rgb[0] & 0xff) << 16) | ((rgb[1] & 0xff) << 8) | (rgb[2] & 0xff));
                            } else if (y > (side1 + squareSide)) {
                                float scale3 = (float) side2 / side2src;
                                int[] rgb = imageInfo.getRGB((int) Math.floor(x / scaleWidth), (int) Math.floor((y - side1 - squareSide) / scale3 + side1src + squareSideSrcImage));
                                expectedImageInfo.setRGB(x, y, 0xff000000 | ((rgb[0] & 0xff) << 16) | ((rgb[1] & 0xff) << 8) | (rgb[2] & 0xff));
                            } else {
                                int[] rgb = imageInfo.getRGB((int) Math.floor(x / ratio), (int) Math.floor((y - side1 + side1src) / ratio));
                                expectedImageInfo.setRGB(x, y, 0xff000000 | ((rgb[0] & 0xff) << 16) | ((rgb[1] & 0xff) << 8) | (rgb[2] & 0xff));
                            }
                        }
                    }
                } else {
                    resizeType = 1;
                    int squareSideSrc = imageInfo.getHeight();
                    squareSide = expectedImageInfo.getHeight();
                    float ratio = (float) squareSide / squareSideSrc;

                    side1 = (int) Math.floor((expectedImageInfo.width() - squareSide) / FAC_TWO);
                    side2 = (int) Math.ceil((expectedImageInfo.width() - squareSide) / FAC_TWO);

                    int side1src = (int) Math.floor((imageInfo.width() - squareSideSrc) / FAC_TWO);
                    int side2src = (int) Math.ceil((imageInfo.width() - squareSideSrc) / FAC_TWO);

                    for (int ydx = 0; ydx < expectedImageInfo.getHeight(); ydx++) {
                        for (int xdx = 0; xdx < expectedImageInfo.width(); xdx++) {
                            if (xdx < side1) {
                                int[] rgbArray = imageInfo.getRGB((int) Math.floor(xdx / ((float) side1 / side1src)), (int) Math.floor(ydx / scaleHeight));
                                expectedImageInfo.setRGB(xdx, ydx, 0xff000000 | ((rgbArray[0] & 0xff) << 16) | ((rgbArray[1] & 0xff) << 8) | (rgbArray[2] & 0xff));
                            } else if (xdx > (side1 + squareSide)) {
                                float scale3 = (float) side2 / side2src;
                                int[] rgbArray = imageInfo.getRGB((int) Math.floor((xdx - side1 - squareSide) / scale3 + side1src + squareSideSrc), (int) Math.floor(ydx / scaleHeight));
                                expectedImageInfo.setRGB(xdx, ydx, 0xff000000 | ((rgbArray[0] & 0xff) << 16) | ((rgbArray[1] & 0xff) << 8) | (rgbArray[2] & 0xff));
                            } else {
                                int[] rgbArray = imageInfo.getRGB((int) Math.floor((xdx - side1 + side1src) / ratio), (int) Math.floor(ydx / ratio));
                                expectedImageInfo.setRGB(xdx, ydx, 0xff000000 | ((rgbArray[0] & 0xff) << 16) | ((rgbArray[1] & 0xff) << 8) | (rgbArray[2] & 0xff));
                            }

                        }
                    }
                }
            } else {
                int resizeType = -1;
                int squareSide = 0;
                int sideOne = 0;
                int sideTw0 = 0;
                if (expectedImageInfo.getHeight() > expectedImageInfo.width()) {
                    resizeType = 0; // Vertical
                    int squareSideSrc = imageInfo.width();
                    squareSide = expectedImageInfo.width();
                    float ratio = (float) squareSide / squareSideSrc;

                    sideOne = (int) Math.floor((expectedImageInfo.getHeight() - squareSide) / FAC_TWO);
                    sideTw0 = (int) Math.ceil((expectedImageInfo.getHeight() - squareSide) / FAC_TWO);

                    int side1src = (int) Math.floor((imageInfo.getHeight() - squareSideSrc) / FAC_TWO);
                    int side2src = (int) Math.ceil((imageInfo.getHeight() - squareSideSrc) / FAC_TWO);

                    for (int yds = 0; yds < expectedImageInfo.getHeight(); yds++) {
                        for (int xdx = 0; xdx < expectedImageInfo.width(); xdx++) {
                            if (yds < sideOne) {
                                int[] rgb = imageInfo.getAverageRGBPixel((int) Math.floor(xdx / scaleWidth), (int) Math.floor(yds / ((float) sideOne / side1src)));
                                expectedImageInfo.setRGB(xdx, yds, 0xff000000 | ((rgb[0] & 0xff) << 16) | ((rgb[1] & 0xff) << 8) | (rgb[2] & 0xff));
                            } else if (yds > (sideOne + squareSide)) {
                                float scale3 = (float) sideTw0 / side2src;
                                int[] rgb = imageInfo.getAverageRGBPixel((int) Math.floor(xdx / scaleWidth), (int) Math.floor((yds - sideOne - squareSide) / scale3 + side1src + squareSideSrc));
                                expectedImageInfo.setRGB(xdx, yds, 0xff000000 | ((rgb[0] & 0xff) << 16) | ((rgb[1] & 0xff) << 8) | (rgb[2] & 0xff));
                            } else {
                                int[] rgb = imageInfo.getAverageRGBPixel((int) Math.floor(xdx / ratio), (int) Math.floor((yds - sideOne + side1src) / ratio));
                                expectedImageInfo.setRGB(xdx, yds, 0xff000000 | ((rgb[0] & 0xff) << 16) | ((rgb[1] & 0xff) << 8) | (rgb[2] & 0xff));
                            }
                        }
                    }
                } else {
                    resizeType = 1;
                    int squareSideSrc = imageInfo.getHeight();
                    squareSide = expectedImageInfo.getHeight();
                    float ratio = (float) squareSide / squareSideSrc;

                    sideOne = (int) Math.floor((expectedImageInfo.width() - squareSide) / FAC_TWO);
                    sideTw0 = (int) Math.ceil((expectedImageInfo.width() - squareSide) / FAC_TWO);

                    int side1src = (int) Math.floor((imageInfo.width() - squareSideSrc) / FAC_TWO);
                    int side2src = (int) Math.ceil((imageInfo.width() - squareSideSrc) / FAC_TWO);

                    for (int ydx = 0; ydx < expectedImageInfo.getHeight(); ydx++) {
                        for (int xdx = 0; xdx < expectedImageInfo.width(); xdx++) {
                            if (xdx < sideOne) {
                                int[] rgb = imageInfo.getAverageRGBPixel((int) Math.floor(xdx / ((float) sideOne / side1src)), (int) Math.floor(ydx / scaleHeight));
                                expectedImageInfo.setRGB(xdx, ydx, 0xff000000 | ((rgb[0] & 0xff) << 16) | ((rgb[1] & 0xff) << 8) | (rgb[2] & 0xff));
                            } else if (xdx > (sideOne + squareSide)) {
                                float scale3 = (float) sideTw0 / side2src;
                                int[] rgb = imageInfo.getAverageRGBPixel((int) Math.floor((xdx - sideOne - squareSide) / scale3 + side1src + squareSideSrc), (int) Math.floor(ydx / scaleHeight));
                                expectedImageInfo.setRGB(xdx, ydx, 0xff000000 | ((rgb[0] & 0xff) << 16) | ((rgb[1] & 0xff) << 8) | (rgb[2] & 0xff));
                            } else {
                                int[] rgb = imageInfo.getAverageRGBPixel((int) Math.floor((xdx - sideOne + side1src) / ratio), (int) Math.floor(ydx / ratio));
                                expectedImageInfo.setRGB(xdx, ydx, 0xff000000 | ((rgb[0] & 0xff) << 16) | ((rgb[1] & 0xff) << 8) | (rgb[2] & 0xff));
                            }

                        }
                    }
                }
            }
        }
    }

    private int[] getAverageRGBPixel(int x, int y) {
        {
            float[] rgbArr = new float[3];
            for (int i = 0; i < 3; i++) {
                rgbArr[i] = 0;
            }
            int[] rgbIntArr = new int[3];
            float FAC_9 = 9.0f;
            float FAC_6 = 6.0f;
            float FAC_4 = 4.0f;

            boolean cond1 = x > 0 && y > 0 && x < this.width - 1 && y < this.height - 1;
            boolean cond2 = x == 0 && y == 0;
            boolean cond3 = x == 0 && y == this.height - 1;
            boolean cond4 = x == this.width - 1 && y == 0;
            boolean cond5 = x == this.width - 1 && y == this.height - 1;

            boolean cond8 = x == this.width - 1;
            boolean cond9 = y == this.height - 1;

            if (cond1) {
                for (int idx = x - 1; idx <= x + 1; idx++) {
                    for (int jdx = y - 1; jdx <= y + 1; jdx++) {
                        int[] rgbArray = this.getRGB(idx, jdx);
                        for (int cdx = 0; cdx < 3; cdx++) {
                            rgbArr[cdx] += rgbArray[cdx] / FAC_9;
                        }
                    }
                }
            } else if (cond2 || cond3) {
                for (int idx = 0; idx <= 1; idx++) {
                    for (int jdx = cond3 ? y - 1 : 0; jdx <= (cond3 ? y : 1); jdx++) {
                        int[] rgbArray = this.getRGB(idx, jdx);
                        for (int cdx = 0; cdx < 3; cdx++) {
                            rgbArr[cdx] += rgbArray[cdx] / FAC_4;
                        }
                    }
                }
            } else if (cond4 || cond5) {
                for (int idx = x - 1; idx <= x; idx++) {
                    for (int jdx = cond5 ? y - 1 : 0; jdx <= (cond5 ? y : 1); jdx++) {
                        int[] rgbArray = this.getRGB(idx, jdx);
                        for (int cdx = 0; cdx < 3; cdx++) {
                            rgbArr[cdx] += rgbArray[cdx] / FAC_4;
                        }
                    }
                }
            } else if (x == 0 || y == 0) {
                for (int idx = y == 0 ? x - 1 : 0; idx <= (y == 0 ? x + 1 : 1); idx++) {
                    for (int jdx = y == 0 ? 0 : y - 1; jdx <= (y == 0 ? 1 : y + 1); jdx++) {
                        int[] rgbArray = this.getRGB(idx, jdx);
                        for (int cdx = 0; cdx < 3; cdx++) {
                            rgbArr[cdx] += rgbArray[cdx] / FAC_6;
                        }
                    }
                }
            } else if (cond8 || cond9) {
                for (int idx = x - 1; idx <= (cond9 ? x + 1 : x); idx++) {
                    for (int jdx = y - 1; jdx <= (cond9 ? y : y + 1); jdx++) {
                        int[] rgbArray = this.getRGB(idx, jdx);
                        for (int cdx = 0; cdx < 3; cdx++) {
                            rgbArr[cdx] += rgbArray[cdx] / FAC_6;
                        }
                    }
                }
            }
            for (int idx = 0; idx < 3; idx++) {
                rgbIntArr[idx] = (int) Math.floor(rgbArr[idx]);
            }
            return rgbIntArr;
        }
    }

    public BufferedImage copyData() {
        return new BufferedImage(this.bufferedImage.getColorModel(), this.bufferedImage.copyData(null), false, null);
    }

    private void getDataFromFile(String filename) {
        try {
            File file = new File(filename);
            InputStream inputStream = new FileInputStream(file);
            this.length = (int) file.length();
            int offset = 0, numRead = 0;
            while (offset < this.length && (numRead = inputStream.read(this.data, offset, this.length - offset)) >= 0) {
                offset += numRead;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public BufferedImage getImg() {
        return this.bufferedImage;
    }

    public int getHeight() {
        return this.height;
    }

    public int width() {
        return this.width;
    }

    public void setRGB(int x, int y, int pix) {
        this.bufferedImage.setRGB(x, y, pix);
    }

    public int[] getRGB(int x, int y) {
        int xTemp = x, yTemp = y;
        if (x >= this.width) xTemp--;
        if (y >= this.height) yTemp--;
        int pixValue = this.bufferedImage.getRGB(xTemp, yTemp);

        int[] rgbArray = new int[3];
        rgbArray[0] = (pixValue >> 16) & 0xff;
        rgbArray[1] = (pixValue >> 8) & 0xff;
        rgbArray[2] = (pixValue & 0xff);
        return rgbArray;
    }
}