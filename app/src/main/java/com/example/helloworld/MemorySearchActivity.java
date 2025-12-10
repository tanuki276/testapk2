package com.ghosthacker.app;

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
import android.widget.Toast;

import com.ghosthacker.app.FakeMemoryManager.MemoryResult;

import java.util.ArrayList;
import java.util.List;

public class MemorySearchActivity extends Activity {
    
    private EditText searchValueInput;
    private ListView resultsListView;
    private ArrayAdapter<String> resultsAdapter;
    private FakeMemoryManager memoryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_search);

        memoryManager = FakeMemoryManager.getInstance();
        
        searchValueInput = findViewById(R.id.search_value_input);
        resultsListView = findViewById(R.id.results_list);
        
        Button searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(this::onSearchClicked);
        
        Button refineButton = findViewById(R.id.refine_button);
        refineButton.setOnClickListener(this::onRefineClicked);

        resultsListView.setOnItemClickListener(this::onResultItemClicked);

        displayCurrentResults();
    }
    
    public void onSearchClicked(View view) {
        String input = searchValueInput.getText().toString();
        if (input.isEmpty()) {
            Toast.makeText(this, "ERROR: 検索値が入力されていません。", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int searchValue = Integer.parseInt(input);
            
            Toast.makeText(this, "メモリ領域をスキャン中です...", Toast.LENGTH_LONG).show();

            new Handler().postDelayed(() -> {
                
                memoryManager.generateNewResults(searchValue);
                
                List<MemoryResult> results = memoryManager.getCurrentResults();
                displayCurrentResults();
                
                Toast.makeText(this, 
                               "検索完了: " + results.size() + "個のエントリが見つかりました。", 
                               Toast.LENGTH_SHORT).show();
            }, 2500);
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "ERROR: 無効な数値フォーマットです。", Toast.LENGTH_SHORT).show();
        }
    }
    
    public void onRefineClicked(View view) {
        if (memoryManager.getCurrentResults().isEmpty()) {
             Toast.makeText(this, "WARNING: 検索結果がありません。", Toast.LENGTH_SHORT).show();
             return;
        }
        
        int initialSize = memoryManager.getCurrentResults().size();
        if (initialSize > 10) {
            memoryManager.getCurrentResults().subList(10, initialSize).clear();
            displayCurrentResults();
            
            Toast.makeText(this, 
                           "Refine成功。エントリを " + (initialSize - 10) + "個削除しました。", 
                           Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "WARNING: エントリが少なすぎるため、改善できません。", Toast.LENGTH_SHORT).show();
        }
    }

    private void onResultItemClicked(AdapterView<?> parent, View view, int position, long id) {
        MemoryResult selectedResult = memoryManager.getCurrentResults().get(position);
        
        final EditText input = new EditText(this);
        input.setText(String.valueOf(selectedResult.valueDecimal));
        
        new AlertDialog.Builder(this)
            .setTitle("Edit Value")
            .setMessage("アドレス: " + selectedResult.fakeAddress)
            .setView(input)
            .setPositiveButton("Freeze/OK", (dialog, which) -> {
                try {
                    int newValue = Integer.parseInt(input.getText().toString());
                    
                    selectedResult.setValue(newValue);
                    selectedResult.isFrozen = true;
                    displayCurrentResults();
                    
                    Toast.makeText(this, 
                                   "新しい値に書き換えました。フリーズ状態を維持します。", 
                                   Toast.LENGTH_LONG).show();
                                   
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "ERROR: 無効な値です。", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void displayCurrentResults() {
        List<MemoryResult> results = memoryManager.getCurrentResults();
        
        List<String> displayList = new ArrayList<>();
        for (MemoryResult result : results) {
            String status = result.isFrozen ? "[FROZEN] " : "";
            displayList.add(status + result.fakeAddress + " -> DEC: " + result.valueDecimal + " / HEX: " + result.valueHex);
        }

        resultsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        resultsListView.setAdapter(resultsAdapter);
    }
}
