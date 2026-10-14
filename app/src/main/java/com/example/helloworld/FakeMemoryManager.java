package com.tci.injector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * In-app synthetic address space used by the GameGuardian-style UI demo.
 * Nothing in this class reads or writes another process.
 */
public class FakeMemoryManager {
    private static FakeMemoryManager instance;

    private final List<MemoryResult> virtualMemory = new ArrayList<>();
    private final List<MemoryResult> currentResults = new ArrayList<>();
    private final List<MemoryResult> savedResults = new ArrayList<>();
    private final Map<String, Integer> previousValues = new HashMap<>();

    private final String[] MEMORY_REGIONS = {"Xa", "A", "Ca", "Cd", "Ch", "Jh", "S"};
    private final String[] DATA_TYPES = {"Dword", "Dword", "Dword", "Float", "Qword", "Word", "Byte"};
    private final Random rand = new Random(0x544954414EL);

    private FakeMemoryManager() {
        resetVirtualMemory();
    }

    public static synchronized FakeMemoryManager getInstance() {
        if (instance == null) instance = new FakeMemoryManager();
        return instance;
    }

    public static class MemoryResult {
        public final String fakeAddress;
        public final String region;
        public final String dataType;
        public int valueDec;
        public String valueHex;
        public boolean isFrozen = false;
        public boolean isSaved = false;

        public MemoryResult(String address, String region, String dataType, int decValue) {
            this.fakeAddress = address;
            this.region = region;
            this.dataType = dataType;
            this.valueDec = decValue;
            this.valueHex = String.format(Locale.US, "0x%08X", decValue);
        }

        public void setValue(int newValue) {
            this.valueDec = newValue;
            this.valueHex = String.format(Locale.US, "0x%08X", newValue);
        }
    }

    public List<MemoryResult> getCurrentResults() {
        return currentResults;
    }

    public List<MemoryResult> getVirtualMemory() {
        return virtualMemory;
    }

    public List<MemoryResult> getSavedResults() {
        return savedResults;
    }

    public void clearResults() {
        currentResults.clear();
        previousValues.clear();
    }

    public void clearSavedResults() {
        for (MemoryResult result : savedResults) result.isSaved = false;
        savedResults.clear();
    }

    public void resetVirtualMemory() {
        virtualMemory.clear();
        currentResults.clear();
        savedResults.clear();
        previousValues.clear();

        // A stable fake 64-bit-ish address space. Values persist across searches.
        for (int i = 0; i < 4096; i++) {
            long address = 0x7000000000L + (i * 0x10L) + (rand.nextInt(4) * 4L);
            String region = MEMORY_REGIONS[rand.nextInt(MEMORY_REGIONS.length)];
            String type = DATA_TYPES[rand.nextInt(DATA_TYPES.length)];
            int value = rand.nextInt(20001) - 5000;
            virtualMemory.add(new MemoryResult(
                    String.format(Locale.US, "0x%012X", address),
                    region,
                    type,
                    value));
        }
    }

    /** Compatibility method used by the Lua sandbox and older screen. */
    public void generateNewResults(int searchValue) {
        searchExact(searchValue);
    }

    public int searchExact(int searchValue) {
        clearResults();
        seedSearchValue(searchValue);
        for (MemoryResult result : virtualMemory) {
            if (result.valueDec == searchValue) currentResults.add(result);
        }
        captureSnapshot();
        return currentResults.size();
    }

    public int searchRange(int min, int max) {
        clearResults();
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        for (MemoryResult result : virtualMemory) {
            if (result.valueDec >= min && result.valueDec <= max) currentResults.add(result);
        }
        captureSnapshot();
        return currentResults.size();
    }

    public int startUnknownSearch() {
        clearResults();
        currentResults.addAll(virtualMemory);
        captureSnapshot();
        return currentResults.size();
    }

    public int refineByChange(String mode, boolean simulateChangeFirst) {
        if (currentResults.isEmpty()) return 0;
        if (simulateChangeFirst) simulateTick();

        List<MemoryResult> keep = new ArrayList<>();
        for (MemoryResult result : currentResults) {
            Integer before = previousValues.get(result.fakeAddress);
            if (before == null) before = result.valueDec;
            int after = result.valueDec;

            boolean matches;
            String normalized = mode == null ? "changed" : mode.toLowerCase(Locale.US);
            if (normalized.contains("unchanged")) {
                matches = after == before;
            } else if (normalized.contains("increased")) {
                matches = after > before;
            } else if (normalized.contains("decreased")) {
                matches = after < before;
            } else {
                matches = after != before;
            }

            if (matches) keep.add(result);
        }

        currentResults.clear();
        currentResults.addAll(keep);
        captureSnapshot();
        return currentResults.size();
    }

    public void simulateTick() {
        for (MemoryResult result : virtualMemory) {
            if (result.isFrozen) continue;
            if (rand.nextDouble() < 0.18) {
                int direction = rand.nextBoolean() ? 1 : -1;
                int magnitude = 1 + rand.nextInt(5);
                result.setValue(result.valueDec + direction * magnitude);
            }
        }
    }

    public void saveResult(MemoryResult result) {
        if (result == null) return;
        result.isSaved = true;
        for (MemoryResult existing : savedResults) {
            if (existing.fakeAddress.equals(result.fakeAddress)) return;
        }
        savedResults.add(result);
    }

    public void removeSavedResult(MemoryResult result) {
        if (result == null) return;
        result.isSaved = false;
        savedResults.remove(result);
    }

    public MemoryResult findByAddress(String address) {
        if (address == null) return null;
        for (MemoryResult result : virtualMemory) {
            if (result.fakeAddress.equalsIgnoreCase(address)) return result;
        }
        return null;
    }

    private void seedSearchValue(int searchValue) {
        // Make the simulator useful for arbitrary demo values while keeping results persistent.
        int[] seedIndexes = {37, 211, 487, 901, 1289, 1741, 2317, 3011, 3559};
        for (int index : seedIndexes) {
            if (index < virtualMemory.size()) {
                MemoryResult result = virtualMemory.get(index);
                if (!result.isFrozen) result.setValue(searchValue);
            }
        }
    }

    private void captureSnapshot() {
        previousValues.clear();
        for (MemoryResult result : currentResults) {
            previousValues.put(result.fakeAddress, result.valueDec);
        }
    }
}
