package com.tci.injector;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.tci.injector.FakeMemoryManager.MemoryResult;

import java.util.ArrayList;
import java.util.List;

public class MemorySearchActivity extends Activity {
    private EditText searchValueInput;
    private ListView resultsListView;
    private ArrayAdapter<String> resultsAdapter;
    private FakeMemoryManager memoryManager;
    private Spinner dataTypeSpinner;
    private Spinner searchTypeSpinner;
    private Spinner refineFilterSpinner;
    private TextView resultCount;
    private TextView title;
    private TextView subtitle;
    private boolean showSaved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_search);

        memoryManager = FakeMemoryManager.getInstance();
        searchValueInput = findViewById(R.id.search_value_input);
        resultsListView = findViewById(R.id.results_list);
        dataTypeSpinner = findViewById(R.id.data_type_spinner);
        searchTypeSpinner = findViewById(R.id.search_type_spinner);
        refineFilterSpinner = findViewById(R.id.refine_filter_spinner);
        resultCount = findViewById(R.id.result_count);
        title = findViewById(R.id.memory_title);
        subtitle = findViewById(R.id.memory_subtitle);
        showSaved = getIntent().getBooleanExtra("SHOW_SAVED", false);

        findViewById(R.id.search_button).setOnClickListener(this::onSearchExecute);
        findViewById(R.id.refine_button).setOnClickListener(this::onRefineExecute);
        findViewById(R.id.freeze_dump_button).setOnClickListener(this::onFreezeDumpClicked);
        resultsListView.setOnItemClickListener(this::onResultItemClicked);
        resultsListView.setOnItemLongClickListener(this::onResultItemLongClicked);

        if (showSaved) {
            title.setText("SAVED LIST");
            subtitle.setText("Long-press results in Search to add them here · synthetic values only");
            searchValueInput.setVisibility(View.GONE);
            dataTypeSpinner.setVisibility(View.GONE);
            searchTypeSpinner.setVisibility(View.GONE);
            findViewById(R.id.search_button).setVisibility(View.GONE);
            refineFilterSpinner.setVisibility(View.GONE);
            findViewById(R.id.refine_button).setVisibility(View.GONE);
        }

        displayCurrentResults();
    }

    public void onSearchExecute(View view) {
        String mode = searchTypeSpinner.getSelectedItem().toString();
        String input = searchValueInput.getText().toString().trim();
        String type = dataTypeSpinner.getSelectedItem().toString();

        try {
            int count;
            if (mode.startsWith("Unknown")) {
                count = memoryManager.startUnknownSearch();
                Toast.makeText(this, "Unknown-value snapshot created", Toast.LENGTH_SHORT).show();
            } else if (mode.startsWith("Range")) {
                String[] parts = input.split("~");
                if (parts.length != 2) {
                    Toast.makeText(this, "Enter a range like 100~200", Toast.LENGTH_SHORT).show();
                    return;
                }
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                count = memoryManager.searchRange(min, max);
                Toast.makeText(this, "Range search · " + type, Toast.LENGTH_SHORT).show();
            } else {
                if (input.isEmpty()) {
                    Toast.makeText(this, "Enter a value", Toast.LENGTH_SHORT).show();
                    return;
                }
                int value = Integer.parseInt(input);
                count = memoryManager.searchExact(value);
                Toast.makeText(this, "Exact search · " + type, Toast.LENGTH_SHORT).show();
            }

            displayCurrentResults();
            Toast.makeText(this, count + " synthetic results", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid numeric value", Toast.LENGTH_SHORT).show();
        }
    }

    public void onRefineExecute(View view) {
        if (memoryManager.getCurrentResults().isEmpty()) {
            Toast.makeText(this, "Run a search first", Toast.LENGTH_SHORT).show();
            return;
        }

        String filter = refineFilterSpinner.getSelectedItem().toString();
        int count = memoryManager.refineByChange(filter, true);
        displayCurrentResults();
        Toast.makeText(this, filter + " → " + count + " results", Toast.LENGTH_SHORT).show();
    }

    public void onFreezeDumpClicked(View view) {
        List<MemoryResult> source = getDisplayedSource();
        if (source.isEmpty()) {
            Toast.makeText(this, "No synthetic results available", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean freeze = false;
        for (MemoryResult item : source) {
            if (!item.isFrozen) {
                freeze = true;
                break;
            }
        }

        for (MemoryResult item : source) item.isFrozen = freeze;
        displayCurrentResults();
        Toast.makeText(this, freeze ? "Results frozen in simulator" : "Results unfrozen", Toast.LENGTH_SHORT).show();
    }

    private void onResultItemClicked(AdapterView<?> parent, View view, int position, long id) {
        List<MemoryResult> source = getDisplayedSource();
        if (position < 0 || position >= source.size()) return;
        MemoryResult selected = source.get(position);

        final EditText input = new EditText(this);
        input.setText(String.valueOf(selected.valueDec));

        new AlertDialog.Builder(this)
                .setTitle(selected.fakeAddress)
                .setMessage(selected.dataType + " · " + selected.region + "\nSynthetic memory only")
                .setView(input)
                .setPositiveButton("SET VALUE", (dialog, which) -> {
                    try {
                        selected.setValue(Integer.parseInt(input.getText().toString().trim()));
                        displayCurrentResults();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid integer", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton(selected.isFrozen ? "UNFREEZE" : "FREEZE", (dialog, which) -> {
                    selected.isFrozen = !selected.isFrozen;
                    displayCurrentResults();
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private boolean onResultItemLongClicked(AdapterView<?> parent, View view, int position, long id) {
        List<MemoryResult> source = getDisplayedSource();
        if (position < 0 || position >= source.size()) return true;
        MemoryResult selected = source.get(position);

        if (showSaved) {
            memoryManager.removeSavedResult(selected);
            Toast.makeText(this, "Removed from saved list", Toast.LENGTH_SHORT).show();
        } else {
            memoryManager.saveResult(selected);
            Toast.makeText(this, "Saved " + selected.fakeAddress, Toast.LENGTH_SHORT).show();
        }
        displayCurrentResults();
        return true;
    }

    private List<MemoryResult> getDisplayedSource() {
        return showSaved ? memoryManager.getSavedResults() : memoryManager.getCurrentResults();
    }

    private void displayCurrentResults() {
        List<MemoryResult> source = getDisplayedSource();
        List<String> displayList = new ArrayList<>();

        for (MemoryResult result : source) {
            String markers = "";
            if (result.isSaved) markers += "★";
            if (result.isFrozen) markers += "❄";
            if (!markers.isEmpty()) markers += "  ";

            displayList.add(markers
                    + result.fakeAddress
                    + "  " + result.valueDec
                    + "  [" + result.dataType + "]"
                    + "  " + result.region);
        }

        resultsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        resultsListView.setAdapter(resultsAdapter);
        resultCount.setText(String.valueOf(source.size()));
    }
}
