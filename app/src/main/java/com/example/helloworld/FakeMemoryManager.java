package com.ghosthacker.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class FakeMemoryManager {
    private static List<MemoryResult> currentResults = new ArrayList<>();
    
    private static FakeMemoryManager instance;

    public static FakeMemoryManager getInstance() {
        if (instance == null) {
            instance = new FakeMemoryManager();
        }
        return instance;
    }

    public static class MemoryResult {
        public String fakeAddress; 
        public int valueDecimal;   
        public String valueHex;    
        public boolean isFrozen = false;

        public MemoryResult(String address, int decValue) {
            this.fakeAddress = address;
            this.valueDecimal = decValue;
            this.valueHex = String.format(Locale.US, "0x%08X", decValue); 
        }

        public void setValue(int newValue) {
            this.valueDecimal = newValue;
            this.valueHex = String.format(Locale.US, "0x%08X", newValue);
        }
    }

    public List<MemoryResult> getCurrentResults() {
        return currentResults;
    }

    public void clearResults() {
        currentResults.clear();
    }

    public void generateNewResults(int searchValue) {
        clearResults();
        Random rand = new Random();
        int totalEntries = rand.nextInt(400) + 100;

        for (int i = 0; i < totalEntries; i++) {
            String address = generateFakeAddress(rand);
            int value;
            
            if (i < totalEntries * 0.05) {
                value = searchValue;
            } else if (i < totalEntries * 0.15) {
                value = searchValue + rand.nextInt(21) - 10;
            } else {
                value = rand.nextInt(1000000); 
            }
            
            currentResults.add(new MemoryResult(address, value));
        }
    }
    
    private String generateFakeAddress(Random rand) {
        long randomAddr = (long) (rand.nextDouble() * 0x1FFFFFFFFL) + 0x700000000L; 
        return String.format(Locale.US, "0x%010X (RWX)", randomAddr); 
    }
}
