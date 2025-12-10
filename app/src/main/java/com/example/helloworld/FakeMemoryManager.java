package com.tci.injector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class FakeMemoryManager {
    private static List<MemoryResult> currentResults = new ArrayList<>();

    private static FakeMemoryManager instance;
    private final String[] MEMORY_REGIONS = {"Heap", "Stack", "Code", "Anon", "RWX", "AIB"};
    private final Random rand = new Random();

    public static FakeMemoryManager getInstance() {
        if (instance == null) {
            instance = new FakeMemoryManager();
        }
        return instance;
    }

    public static class MemoryResult {
        public String fakeAddress; 
        public String region;      
        public int valueDec;       
        public String valueHex;    
        public boolean isFrozen = false;

        public MemoryResult(String address, String region, int decValue) {
            this.fakeAddress = address;
            this.region = region;
            this.valueDec = decValue;
            this.valueHex = String.format(Locale.US, "0x%08X", decValue); 
        }

        public void setValue(int newValue) {
            this.valueDec = newValue;
            this.valueHex = String.format(Locale.US, "0x%08X", newValue);
        }
    }

    public List<MemoryResult> getCurrentResults() {
        // GG風: Freezeされていない値をランダムに変動させる
        for (MemoryResult result : currentResults) {
            if (!result.isFrozen && rand.nextDouble() < 0.3) {
                // 30%の確率で±1変動させることで、Refineの必要性をシミュレート
                int change = rand.nextBoolean() ? 1 : -1;
                result.setValue(result.valueDec + change);
            }
        }
        return currentResults;
    }

    public void clearResults() {
        currentResults.clear();
    }

    public void generateNewResults(int searchValue) {
        clearResults();
        
        // GG風: 検索数に幅を持たせる
        int totalEntries = rand.nextInt(500) + 150; 

        for (int i = 0; i < totalEntries; i++) {
            String address = generateFakeAddress();
            String region = MEMORY_REGIONS[rand.nextInt(MEMORY_REGIONS.length)];
            int value;

            // 検索値と完全に一致するものを少なくする
            if (i < totalEntries * 0.02) { 
                value = searchValue;
            } 
            // 検索値の近傍の値を多くする (ファジー検索のシミュレーション)
            else if (i < totalEntries * 0.1) { 
                value = searchValue + rand.nextInt(31) - 15;
            } 
            // ノイズとしてランダムな値を格納
            else {
                value = rand.nextInt(1000000); 
            }

            currentResults.add(new MemoryResult(address, region, value));
        }
    }

    private String generateFakeAddress() {
        // GG風: よりリアルなAndroidの64ビットアドレス空間をシミュレート (0x7...から始まる)
        long randomAddr = (long) (rand.nextDouble() * 0x1FFFFFFFFL) + 0x7000000000L; 
        return String.format(Locale.US, "0x%012X", randomAddr); // 12桁のHexアドレス
    }
}
