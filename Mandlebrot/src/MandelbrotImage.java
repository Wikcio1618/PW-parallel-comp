
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class MandelbrotImage {

    private static final double xMin = -2.1;
    private static final double xMax = -1.2;
    private static final double yMin = 0.6;
    private static final double yMax = 1.2;
    private BufferedImage image;
    private int xPixels;
    private int yPixels;

    public MandelbrotImage(int xPixels, int yPixels) {
        this.xPixels = xPixels;
        this.yPixels = yPixels;
        this.image = new BufferedImage(xPixels, yPixels, BufferedImage.TYPE_INT_RGB);
    }

    public int[][] computeMandelbrotBlock(double startX, double endX, double startY, double endY, int width, int height) {
        final int maxIter = 1000;
        int[][] colors = new int[height][width];

        double dx = (endX - startX) / width;
        double dy = (endY - startY) / height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double cx = startX + x * dx;
                double cy = startY + y * dy;

                int iteration = 0;
                double zx = 0, zy = 0;

                while (zx * zx + zy * zy < 4 && iteration < maxIter) {
                    double temp = zx * zx - zy * zy + cx;
                    zy = 2.0 * zx * zy + cy;
                    zx = temp;
                    iteration++;
                }

                int color = iteration == maxIter ? 0 : (iteration * 255 / maxIter);
                colors[y][x] = color;
            }
        }
        return colors;
    }

    // Funkcja generująca obraz Mandelbrota
    public BufferedImage generateMandelbrotSeqeuntial(int width, int height) {
        int[][] colors = computeMandelbrotBlock(MandelbrotImage.xMin, MandelbrotImage.xMax, MandelbrotImage.yMin, MandelbrotImage.yMax, xPixels, yPixels);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < height; i++) {
                image.setRGB(i, j, colors[i][j]);
            }
        }

        return image;
    }

    // Funkcja do pomiaru czasu i uśrednienia
    // public static long measureTime(int repetitions, Callable<int[][]> genFunc) {
    //     long totalTime = 0;
    //     for (int i = 0; i < repetitions; i++) {
    //         long startTime = System.nanoTime();
    //         genFunc(xPixels, yPixels);
    //         long endTime = System.nanoTime();
    //         totalTime += (endTime - startTime);
    //     }
    //     return totalTime / repetitions;
    // }
    public void saveImageToFile(String path) throws IOException {
        File outputFile = new File(path + ".png");
        ImageIO.write(image, "png", outputFile);
    }

    public static void main(String[] args) {
        int[] sizes = {32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};
    }
}
