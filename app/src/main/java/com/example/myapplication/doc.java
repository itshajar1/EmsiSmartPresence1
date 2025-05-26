package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class doc extends AppCompatActivity {

    EditText documentNameInput, documentUrlInput, searchInput;
    Button saveButton, addDocumentButton;
    ListView documentListView;
    ArrayAdapter<String> adapter;
    LinearLayout addDocumentSection;

    // Original lists (contain all documents)
    List<String> documentNames = new ArrayList<>();
    List<String> documentUrls = new ArrayList<>();
    List<Timestamp> documentTimestamps = new ArrayList<>();

    // Filtered lists (contain search results)
    List<String> filteredDocumentNames = new ArrayList<>();
    List<String> filteredDocumentUrls = new ArrayList<>();
    List<Timestamp> filteredDocumentTimestamps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.document);

        documentNameInput = findViewById(R.id.documentNameInput);
        documentUrlInput = findViewById(R.id.documentUrlInput);
        searchInput = findViewById(R.id.searchInput);
        saveButton = findViewById(R.id.saveButton);
        addDocumentButton = findViewById(R.id.addDocumentButton);
        documentListView = findViewById(R.id.documentListView);
        addDocumentSection = findViewById(R.id.addDocumentSection);

        // Initially hide the add document section
        addDocumentSection.setVisibility(View.GONE);

        // Initialize adapter with filtered lists
        adapter = new ArrayAdapter<String>(this, R.layout.item_document_card, R.id.documentName, filteredDocumentNames) {
            @SuppressLint("SimpleDateFormat")
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView dateView = view.findViewById(R.id.documentDate);
                TextView urlView = view.findViewById(R.id.documentUrl);

                // Set URL text from filtered list
                urlView.setText(filteredDocumentUrls.get(position));

                // Format and set date from filtered list
                if (position < filteredDocumentTimestamps.size()) {
                    Timestamp timestamp = filteredDocumentTimestamps.get(position);
                    if (timestamp != null) {
                        String formattedDate = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                                .format(timestamp.toDate());
                        dateView.setText(formattedDate);
                    }
                }

                return view;
            }
        };

        documentListView.setAdapter(adapter);

        // Set click listeners
        saveButton.setOnClickListener(v -> saveDocument());
        addDocumentButton.setOnClickListener(v -> toggleAddDocumentSection());
        documentListView.setOnItemClickListener((parent, view, position, id) -> {
            String url = filteredDocumentUrls.get(position);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        // Add search functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDocuments(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadDocuments(); // Load documents initially
    }

    private void toggleAddDocumentSection() {
        if (addDocumentSection.getVisibility() == View.GONE) {
            addDocumentSection.setVisibility(View.VISIBLE);
            addDocumentButton.setText("Cancel");
        } else {
            addDocumentSection.setVisibility(View.GONE);
            addDocumentButton.setText("Add Document");
            // Clear inputs when hiding
            documentNameInput.setText("");
            documentUrlInput.setText("");
        }
    }

    private void filterDocuments(String searchText) {
        filteredDocumentNames.clear();
        filteredDocumentUrls.clear();
        filteredDocumentTimestamps.clear();

        if (searchText.isEmpty()) {
            // If search is empty, show all documents
            filteredDocumentNames.addAll(documentNames);
            filteredDocumentUrls.addAll(documentUrls);
            filteredDocumentTimestamps.addAll(documentTimestamps);
        } else {
            // Filter documents by name (case-insensitive)
            String searchLower = searchText.toLowerCase();
            for (int i = 0; i < documentNames.size(); i++) {
                if (documentNames.get(i).toLowerCase().contains(searchLower)) {
                    filteredDocumentNames.add(documentNames.get(i));
                    filteredDocumentUrls.add(documentUrls.get(i));
                    if (i < documentTimestamps.size()) {
                        filteredDocumentTimestamps.add(documentTimestamps.get(i));
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void saveDocument() {
        String name = documentNameInput.getText().toString().trim();
        String url = documentUrlInput.getText().toString().trim();
        String uid = FirebaseAuth.getInstance().getUid();

        if (name.isEmpty() || url.isEmpty()) {
            Toast.makeText(this, "Please enter both name and URL", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("name", name);
        doc.put("url", url);
        doc.put("teacherId", uid);
        doc.put("uploadDate", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("documents")
                .add(doc)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, "Document saved", Toast.LENGTH_SHORT).show();
                    documentNameInput.setText("");
                    documentUrlInput.setText("");
                    // Hide the add document section after saving
                    addDocumentSection.setVisibility(View.GONE);
                    addDocumentButton.setText("Add Document");
                    loadDocuments(); // reload list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadDocuments() {
        String uid = FirebaseAuth.getInstance().getUid();
        documentNames.clear();
        documentUrls.clear();
        documentTimestamps.clear();

        FirebaseFirestore.getInstance().collection("documents")
                .whereEqualTo("teacherId", uid)
                .orderBy("uploadDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query) {
                        documentNames.add(doc.getString("name"));
                        documentUrls.add(doc.getString("url"));
                        documentTimestamps.add(doc.getTimestamp("uploadDate"));
                    }
                    // After loading, apply current search filter
                    filterDocuments(searchInput.getText().toString());
                })
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Load error: " + e.getMessage()));
    }
}