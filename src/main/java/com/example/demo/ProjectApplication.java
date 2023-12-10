package com.example.demo;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@SpringBootApplication
public class ProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectApplication.class, args);
    }
}

@Controller
@RequestMapping("/sorting")
class SortingController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/process-single")
    public SortingResponse processSingle(@RequestBody SortingRequest request) {
        long startTime = System.nanoTime();

        int[][] sortedArrays = Arrays.stream(request.getToSort())
                .map(this::sortArray)
                .toArray(int[][]::new);

        long timeTaken = System.nanoTime() - startTime;

        return new SortingResponse(sortedArrays, timeTaken);
    }

    @PostMapping("/process-concurrent")
    public SortingResponse processConcurrent(@RequestBody SortingRequest request) throws InterruptedException, ExecutionException {
        long startTime = System.nanoTime();

        int[][] sortedArrays;
        ExecutorService executorService = Executors.newFixedThreadPool(request.getToSort().length);

        try {
            sortedArrays = executorService.invokeAll(Arrays.stream(request.getToSort())
                    .map(this::getSortingTask)
                    .collect(Collectors.toList()))
                    .stream()
                    .map(this::getSortedArray)
                    .toArray(int[][]::new);
        } finally {
            executorService.shutdown();
        }

        long timeTaken = System.nanoTime() - startTime;

        return new SortingResponse(sortedArrays, timeTaken);
    }

    private int[] sortArray(int[] array) {
        int[] sortedArray = Arrays.copyOf(array, array.length);
        Arrays.sort(sortedArray);
        return sortedArray;
    }

    private Callable<int[]> getSortingTask(int[] array) {
        return () -> sortArray(array);
    }

    private int[] getSortedArray(Future<int[]> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error while sorting array", e);
        }
    }
}

class SortingRequest {
    private int[][] toSort;

    public int[][] getToSort() {
        return toSort;
    }

    public void setToSort(int[][] toSort) {
        this.toSort = toSort;
    }
}

class SortingResponse {
    private int[][] sortedArrays;
    private long timeNs;

    public SortingResponse(int[][] sortedArrays, long timeNs) {
        this.sortedArrays = sortedArrays;
        this.timeNs = timeNs;
    }

    public int[][] getSortedArrays() {
        return sortedArrays;
    }

    public long getTimeNs() {
        return timeNs;
    }
}


