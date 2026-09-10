package com.tci.injector;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.tci.injector.FakeMemoryManager.MemoryResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MemorySearchActivity extends Activity {
    private EditText searchValueInput;
    private ListView resultsListView;
    private ArrayAdapter<String> resultsAdapter;
    private FakeMemoryManager memoryManager;
    private Spinner dataTypeSpinner;
    private Spinner refineFilterSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_search);
        memoryManager = FakeMemoryManager.getInstance();
        searchValueInput = findViewById(R.id.search_value_input);
        resultsListView = findViewById(R.id.results_list);
        dataTypeSpinner = findViewById(R.id.data_type_spinner);
        refineFilterSpinner = findViewById(R.id.refine_filter_spinner);

        findViewById(R.id.search_button).setOnClickListener(this::onSearchExecute);
        findViewById(R.id.refine_button).setOnClickListener(this::onRefineExecute);
        findViewById(R.id.freeze_dump_button).setOnClickListener(this::onFreezeDumpClicked);
        resultsListView.setOnItemClickListener(this::onResultItemClicked);
        displayCurrentResults();
    }

    public void onSearchExecute(View view) {
        String input = searchValueInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Enter a value for the sandbox scan", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedType = dataTypeSpinner.getSelectedItem().toString();
        try {
            int searchValue = Integer.parseInt(input.split("~")[0].trim());
            Toast.makeText(this, "Sandbox scan: " + selectedType, Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(() -> {
                memoryManager.generateNewResults(searchValue);
                displayCurrentResults();
                Toast.makeText(this, "Synthetic scan complete: " + memoryManager.getCurrentResults().size() + " results", Toast.LENGTH_SHORT).show();
            }, 700);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid demo value", Toast.LENGTH_SHORT).show();
        }
    }

    public void onRefineExecute(View view) {
        List<MemoryResult> currentResults = memoryManager.getCurrentResults();
        if (currentResults.isEmpty()) {
            Toast.makeText(this, "Run a sandbox scan first", Toast.LENGTH_SHORT).show();
            return;
        }
        String filter = refineFilterSpinner.getSelectedItem().toString();
        int initialSize = currentResults.size();
        Toast.makeText(this, "Refining synthetic results: " + filter, Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(() -> {
            if (initialSize > 5) {
                int itemsToRemove = initialSize / 2;
                Random random = new Random(7);
                for (int i = 0; i < itemsToRemove && !currentResults.isEmpty(); i++) currentResults.remove(random.nextInt(currentResults.size()));
            }
            displayCurrentResults();
        }, 500);
    }

    public void onFreezeDumpClicked(View view) {
        if (memoryManager.getCurrentResults().isEmpty()) {
            Toast.makeText(this, "No synthetic results available", Toast.LENGTH_SHORT).show();
            return;
        }
        for (MemoryResult result : memoryManager.getCurrentResults()) result.isFrozen = true;
        displayCurrentResults();
        Toast.makeText(this, "Synthetic entries marked frozen. No system memory changed.", Toast.LENGTH_LONG).show();
    }

    private void onResultItemClicked(AdapterView<?> parent, View view, int position, long id) {
        MemoryResult selectedResult = memoryManager.getCurrentResults().get(position);
        final EditText input = new EditText(this);
        input.setText(String.valueOf(selectedResult.valueDec));
        new AlertDialog.Builder(this)
            .setTitle("Edit Synthetic Value")
            .setMessage("Address: " + selectedResult.fakeAddress + "\nRegion: " + selectedResult.region + "\nLOCAL SANDBOX ONLY")
            .setView(input)
            .setPositiveButton("APPLY LOCAL", (dialog, which) -> {
                try {
                    selectedResult.setValue(Integer.parseInt(input.getText().toString()));
                    displayCurrentResults();
                    Toast.makeText(this, "Synthetic value updated locally", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid integer", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void displayCurrentResults() {
        List<String> displayList = new ArrayList<>();
        for (MemoryResult result : memoryManager.getCurrentResults()) {
            String status = result.isFrozen ? "[FROZEN] " : "          ";
            displayList.add(status + result.fakeAddress + "  [" + result.region + "]  " + result.valueDec + "  /  " + result.valueHex);
        }
        resultsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        resultsListView.setAdapter(resultsAdapter);
    }
}
