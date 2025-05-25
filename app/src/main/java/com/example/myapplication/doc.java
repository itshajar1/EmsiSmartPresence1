package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

    EditText documentNameInput, documentUrlInput;
    Button saveButton;
    ListView documentListView;
    ArrayAdapter<String> adapter;
    List<String> documentNames = new ArrayList<>();
    List<String> documentUrls = new ArrayList<>();


    // Add this with your existing list declarations
    private List<Timestamp> documentTimestamps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.document);

        documentNameInput = findViewById(R.id.documentNameInput);
        documentUrlInput = findViewById(R.id.documentUrlInput);
        saveButton = findViewById(R.id.saveButton);
        documentListView = findViewById(R.id.documentListView); // Changed from listView to documentListView

        // Initialize adapter BEFORE setting it to the ListView
        adapter = new ArrayAdapter<String>(this, R.layout.item_document_card, R.id.documentName, documentNames) {
            @SuppressLint("SimpleDateFormat")
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView dateView = view.findViewById(R.id.documentDate);
                TextView urlView = view.findViewById(R.id.documentUrl);

                // Set URL text
                urlView.setText(documentUrls.get(position));

                // Format and set date
                if (position < documentTimestamps.size()) {
                    Timestamp timestamp = documentTimestamps.get(position);
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

        // Set click listeners AFTER adapter is initialized
        saveButton.setOnClickListener(v -> saveDocument());
        documentListView.setOnItemClickListener((parent, view, position, id) -> {
            String url = documentUrls.get(position);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        loadDocuments(); // Load documents initially
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
        documentTimestamps.clear(); // Clear timestamps too

        FirebaseFirestore.getInstance().collection("documents")
                .whereEqualTo("teacherId", uid)
                .orderBy("uploadDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query) {
                        documentNames.add(doc.getString("name"));
                        documentUrls.add(doc.getString("url"));
                        documentTimestamps.add(doc.getTimestamp("uploadDate")); // Store timestamp
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Load error: " + e.getMessage()));
    }
}
