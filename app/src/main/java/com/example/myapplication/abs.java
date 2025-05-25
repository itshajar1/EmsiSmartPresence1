package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

        spinnerGroup = findViewById(R.id.spinnerGroup);
        spinnerSite = findViewById(R.id.spinnerSite);
        spinnerClass = findViewById(R.id.spinnerClass);
        textDate = findViewById(R.id.textDate);
        editRemarks = findViewById(R.id.editRemarks);
        recyclerView = findViewById(R.id.recyclerStudents);
        btnSave = findViewById(R.id.btnSave);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(studentList);
        recyclerView.setAdapter(adapter);

        loadSpinners();
        loadStudents();
        setupSpinnerListeners();

        textDate.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveAbsences());
    }

    private void loadSpinners() {
        String[] groups = {"G5", "Groupe 2"};
        String[] sites = {"Centre1", "Site B"};
        String[] classes = {"4IIR", "Classe B", "Classe C"};

        spinnerGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, groups));
        spinnerSite.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sites));
        spinnerClass.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classes));
    }

    private void loadStudents() {
        String selectedClass = spinnerClass.getSelectedItem().toString();
        String selectedGroup = spinnerGroup.getSelectedItem().toString();

        db.collection("students")
                .whereEqualTo("class", selectedClass)
                .whereEqualTo("group", selectedGroup)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String name = document.getString("name");
                        // Default to present unless specified otherwise
                        boolean present = document.getBoolean("present") != null
                                ? document.getBoolean("present")
                                : true;

                        studentList.add(new Student(id, name, present));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void setupSpinnerListeners() {
        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadStudents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadStudents();
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

// Then in saveAbsences():

    private void saveAbsences() {
        String group = spinnerGroup.getSelectedItem().toString();
        String site = spinnerSite.getSelectedItem().toString();
        String selectedClass = spinnerClass.getSelectedItem().toString();
        String date = textDate.getText().toString();
        String remarque = editRemarks.getText().toString();

        // Prepare the students array
        List<Map<String, Object>> studentsData = new ArrayList<>();
        for (Student student : adapter.getStudents()) {
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving absence: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error saving absence", e);
                });
        if (!validateForm()) return;
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
            return studentList;
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
            holder.present.setChecked(s.isPresent());

            holder.present.setOnCheckedChangeListener((buttonView, isChecked) -> s.setPresent(isChecked));
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
