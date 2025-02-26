
public class Main {

    public static int[] generateRandomArray(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = (int) (Math.random() * 100);
        }
        return arr;
    }
    public static void main(String[] args) {
        testSorts();
        
    }

    public static void printArray(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i]+" ");
        }
    }

    private static void testSorts() {
        int[] arr = generateRandomArray(500000);
        Sorter s = new Sorter();
        System.out.println("Original:");
        //printArray(arr);
        long start;
        long time;
        int[] sorted;
/*
        start = System.currentTimeMillis();
        sorted = s.selectSort(arr.clone());
        time = System.currentTimeMillis() - start;
        System.out.println("\nSelect Sort: ("+time+" ms)");
        //printArray(sorted);

        start = System.currentTimeMillis();
        sorted = s.bubbleSort(arr.clone());
        time = System.currentTimeMillis() - start;
        System.out.println("\nBubble Sort: ("+time+" ms)");
        //printArray(sorted);

        start = System.currentTimeMillis();
        sorted = s.insertSort(arr.clone());
        time = System.currentTimeMillis() - start;
        System.out.println("\nInsert Sort: ("+time+" ms)");
        //printArray(sorted);
*/
        start = System.currentTimeMillis();
        sorted = s.mergeSort(arr.clone());
        time = System.currentTimeMillis() - start;
        System.out.println("\nMerge Sort: ("+time+" ms)");
        //printArray(sorted);

        start = System.currentTimeMillis();
        sorted = s.quickSort(arr.clone());
        time = System.currentTimeMillis() - start;
        System.out.println("\nQuick Sort: ("+time+" ms)");
        //printArray(sorted);
        
        start = System.currentTimeMillis();
        sorted = s.heapSort(arr.clone());
        time = System.currentTimeMillis() - start;
        System.out.println("\nHeap Sort: ("+time+" ms)");
        //printArray(sorted);
        
        start = System.currentTimeMillis();
        sorted = s.countingSort(arr.clone());
        time = System.currentTimeMillis() - start;
        System.out.println("\nCounting Sort: ("+time+" ms)");
        //printArray(sorted);
        
        start = System.currentTimeMillis();
        sorted = s.bucketSort(arr.clone());
        time = System.currentTimeMillis() - start;
        System.out.println("\nBucket Sort: ("+time+" ms)");
        //printArray(sorted);
        
        start = System.currentTimeMillis();
        sorted = s.introSort(arr.clone());
        time = System.currentTimeMillis() - start;
        System.out.println("\nIntro Sort: ("+time+" ms)");
        //printArray(sorted);
    }

    
}