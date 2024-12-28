
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class Sort {

    public static <T extends Comparable<T>> void quickSortPar(ArrayList<T> arr) {
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new QuickSortTask<>(arr, 0, arr.size() - 1));
    }

    static class QuickSortTask<T extends Comparable<T>> extends RecursiveAction {

        private final ArrayList<T> arr;
        private final int lo;
        private final int hi;
        private static final int THRESHOLD = 16; // Below this size, use sequential sort

        public QuickSortTask(ArrayList<T> arr, int lo, int hi) {
            this.arr = arr;
            this.lo = lo;
            this.hi = hi;
        }

        @Override
        protected void compute() {
            if (lo >= hi) {
                return;
            }

            // If the partition is small, sort sequentially
            if (hi - lo <= THRESHOLD) {
                quickRec(arr, lo, hi);
                return;
            }

            // Partition the array
            T pivot = arr.get(hi);
            int idx = lo - 1;
            for (int i = lo; i < hi; i++) {
                if (arr.get(i).compareTo(pivot) <= 0) {
                    idx++;
                    swap(arr, i, idx);
                }
            }
            swap(arr, idx + 1, hi);

            // Fork tasks for left and right partitions
            QuickSortTask<T> leftTask = new QuickSortTask<>(arr, lo, idx);
            QuickSortTask<T> rightTask = new QuickSortTask<>(arr, idx + 2, hi);

            // invokeAll(leftTask, rightTask);
            leftTask.fork();
            rightTask.compute();
            leftTask.join();
        }
    }

    public static <T extends Comparable<T>> void quickSortSeq(ArrayList<T> arr) {
        quickRec(arr, 0, arr.size() - 1);
    }

    private static <T extends Comparable<T>> void quickRec(ArrayList<T> arr, int lo, int hi) {
        if (lo >= hi) {
            return;
        }
        T pivot = arr.get(hi);
        int idx = lo - 1;
        for (int i = lo; i < hi; i++) {
            if (arr.get(i).compareTo(pivot) <= 0) {
                idx++;
                swap(arr, i, idx);
            }
        }
        swap(arr, idx + 1, hi);

        quickRec(arr, lo, idx);
        quickRec(arr, idx + 2, hi);
    }

    public static <T> void swap(ArrayList<T> arr, int i, int j) {
        T temp = arr.get(i);
        arr.set(i, arr.get(j));
        arr.set(j, temp);
    }

    public static void main(String[] args) throws IOException {
        Random r = new Random();
        ArrayList<Integer> arr = new ArrayList<>();
        int sizes[] = {100, 1000, 10_000, 100_000, 1_000_000, 2_500_000, 5_000_000, 7_500_000, 10_000_000, 12_500_000, 15_000_000, 17_500_000, 20_000_000, 30_000_000, 40_000_000, 50_000_000};

        try (FileWriter writer = new FileWriter("Sorting/times.csv")) {
            writer.write("N,seq,par\n");
            for (int size : sizes) {
                writer.write(size + ",");

                arr.clear();
                for (int i = 0; i < size; i++) {
                    arr.add(r.nextInt());
                }
                long start = System.currentTimeMillis();
                quickSortSeq(arr);
                long end = System.currentTimeMillis();
                long time = end - start;
                writer.write(time + ",");
                System.out.println("Finished sequential with size " + size + " in " + time + " ms");
                // System.out.println(arr);

                arr.clear();
                for (int i = 0; i < size; i++) {
                    arr.add(r.nextInt());
                }
                start = System.currentTimeMillis();
                quickSortPar(arr);

                end = System.currentTimeMillis();
                time = end - start;
                writer.write(time + "\n");
                System.out.println("Finished parallel with size " + size + " in " + time + " ms");
                // System.out.println(arr);
            }

            writer.close();
        }
    }
}
