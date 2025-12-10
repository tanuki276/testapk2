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

        Button searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(this::onSearchExecute);

        Button refineButton = findViewById(R.id.refine_button);
        refineButton.setOnClickListener(this::onRefineExecute);
        
        Button freezeDumpButton = findViewById(R.id.freeze_dump_button);
        freezeDumpButton.setOnClickListener(this::onFreezeDumpClicked);


        resultsListView.setOnItemClickListener(this::onResultItemClicked);

        displayCurrentResults();
    }

    public void onSearchExecute(View view) {
        String input = searchValueInput.getText().toString();
        if (input.isEmpty()) {
            Toast.makeText(this, "ERROR: Target value required for scan.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String selectedType = dataTypeSpinner.getSelectedItem().toString();
        
        try {
            int searchValue = Integer.parseInt(input.split("~")[0].trim());

            Toast.makeText(this, 
                "TCI: Initial scan requested. Type: " + selectedType, 
                Toast.LENGTH_LONG).show();

            new Handler().postDelayed(() -> {

                memoryManager.generateNewResults(searchValue);

                List<MemoryResult> results = memoryManager.getCurrentResults();
                displayCurrentResults();

                Toast.makeText(this, 
                               "Scan Complete: " + results.size() + " addresses found.", 
                               Toast.LENGTH_SHORT).show();
            }, 2500);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "ERROR: Invalid value format detected.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onRefineExecute(View view) {
        List<MemoryResult> currentResults = memoryManager.getCurrentResults();
        if (currentResults.isEmpty()) {
             Toast.makeText(this, "WARNING: No addresses indexed. Run 'First Scan'.", Toast.LENGTH_SHORT).show();
             return;
        }

        String selectedFilter = refineFilterSpinner.getSelectedItem().toString();
        int initialSize = currentResults.size();
        
        Toast.makeText(this, 
            "Refining results by filter: " + selectedFilter, 
            Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> {
            if (initialSize > 5 && new Random().nextBoolean()) {
                int itemsToRemove = initialSize / 2 + 1;
                for (int i = 0; i < itemsToRemove && !currentResults.isEmpty(); i++) {
                    currentResults.remove(new Random().nextInt(currentResults.size()));
                }
                displayCurrentResults();

                Toast.makeText(this, 
                               "Refine Successful: " + currentResults.size() + " entries remaining.", 
                               Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "WARNING: Filter produced minimal change.", Toast.LENGTH_SHORT).show();
            }
        }, 1500);
    }
    
    public void onFreezeDumpClicked(View view) {
        if (memoryManager.getCurrentResults().isEmpty()) {
             Toast.makeText(this, "WARNING: No addresses indexed. Cannot execute DUMP.", Toast.LENGTH_SHORT).show();
             return;
        }
        Toast.makeText(this, "EXEC: Initiating memory DUMP/FREEZE sequence on selected entries.", Toast.LENGTH_LONG).show();
    }


    private void onResultItemClicked(AdapterView<?> parent, View view, int position, long id) {
        MemoryResult selectedResult = memoryManager.getCurrentResults().get(position);

        final EditText input = new EditText(this);
        input.setText(String.valueOf(selectedResult.valueDec));

        new AlertDialog.Builder(this)
            .setTitle("Edit Memory Value")
            .setMessage("Address: " + selectedResult.fakeAddress + "\nRegion: " + selectedResult.region)
            .setView(input)
            .setPositiveButton("Set/Freeze", (dialog, which) -> {
                try {
                    int newValue = Integer.parseInt(input.getText().toString());

                    selectedResult.setValue(newValue);
                    selectedResult.isFrozen = true;
                    displayCurrentResults();

                    Toast.makeText(this, 
                                   "INJECT SUCCESS: Address value overwritten and FREEZE applied.", 
                                   Toast.LENGTH_LONG).show();

                } catch (NumberFormatException e) {
                    Toast.makeText(this, "ERROR: Invalid integer value provided.", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void displayCurrentResults() {
        List<MemoryResult> results = memoryManager.getCurrentResults();

        List<String> displayList = new ArrayList<>();
        for (MemoryResult result : results) {
            String status = result.isFrozen ? "[FROZEN] " : "         ";
            displayList.add(status + result.fakeAddress + " [" + result.region + "] -> DEC: " + result.valueDec + " / HEX: " + result.valueHex);
        }

        resultsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        resultsListView.setAdapter(resultsAdapter);
    }
}
