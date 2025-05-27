package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class time extends AppCompatActivity {

    private TableLayout tableLayout;
    private ProgressBar indicateurChargement;
    private TextView texteEtatVide;
    private FloatingActionButton fabActualiser;
    private FirebaseFirestore db;

    // Créneaux horaires fixes - mais nous afficherons les cours même si les heures ne correspondent pas exactement
    private String[][] creneauxHoraires = {
            {"08:30", "10:00", "Séance Matinale 1"},
            {"10:15", "11:45", "Séance Matinale 2"},
            {"14:30", "16:00", "Séance Après-midi 1"},
            {"16:15", "17:45", "Séance Après-midi 2"}
    };

    private String[] jours = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi"};
    private String[] couleursJours = {"#E3F2FD", "#F3E5F5", "#E8F5E8", "#FFF3E0", "#FCE4EC"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.time);

        initialiserVues();
        configurerBarreAction();
        chargerEmploiDuTemps();
    }

    private void initialiserVues() {
        tableLayout = findViewById(R.id.tableLayoutSchedule);
        indicateurChargement = findViewById(R.id.loadingIndicator);
        texteEtatVide = findViewById(R.id.emptyStateText);
        fabActualiser = findViewById(R.id.fabRefresh);

        // Configurer le bouton actualiser
        fabActualiser.setOnClickListener(v -> {
            Toast.makeText(this, "Actualisation de l'emploi du temps...", Toast.LENGTH_SHORT).show();
            chargerEmploiDuTemps();
        });

        // Afficher l'état de chargement
        afficherChargement(true);
    }

    private void configurerBarreAction() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Emploi du Temps Hebdomadaire");
            getSupportActionBar().setSubtitle(obtenirPlageeSemaineCourante());
        }
    }

    private String obtenirPlageeSemaineCourante() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.FRENCH);
        String debut = sdf.format(cal.getTime());

        cal.add(Calendar.DAY_OF_WEEK, 4);
        String fin = sdf.format(cal.getTime());

        return debut + " - " + fin;
    }

    private void afficherChargement(boolean afficher) {
        indicateurChargement.setVisibility(afficher ? View.VISIBLE : View.GONE);
        tableLayout.setVisibility(afficher ? View.GONE : View.VISIBLE);
        texteEtatVide.setVisibility(View.GONE);
    }

    private void afficherEtatVide() {
        indicateurChargement.setVisibility(View.GONE);
        tableLayout.setVisibility(View.GONE);
        texteEtatVide.setVisibility(View.VISIBLE);
        texteEtatVide.setText("Aucune donnée d'emploi du temps disponible.\nVeuillez réessayer plus tard.");
    }

    private void chargerEmploiDuTemps() {
        db = FirebaseFirestore.getInstance();

        db.collection("schedule")
                .get()
                .addOnCompleteListener(task -> {
                    afficherChargement(false);

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        traiterDonneesEmploiDuTemps(task.getResult());
                    } else {
                        afficherEtatVide();
                        if (!task.isSuccessful()) {
                            Toast.makeText(this, "Échec du chargement de l'emploi du temps", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    afficherChargement(false);
                    afficherEtatVide();
                    Toast.makeText(this, "Erreur de réseau. Veuillez réessayer.", Toast.LENGTH_SHORT).show();
                });
    }

    private void traiterDonneesEmploiDuTemps(com.google.firebase.firestore.QuerySnapshot result) {
        Map<String, Map<Integer, java.util.List<ElementEmploiDuTemps>>> carteEmploiDuTemps = new HashMap<>();

        for (QueryDocumentSnapshot document : result) {
            String jour = document.getString("day");
            String heureDebut = document.getString("startTime");
            String heureFin = document.getString("endTime");
            String groupe = document.getString("group");
            String salle = document.getString("room");
            String matiere = document.getString("subject");
            String nomClasse = document.getString("class");

            if (jour == null || heureDebut == null || heureFin == null || matiere == null) continue;

            int indexCreneau = obtenirCreneauHorairePourHeure(heureDebut);
            if (indexCreneau == -1) continue;

            ElementEmploiDuTemps element = new ElementEmploiDuTemps(heureDebut, heureFin, groupe, nomClasse, salle, matiere);

            carteEmploiDuTemps.putIfAbsent(jour, new HashMap<>());
            carteEmploiDuTemps.get(jour).putIfAbsent(indexCreneau, new java.util.ArrayList<>());
            carteEmploiDuTemps.get(jour).get(indexCreneau).add(element);
        }

        construireTableauEmploiDuTemps(carteEmploiDuTemps);
    }

    private void construireTableauEmploiDuTemps(Map<String, Map<Integer, java.util.List<ElementEmploiDuTemps>>> carteEmploiDuTemps) {
        tableLayout.removeAllViews();

        // Ajouter le titre principal
        ajouterTitrePrincipal();

        // Ajouter les en-têtes des jours
        ajouterEntetesJours();

        // Ajouter les lignes de créneaux horaires avec une hauteur plus grande
        for (int i = 0; i < creneauxHoraires.length; i++) {
            ajouterLigneCreneauHoraire(i, carteEmploiDuTemps);
        }
    }

    private void ajouterTitrePrincipal() {
        TableRow ligneTitre = new TableRow(this);
        ligneTitre.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));

        TextView enteteTitre = new TextView(this);
        enteteTitre.setText("📅 EMPLOI DU TEMPS HEBDOMADAIRE");
        enteteTitre.setTextSize(24);
        enteteTitre.setTypeface(null, Typeface.BOLD);
        enteteTitre.setTextColor(Color.parseColor("#2E7D32"));
        enteteTitre.setGravity(Gravity.CENTER);
        enteteTitre.setPadding(20, 30, 20, 30);
        enteteTitre.setBackgroundColor(Color.parseColor("#E8F5E8"));

        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.span = 6; // S'étendre sur toutes les colonnes
        enteteTitre.setLayoutParams(params);

        ligneTitre.addView(enteteTitre);
        tableLayout.addView(ligneTitre);

        // Ajouter le sous-titre avec la plage de la semaine
        TableRow ligneSousTitre = new TableRow(this);
        TextView enteteSousTitre = new TextView(this);
        enteteSousTitre.setText("Semaine du " + obtenirPlageeSemaineCourante());
        enteteSousTitre.setTextSize(16);
        enteteSousTitre.setTypeface(null, Typeface.NORMAL);
        enteteSousTitre.setTextColor(Color.parseColor("#1B5E20"));
        enteteSousTitre.setGravity(Gravity.CENTER);
        enteteSousTitre.setPadding(20, 15, 20, 25);
        enteteSousTitre.setBackgroundColor(Color.parseColor("#F1F8E9"));

        TableRow.LayoutParams paramsSousTitre = new TableRow.LayoutParams();
        paramsSousTitre.span = 6;
        enteteSousTitre.setLayoutParams(paramsSousTitre);

        ligneSousTitre.addView(enteteSousTitre);
        tableLayout.addView(ligneSousTitre);
    }

    private void ajouterEntetesJours() {
        TableRow ligneEntete = new TableRow(this);

        // En-tête de la colonne heure
        TextView enteteHeure = creerCelluleEntete("Heure", "#1B5E20");
        ligneEntete.addView(enteteHeure);

        // En-têtes des jours avec différentes couleurs
        for (int i = 0; i < jours.length; i++) {
            TextView enteteJour = creerCelluleEntete(jours[i], "#2E7D32");
            enteteJour.setBackgroundColor(Color.parseColor(couleursJours[i]));
            ligneEntete.addView(enteteJour);
        }

        tableLayout.addView(ligneEntete);
    }

    private TextView creerCelluleEntete(String texte, String couleurTexte) {
        TextView cellule = new TextView(this);
        cellule.setText(texte);
        cellule.setTextSize(14);
        cellule.setTypeface(null, Typeface.BOLD);
        cellule.setTextColor(Color.parseColor(couleurTexte));
        cellule.setGravity(Gravity.CENTER);
        cellule.setPadding(12, 20, 12, 20);
        cellule.setBackgroundColor(Color.parseColor("#F1F8E9"));

        // Ajouter une bordure subtile
        cellule.setBackgroundResource(android.R.drawable.editbox_background);

        return cellule;
    }

    private void ajouterLigneCreneauHoraire(int indexCreneau, Map<String, Map<Integer, java.util.List<ElementEmploiDuTemps>>> carteEmploiDuTemps) {
        TableRow ligne = new TableRow(this);
        ligne.setPadding(0, 12, 0, 12);

        // Cellule de créneau horaire - plus grande
        TextView celluleHeure = creerCelluleHeure(indexCreneau);
        ligne.addView(celluleHeure);

        // Cellules d'emploi du temps pour chaque jour - plus grandes
        for (int indexJour = 0; indexJour < jours.length; indexJour++) {
            String jour = jours[indexJour];
            TextView cellule = creerCelluleEmploiDuTemps(carteEmploiDuTemps, jour, indexCreneau, indexJour);
            ligne.addView(cellule);
        }

        tableLayout.addView(ligne);
    }

    private TextView creerCelluleHeure(int indexCreneau) {
        TextView celluleHeure = new TextView(this);
        // Afficher à la fois le créneau horaire fixe et le nom descriptif
        String plageHoraire = creneauxHoraires[indexCreneau][0] + " - " + creneauxHoraires[indexCreneau][1] + "\n" + creneauxHoraires[indexCreneau][2];
        celluleHeure.setText(plageHoraire);
        celluleHeure.setTextSize(14);
        celluleHeure.setTypeface(null, Typeface.BOLD);
        celluleHeure.setTextColor(Color.parseColor("#1B5E20"));
        celluleHeure.setGravity(Gravity.CENTER);
        celluleHeure.setPadding(20, 80, 20, 80); // Padding beaucoup plus grand
        celluleHeure.setBackgroundColor(Color.parseColor("#F1F8E9"));
        celluleHeure.setMinWidth(160);
        celluleHeure.setMinHeight(300); // Beaucoup plus haut

        return celluleHeure;
    }

    private TextView creerCelluleEmploiDuTemps(Map<String, Map<Integer, java.util.List<ElementEmploiDuTemps>>> carteEmploiDuTemps,
                                               String jour, int indexCreneau, int indexJour) {
        TextView cellule = new TextView(this);
        cellule.setPadding(16, 40, 16, 40); // Padding plus grand
        cellule.setMinHeight(300); // Cellules beaucoup plus hautes
        cellule.setMinWidth(280); // Cellules plus larges
        cellule.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);

        if (carteEmploiDuTemps.containsKey(jour) && carteEmploiDuTemps.get(jour).containsKey(indexCreneau)) {
            java.util.List<ElementEmploiDuTemps> elements = carteEmploiDuTemps.get(jour).get(indexCreneau);
            cellule.setText(formaterElementsEmploiDuTemps(elements));
            cellule.setTextSize(12);
            cellule.setTextColor(Color.parseColor("#1B5E20"));
            cellule.setBackgroundColor(Color.parseColor(couleursJours[indexJour]));
            cellule.setBackgroundResource(R.drawable.schedule_cell_filled);
        } else {
            cellule.setText("📅\n\nTemps Libre");
            cellule.setTextSize(14);
            cellule.setTextColor(Color.parseColor("#4CAF50"));
            cellule.setGravity(Gravity.CENTER);
            cellule.setBackgroundColor(Color.parseColor("#F9FBE7"));
            cellule.setBackgroundResource(R.drawable.schedule_cell_empty);
        }

        return cellule;
    }

    private String formaterElementsEmploiDuTemps(java.util.List<ElementEmploiDuTemps> elements) {
        StringBuilder formate = new StringBuilder();
        for (int i = 0; i < elements.size(); i++) {
            ElementEmploiDuTemps element = elements.get(i);
            formate.append("⏰ ").append(element.heureDebut).append(" - ").append(element.heureFin).append("\n");
            formate.append("📚 ").append(element.matiere).append("\n");
            formate.append("👥 ").append(element.groupe != null ? element.groupe : "N/A").append("\n");
            if (element.nomClasse != null && !element.nomClasse.isEmpty()) {
                formate.append("🎓 ").append(element.nomClasse).append("\n");
            }
            formate.append("🏢 ").append(element.salle != null ? element.salle : "N/A");

            if (i < elements.size() - 1) {
                formate.append("\n\n───────────\n\n"); // Séparateur pour plusieurs cours
            }
        }
        return formate.toString();
    }

    // Méthode d'aide pour trouver le créneau horaire le plus proche pour une heure donnée
    private int obtenirCreneauHorairePourHeure(String heureDebut) {
        try {
            // D'abord essayer une correspondance exacte
            for (int i = 0; i < creneauxHoraires.length; i++) {
                if (creneauxHoraires[i][0].equals(heureDebut)) {
                    return i;
                }
            }

            // Si aucune correspondance exacte, trouver le créneau le plus proche par proximité temporelle
            String[] parties = heureDebut.split(":");
            int heure = Integer.parseInt(parties[0]);
            int minute = Integer.parseInt(parties[1]);
            int totalMinutes = heure * 60 + minute;

            int creneauLePlusProche = -1;
            int plusPetiteDifference = Integer.MAX_VALUE;

            for (int i = 0; i < creneauxHoraires.length; i++) {
                String[] partiesCreneau = creneauxHoraires[i][0].split(":");
                int heureCreneau = Integer.parseInt(partiesCreneau[0]);
                int minuteCreneau = Integer.parseInt(partiesCreneau[1]);
                int totalMinutesCreneau = heureCreneau * 60 + minuteCreneau;

                int difference = Math.abs(totalMinutes - totalMinutesCreneau);
                if (difference < plusPetiteDifference) {
                    plusPetiteDifference = difference;
                    creneauLePlusProche = i;
                }
            }

            // N'assigner au créneau le plus proche que si dans les 2 heures (120 minutes)
            return plusPetiteDifference <= 120 ? creneauLePlusProche : -1;

        } catch (Exception e) {
            return -1;
        }
    }

    private int obtenirIndexCreneau(String heureDebut) {
        return obtenirCreneauHorairePourHeure(heureDebut);
    }

    // Classe d'aide pour une meilleure organisation des données
    private static class ElementEmploiDuTemps {
        String heureDebut, heureFin, groupe, nomClasse, salle, matiere;

        ElementEmploiDuTemps(String heureDebut, String heureFin, String groupe, String nomClasse, String salle, String matiere) {
            this.heureDebut = heureDebut;
            this.heureFin = heureFin;
            this.groupe = groupe;
            this.nomClasse = nomClasse;
            this.salle = salle;
            this.matiere = matiere;
        }
    }
}