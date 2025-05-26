package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class abs extends AppCompatActivity {

    private Spinner spinnerGroup, spinnerSite, spinnerClass;
    private TextView textDate;
    private EditText editRemarks;
    private RecyclerView recyclerView;
    private Button btnSave;
    private StudentAdapter adapter;
    private List<Student> studentList = new ArrayList<>();
    private List<Student> filteredStudentList = new ArrayList<>();

    // Nouveaux éléments pour la recherche
    private ImageView searchIcon;
    private TextInputLayout searchLayout;
    private EditText searchEditText;
    private boolean isSearchVisible = false;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            // Not logged in, redirect to login activity
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Signin.class)); // change LoginActivity to your actual login activity
            finish(); // prevent returning here
            return;
        }

        setContentView(R.layout.activity_absence);

        initializeViews();
        setupRecyclerView();
        setupSearchFunctionality();
        loadSpinners();
        loadStudents();
        setupSpinnerListeners();
        setDefaultDate();
        textDate.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveAbsences());
    }

    private MaterialButton btnAll, btnPresent, btnAbsent;

    private void initializeViews() {
        spinnerGroup = findViewById(R.id.spinnerGroup);
        spinnerSite = findViewById(R.id.spinnerSite);
        spinnerClass = findViewById(R.id.spinnerClass);
        textDate = findViewById(R.id.textDate);
        editRemarks = findViewById(R.id.editRemarks);
        recyclerView = findViewById(R.id.recyclerStudents);
        btnSave = findViewById(R.id.btnSave);

        // Nouveaux éléments de recherche
        searchIcon = findViewById(R.id.searchIcon);
        searchLayout = findViewById(R.id.searchLayout);
        searchEditText = findViewById(R.id.searchEditText);

        // Filter buttons
        btnAll = findViewById(R.id.btnAll);
        btnPresent = findViewById(R.id.btnPresent);
        btnAbsent = findViewById(R.id.btnAbsent);

        setupFilterButtons();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    private void setupFilterButtons() {
        View.OnClickListener listener = v -> {
            resetFilterButtons();
            ((MaterialButton) v).setBackgroundTintList(getColorStateList(android.R.color.holo_green_light));
            if (v.getId() == R.id.btnAll) {
                filterByStatus(null);
            } else if (v.getId() == R.id.btnPresent) {
                filterByStatus(true);
            } else if (v.getId() == R.id.btnAbsent) {
                filterByStatus(false);
            }
        };

        btnAll.setOnClickListener(listener);
        btnPresent.setOnClickListener(listener);
        btnAbsent.setOnClickListener(listener);
    }
    private void setDefaultDate() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH); // Janvier = 0
        int day = c.get(Calendar.DAY_OF_MONTH);

        String dateStr = day + "/" + (month + 1) + "/" + year;
        textDate.setText(dateStr);
    }
    private void resetFilterButtons() {
        btnAll.setBackgroundTintList(getColorStateList(android.R.color.white));
        btnPresent.setBackgroundTintList(getColorStateList(android.R.color.white));
        btnAbsent.setBackgroundTintList(getColorStateList(android.R.color.white));
    }

    private void filterByStatus(Boolean status) {
        filteredStudentList.clear();
        if (status == null) {
            // Show all
            filteredStudentList.addAll(studentList);
        } else {
            // Show only matching presence status
            for (Student student : studentList) {
                if (student.isPresent() == status) {
                    filteredStudentList.add(student);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(filteredStudentList);
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchFunctionality() {
        // Cacher initialement la barre de recherche
        searchLayout.setVisibility(View.GONE);

        // Gérer le clic sur l'icône de recherche
        searchIcon.setOnClickListener(v -> toggleSearchVisibility());

        // Ajouter un TextWatcher pour filtrer en temps réel
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void toggleSearchVisibility() {
        if (isSearchVisible) {
            // Cacher la barre de recherche
            searchLayout.setVisibility(View.GONE);
            searchEditText.setText("");
            filterStudents(""); // Réinitialiser le filtre
            isSearchVisible = false;
        } else {
            // Afficher la barre de recherche
            searchLayout.setVisibility(View.VISIBLE);
            searchEditText.requestFocus();
            isSearchVisible = true;
        }
    }

    private void filterStudents(String query) {
        filteredStudentList.clear();

        if (query.isEmpty()) {
            filteredStudentList.addAll(studentList);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (Student student : studentList) {
                if (student.getName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredStudentList.add(student);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void loadSpinners() {
        String[] classes = {"Sélectionner une classe","1AP","2AP","3IIR", "4IIR", "5IIR"};
        String[] groups = {"Sélectionner un groupe","G1","G2","G3","G4", "G5", "G6",};
        String[] sites = {"Sélectionner un site", "centre1", "centre2","maarif"};

        // Create adapters with placeholders
        ArrayAdapter<CharSequence> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, classes);
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClass.setAdapter(classAdapter);

        ArrayAdapter<CharSequence> groupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groups);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGroup.setAdapter(groupAdapter);

        ArrayAdapter<CharSequence> siteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sites);
        siteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSite.setAdapter(siteAdapter);

        // Optional: Set initial selection to placeholder
        spinnerClass.setSelection(0);
        spinnerGroup.setSelection(0);
        spinnerSite.setSelection(0);
    }

    private void loadStudents() {
        String selectedClass = spinnerClass.getSelectedItem().toString();
        String selectedGroup = spinnerGroup.getSelectedItem().toString();
        String selectedSite = spinnerSite.getSelectedItem().toString();

        Log.d("FirestoreQuery", "Selected Class: " + selectedClass);
        Log.d("FirestoreQuery", "Selected Group: " + selectedGroup);
        Log.d("FirestoreQuery", "Selected Site: " + selectedSite);

        if (selectedClass.equals("Sélectionner une classe") ||
                selectedGroup.equals("Sélectionner un groupe") ||
                selectedSite.equals("Sélectionner un site")) {
            Toast.makeText(this, "Veuillez sélectionner tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("students")
                .whereEqualTo("class", selectedClass)
                .whereEqualTo("group", selectedGroup)
                .whereEqualTo("centre", selectedSite)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("FirestoreQuery", "Number of students fetched: " + queryDocumentSnapshots.size());

                    studentList.clear();
                    filteredStudentList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String name = document.getString("name");
                        boolean present = document.getBoolean("present") != null ? document.getBoolean("present") : true;
                        studentList.add(new Student(id, name, present));
                    }

                    filteredStudentList.addAll(studentList);
                    adapter.notifyDataSetChanged();

                    if (studentList.isEmpty()) {
                        Toast.makeText(this, "Aucun étudiant trouvé pour ces critères", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("StudentList", "Total students fetched: " + studentList.size());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur lors du chargement des étudiants: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSpinnerListeners() {
        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadStudents();
                // Réinitialiser la recherche quand on change de classe
                if (isSearchVisible) {
                    searchEditText.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadStudents();
                // Réinitialiser la recherche quand on change de groupe
                if (isSearchVisible) {
                    searchEditText.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, day) -> {
            String dateStr = day + "/" + (month + 1) + "/" + year;
            textDate.setText(dateStr);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    private boolean validateForm() {
        if (textDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (studentList.isEmpty()) {
            Toast.makeText(this, "No students to save", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveAbsences() {
        if (!validateForm()) return;

        String group = spinnerGroup.getSelectedItem().toString();
        String site = spinnerSite.getSelectedItem().toString();
        String selectedClass = spinnerClass.getSelectedItem().toString();
        String date = textDate.getText().toString();
        String remarque = editRemarks.getText().toString();

        // Prepare the students array - utiliser la liste complète, pas la liste filtrée
        List<Map<String, Object>> studentsData = new ArrayList<>();
        for (Student student : studentList) { // Utiliser studentList au lieu de filteredStudentList
            Map<String, Object> studentData = new HashMap<>();
            studentData.put("studentId", student.getId());
            studentData.put("name", student.getName());
            studentData.put("present", student.isPresent());
            studentsData.add(studentData);
        }

        // Create the absence document
        Map<String, Object> absenceData = new HashMap<>();
        absenceData.put("profId", auth.getUid());
        absenceData.put("group", group);
        absenceData.put("site", site);
        absenceData.put("class", selectedClass);
        absenceData.put("date", date);
        absenceData.put("remarque", remarque);
        absenceData.put("students", studentsData);

        db.collection("absences")
                .add(absenceData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Absence saved successfully!", Toast.LENGTH_SHORT).show();
                    // Optional: Clear the form after successful save
                    textDate.setText("");
                    editRemarks.setText("");
                    // Cacher la barre de recherche et réinitialiser
                    if (isSearchVisible) {
                        toggleSearchVisibility();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving absence: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error saving absence", e);
                });
    }

    // ------- Classes internes --------

    public class Student {
        private String id;
        private String name;
        private boolean present;

        public Student() {}

        public Student(String id, String name, boolean present) {
            this.id = id;
            this.name = name;
            this.present = present;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public boolean isPresent() { return present; }
        public void setPresent(boolean present) { this.present = present; }
    }

    public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
        private List<Student> studentList;

        public StudentAdapter(List<Student> list) {
            this.studentList = list;
        }

        public List<Student> getStudents() {
            // Retourner la liste complète des étudiants, pas la liste filtrée
            return abs.this.studentList;
        }

        @NonNull
        @Override
        public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
            return new StudentViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
            Student s = studentList.get(position);
            holder.name.setText(s.getName());

            // Checked = Absent, Unchecked = Present
            holder.present.setChecked(s.isPresent() == false);

            holder.present.setOnCheckedChangeListener((buttonView, isChecked) -> {
                s.setPresent(!isChecked); // If checked, student is absent
            });
        }

        @Override
        public int getItemCount() {
            return studentList.size();
        }

        class StudentViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            CheckBox present;

            public StudentViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.textStudentName);
                present = itemView.findViewById(R.id.checkPresent);
            }
        }
    }
}