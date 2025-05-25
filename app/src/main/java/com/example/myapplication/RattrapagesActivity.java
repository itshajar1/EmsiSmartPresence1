package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RattrapagesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RattrapageAdapter adapter;
    private List<Rattrapage> rattrapageList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rattrapages);

        recyclerView = findViewById(R.id.recyclerRattrapages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RattrapageAdapter(rattrapageList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadRattrapages();
    }

    private void loadRattrapages() {
        String profId = auth.getUid();
        if (profId == null) {
            Toast.makeText(this, "Non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("rattrapages")
                .whereEqualTo("profId", profId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    rattrapageList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Rattrapage r = doc.toObject(Rattrapage.class);
                        rattrapageList.add(r);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur chargement", Toast.LENGTH_SHORT).show());
    }

    // Classe interne modèle
    public static class Rattrapage {
        private String matiere;
        private String groupe;
        private String classe;
        private String date;
        private String heure;

        public Rattrapage() {} // requis Firestore

        public String getMatiere() { return matiere; }
        public String getGroupe() { return groupe; }
        public String getClasse() { return classe; }
        public String getDate() { return date; }
        public String getHeure() { return heure; }
    }

    // Adapter interne RecyclerView
    public class RattrapageAdapter extends RecyclerView.Adapter<RattrapageAdapter.ViewHolder> {
        private List<Rattrapage> list;

        public RattrapageAdapter(List<Rattrapage> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rattrapage, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Rattrapage r = list.get(position);
            holder.textDateHeure.setText(r.getDate() + " à " + r.getHeure());
            holder.textMatiere.setText(r.getMatiere());
            holder.textClasseGroupe.setText(r.getClasse() + " - " + r.getGroupe());
        }


        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView textDateHeure, textMatiere, textClasseGroupe;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textDateHeure = itemView.findViewById(R.id.textDateHeure);
                textMatiere = itemView.findViewById(R.id.textMatiere);
                textClasseGroupe = itemView.findViewById(R.id.textClasseGroupe);
            }
        }

    }
}
