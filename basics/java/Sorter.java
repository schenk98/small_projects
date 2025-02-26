import java.util.ArrayList;
import java.util.Collections;

public class Sorter{
    public int[] selectSort(int[] arr){     
        int max = Integer.MIN_VALUE;  
        int pointerIndex = 0;
        for(int i = arr.length-1; i >= 0; i--){
            for(int j = 0; j < i+1; j++){
                if(arr[j] > max){
                    max = arr[j];
                    pointerIndex = j;
                }
            }
            arr[pointerIndex] = arr[i];
            arr[i] = max;
            max = Integer.MIN_VALUE;
        }
        return arr;
    }
    public int[] bubbleSort(int[] arr){    
        int n = arr.length;
        boolean swapped;
        for (int i = 0; i < n - 1; i++) {
            swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    // Swap arr[j] and arr[j + 1]
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }
            // If no two elements were swapped by inner loop, then break
            if (!swapped) break;
        }
        return arr;
    }
    public int[] insertSort(int[] arr){       
       int n = arr.length;
        for (int i = 1; i < n; ++i) {
            int key = arr[i];
            int j = i - 1;

            // Move elements of arr[0..i-1], that are greater than key,
            // to one position ahead of their current position
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j = j - 1;
            }
            arr[j + 1] = key;
        }
        return arr;
    }
    public int[] mergeSort(int[] arr) {
        if (arr.length <= 1) {
            return arr;
        }
        int mid = arr.length / 2;
        int[] left = new int[mid];
        int[] right = new int[arr.length - mid];

        System.arraycopy(arr, 0, left, 0, mid);
        System.arraycopy(arr, mid, right, 0, arr.length - mid);

        mergeSort(left);
        mergeSort(right);

        merge(arr, left, right);
        return arr;
    }

    private void merge(int[] arr, int[] left, int[] right) {
        int i = 0, j = 0, k = 0;
        while (i < left.length && j < right.length) {
            if (left[i] <= right[j]) {
                arr[k++] = left[i++];
            } else {
                arr[k++] = right[j++];
            }
        }
        while (i < left.length) {
            arr[k++] = left[i++];
        }
        while (j < right.length) {
            arr[k++] = right[j++];
        }
    }

    public int[] quickSort(int[] arr) {
        quickSort(arr, 0, arr.length - 1);
        return arr;
    }

    private void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }

    private int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int i = (low - 1);
        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) {
                i++;
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        int temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        return i + 1;
    }
    public int[] heapSort(int[] arr) {
        int n = arr.length;

        // Build heap (rearrange array)
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i);
        }

        // One by one extract an element from heap
        for (int i = n - 1; i > 0; i--) {
            // Move current root to end
            int temp = arr[0];
            arr[0] = arr[i];
            arr[i] = temp;

            // call max heapify on the reduced heap
            heapify(arr, i, 0);
        }
        return arr;
    }

    void heapify(int[] arr, int n, int i) {
        int largest = i; // Initialize largest as root
        int left = 2 * i + 1; // left = 2*i + 1
        int right = 2 * i + 2; // right = 2*i + 2

        // If left child is larger than root
        if (left < n && arr[left] > arr[largest]) {
            largest = left;
        }

        // If right child is larger than largest so far
        if (right < n && arr[right] > arr[largest]) {
            largest = right;
        }

        // If largest is not root
        if (largest != i) {
            int swap = arr[i];
            arr[i] = arr[largest];
            arr[largest] = swap;

            // Recursively heapify the affected sub-tree
            heapify(arr, n, largest);
        }
    }
    public int[] countingSort(int[] arr) {
        int n = arr.length;

        // Find the maximum element in the array
        int max = arr[0];
        for (int i = 1; i < n; i++) {
            if (arr[i] > max) {
                max = arr[i];
            }
        }

        // Create a count array to store count of individual elements
        int[] count = new int[max + 1];

        // Store count of each element
        for (int i = 0; i < n; i++) {
            count[arr[i]]++;
        }

        // Change count[i] so that count[i] now contains actual
        // position of this element in output array
        for (int i = 1; i <= max; i++) {
            count[i] += count[i - 1];
        }

        // Build the output array
        int[] output = new int[n];
        for (int i = n - 1; i >= 0; i--) {
            output[count[arr[i]] - 1] = arr[i];
            count[arr[i]]--;
        }

        // Copy the output array to arr, so that arr now
        // contains sorted elements
        System.arraycopy(output, 0, arr, 0, n);
        return arr;
    }
    

    public int[] bucketSort(int[] arr) {
        int n = arr.length;
        if (n <= 0) {
            return arr;
        }

        // Find the maximum value in the array
        int max = arr[0];
        for (int i = 1; i < n; i++) {
            if (arr[i] > max) {
                max = arr[i];
            }
        }

        // Create n empty buckets
        ArrayList<Integer>[] buckets = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            buckets[i] = new ArrayList<Integer>();
        }

        // Put array elements in different buckets
        for (int i = 0; i < n; i++) {
            int bucketIndex = (arr[i] * n) / (max + 1);
            buckets[bucketIndex].add(arr[i]);
        }

        // Sort individual buckets
        for (int i = 0; i < n; i++) {
            Collections.sort(buckets[i]);
        }

        // Concatenate all buckets into arr[]
        int index = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < buckets[i].size(); j++) {
                arr[index++] = buckets[i].get(j);
            }
        }
        return arr;
    }
    public int[] introSort(int[] arr) {
        int n = arr.length;
        int maxDepth = (int) (2 * Math.log(n) / Math.log(2));
        introSort(arr, 0, n - 1, maxDepth);
        return arr;
    }

    private void introSort(int[] arr, int low, int high, int maxDepth) {
        if (high - low <= 16) {
            insertionSort(arr, low, high);
        } else if (maxDepth == 0) {
            heapSort(arr, low, high);
        } else {
            int pivot = partition(arr, low, high);
            introSort(arr, low, pivot - 1, maxDepth - 1);
            introSort(arr, pivot + 1, high, maxDepth - 1);
        }
    }

    private void insertionSort(int[] arr, int low, int high) {
        for (int i = low + 1; i <= high; i++) {
            int key = arr[i];
            int j = i - 1;
            while (j >= low && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }

    private void heapSort(int[] arr, int low, int high) {
        int n = high - low + 1;
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i, low);
        }
        for (int i = n - 1; i > 0; i--) {
            int temp = arr[low];
            arr[low] = arr[low + i];
            arr[low + i] = temp;
            heapify(arr, i, 0, low);
        }
    }

    private void heapify(int[] arr, int n, int i, int low) {
        int largest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;
        if (left < n && arr[low + left] > arr[low + largest]) {
            largest = left;
        }
        if (right < n && arr[low + right] > arr[low + largest]) {
            largest = right;
        }
        if (largest != i) {
            int swap = arr[low + i];
            arr[low + i] = arr[low + largest];
            arr[low + largest] = swap;
            heapify(arr, n, largest, low);
        }
    }
}

