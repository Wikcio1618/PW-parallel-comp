
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public class MandelbrotImage {

    private static final double XMIN = -2.1;
    private static final double XMAX = 0.6;
    private static final double YMIN = -1.2;
    private static final double YMAX = 1.2;

    private final int NUMCORES = Runtime.getRuntime().availableProcessors();
    private ExecutorService executor = Executors.newFixedThreadPool(NUMCORES);
    private final int[][] colors;
    private final int size;

    public MandelbrotImage(int size) {
        this.size = size;
        this.colors = new int[size][size];
    }

    private void computeMandelbrotBlock(double startX, double endX, double startY, double endY, int xPixels, int yPixels, int xOffset) {
        // blocks are of max height but can be cut by width - that's why there is x offset
        final int maxIter = 200;
        double dx = (endX - startX) / xPixels;
        double dy = (endY - startY) / yPixels;

        for (int y = 0; y < yPixels; y++) {
            for (int x = 0; x < xPixels; x++) {
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
                colors[y][x + xOffset] = (color << 16) | (color << 8) | color;
                // System.out.println("Hello from thread " + Thread.currentThread().getName());
            }
        }
    }

    public void saveImageToFile(String path, String name) throws IOException {
        File outputFile = new File(path + "/" + name + ".png");
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, colors[y][x]);
            }
        }
        ImageIO.write(image, "png", outputFile);
    }

    public void computeSequential() {
        computeMandelbrotBlock(XMIN, XMAX, YMIN, YMAX, size, size, 0);
    }

    public void computeWithThreads() throws InterruptedException {
        Thread[] workers = new Thread[NUMCORES];
        double blockWidth = (XMAX - XMIN) / NUMCORES;
        int xSize = size / NUMCORES;
        int pixelsCounter = 0;
        for (int i = 0; i < NUMCORES; i++) {
            double startX = XMIN + blockWidth * i;
            double endX = XMIN + blockWidth * (i + 1);
            double startY = YMIN;
            double endY = YMAX;
            int xPixels = (i == NUMCORES - 1) ? size - pixelsCounter : xSize;
            int yPixels = size;
            int xOffset = pixelsCounter;
            workers[i] = new Thread(() -> computeMandelbrotBlock(startX, endX, startY, endY, xPixels, yPixels, xOffset), "thread" + i);
            workers[i].start();
            pixelsCounter += xPixels;
        }
        for (Thread worker : workers) {
            worker.join();
        }
    }

    public void computeWithPool() {
        double blockWidth = (XMAX - XMIN) / NUMCORES;
        int xSize = size / NUMCORES;
        int pixelsCounter = 0;
        for (int i = 0; i < NUMCORES; i++) {
            double startX = XMIN + blockWidth * i;
            double endX = XMIN + blockWidth * (i + 1);
            double startY = YMIN;
            double endY = YMAX;
            int xPixels = (i == NUMCORES - 1) ? size - pixelsCounter : xSize;
            int yPixels = size;
            int xOffset = pixelsCounter;
            executor.submit(() -> computeMandelbrotBlock(startX, endX, startY, endY, xPixels, yPixels, xOffset));
            pixelsCounter += xPixels;
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        int[] sizes = {32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};
        int reps = 10;
        long start, end;
        MandelbrotImage image;

        try (FileWriter writer = new FileWriter("Mandelbrot/times.csv")) {
            writer.write("N,seq,threads,pool,repool,reps\n");
            for (int size: sizes) {
                writer.write(size + ",");
                image = new MandelbrotImage(size);

                // SEQUENTIAL
                start = System.currentTimeMillis();
                for (int i = 0; i < reps; i++) {
                    image.computeSequential();
                }
                end = System.currentTimeMillis();
                writer.write(end - start + ",");
                image.saveImageToFile("Mandelbrot/images", "seq_" + size);

                // THREADS
                image = new MandelbrotImage(size);
                start = System.currentTimeMillis();
                for (int i = 0; i < reps; i++) {
                    image.computeWithThreads();
                }
                end = System.currentTimeMillis();
                writer.write(end - start + ",");
                image.saveImageToFile("Mandelbrot/images", "thread_" + size);

                // POOL
                image = new MandelbrotImage(size);
                start = System.currentTimeMillis();
                for (int i = 0; i < reps; i++) {
                    image.computeWithPool();
                }
                image.executor.shutdown();
                try {
                    if (!image.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                        System.err.println("Tasks did not finish within the expected time!");
                    }
                } catch (InterruptedException e) {
                    System.err.println("AwaitTermination interrupted!");
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
                end = System.currentTimeMillis();
                writer.write(end - start + ",");
                image.saveImageToFile("Mandelbrot/images", "pool_" + size);

                // POOL WITH REASSIGNMENT
                image = new MandelbrotImage(size);
                start = System.currentTimeMillis();
                for (int i = 0; i < reps; i++) {
                    image.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                    image.computeWithPool();
                    image.executor.shutdown();
                    try {
                        if (!image.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                            System.err.println("Tasks did not finish within the expected time!");
                        }
                    } catch (InterruptedException e) {
                        System.err.println("AwaitTermination interrupted!");
                        Thread.currentThread().interrupt(); // Restore interrupted status
                    }
                }
                end = System.currentTimeMillis();
                writer.write(end - start + ",");
                image.saveImageToFile("Mandelbrot/images", "repool_" + size);

                writer.write(reps + "\n");
            }
        }

    }
}
