package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Home extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int CAMERA_PERMISSION_CODE = 100;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private LinearLayout notificationsPopup;
    private LinearLayout notificationsContainer;
    private ImageView notificationIcon;
    private ImageView profileImage;
    private ImageButton addPhotoButton;
    private boolean notificationsVisible = false;
    private Uri imageUri;

    private List<NotificationItem> notifications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        // Initialize Firebase services
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Initialize views
        initializeViews();

        // Set up user info
        setupUserInfo();

        // Set up click listeners
        setupClickListeners();

        // Profile image handling
        setupProfileImageHandling();

        // Notifications setup
        setupNotifications();
    }

    private void initializeViews() {
        notificationsPopup = findViewById(R.id.notifications_popup);
        notificationsContainer = findViewById(R.id.notifications_container);
        notificationIcon = findViewById(R.id.notification_icon);
        profileImage = findViewById(R.id.profile_image);
        addPhotoButton = findViewById(R.id.btn_add_photo);

        // Initially hide notifications
        if (notificationsPopup != null) {
            notificationsPopup.setVisibility(View.GONE);
        }
        if (notificationIcon != null) {
            notificationIcon.setVisibility(View.GONE);
        }
    }

    private void setupUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        TextView userNameTextView = findViewById(R.id.user_name);
        if (userNameTextView != null) {
            if (currentUser != null && currentUser.getDisplayName() != null) {
                userNameTextView.setText(currentUser.getDisplayName());
            } else {
                userNameTextView.setText("No Name Set");
            }
            userNameTextView.setTypeface(Typeface.SANS_SERIF);
        }
    }

    private void setupClickListeners() {
        View signOutBtn = findViewById(R.id.btnSignOut);
        if (signOutBtn != null) {
            signOutBtn.setOnClickListener(v -> signOut());
        }

        setupFeatureButtons();
        setupChatbotButton();
        setupSettingsButton();
    }

    private void setupProfileImageHandling() {
        if (profileImage != null) {
            profileImage.setOnClickListener(v -> openSettings());
        }
        if (addPhotoButton != null) {
            addPhotoButton.setOnClickListener(v -> showImagePickerDialog());
        }
        loadProfileImage();
    }

    private void setupNotifications() {
        if (notificationIcon != null) {
            notificationIcon.setOnClickListener(v -> toggleNotifications());
        }
        loadNotificationsFromFirebase();
    }

    private void signOut() {
        mAuth.signOut();
        Intent intent = new Intent(Home.this, Signin.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void toggleNotifications() {
        if (notifications.isEmpty()) {
            if (notificationsPopup != null) {
                notificationsPopup.setVisibility(View.GONE);
            }
            return;
        }

        if (notificationsPopup != null) {
            if (notificationsVisible) {
                notificationsPopup.setVisibility(View.GONE);
            } else {
                notificationsPopup.setVisibility(View.VISIBLE);
            }
        }
        notificationsVisible = !notificationsVisible;
    }

    private void setupFeatureButtons() {
        View mapBtn = findViewById(R.id.map);
        if (mapBtn != null) mapBtn.setOnClickListener(v -> openMaps());

        View docBtn = findViewById(R.id.doc);
        if (docBtn != null) docBtn.setOnClickListener(v -> openDocuments());

        View timeBtn = findViewById(R.id.time);
        if (timeBtn != null) timeBtn.setOnClickListener(v -> openTimetable());

        View rattBtn = findViewById(R.id.ratt);
        if (rattBtn != null) rattBtn.setOnClickListener(v -> openRattrapages());

        View absBtn = findViewById(R.id.abs);
        if (absBtn != null) absBtn.setOnClickListener(v -> openAbsences());
    }

    private void setupChatbotButton() {
        View chatbotBtn = findViewById(R.id.btnChatbot);
        if (chatbotBtn != null) {
            chatbotBtn.setOnClickListener(v -> openChatbot());
        }
    }

    private void setupSettingsButton() {
        View settingsBtn = findViewById(R.id.btnSettings);
        if (settingsBtn != null) {
            settingsBtn.setOnClickListener(v -> openSettings());
        }
    }

    // Profile Image Methods
    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Profile Photo");
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: takePhotoFromCamera(); break;
                case 1: chooseImageFromGallery(); break;
                case 2: dialog.dismiss(); break;
            }
        });
        builder.show();
    }

    private void takePhotoFromCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void chooseImageFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                imageUri = data.getData();
                uploadImageToFirebase();
            } else if (requestCode == CAMERA_REQUEST && data != null && data.getExtras() != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                if (photo != null && profileImage != null) {
                    profileImage.setImageBitmap(photo);
                    uploadBitmapToFirebase(photo);
                }
            }
        }
    }

    private void uploadImageToFirebase() {
        if (imageUri != null && mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            StorageReference fileRef = storageRef.child("profile_images/" + userId + ".jpg");

            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            if (profileImage != null) {
                                Glide.with(this).load(uri).into(profileImage);
                            }
                            Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Home", "Upload failed", e);
                    });
        }
    }

    private void uploadBitmapToFirebase(Bitmap bitmap) {
        if (bitmap == null || mAuth.getCurrentUser() == null) return;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        String userId = mAuth.getCurrentUser().getUid();
        StorageReference fileRef = storageRef.child("profile_images/" + userId + ".jpg");

        fileRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        if (profileImage != null) {
                            Glide.with(this).load(uri).into(profileImage);
                        }
                        Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Home", "Upload failed", e);
                });
    }

    private void loadProfileImage() {
        if (mAuth.getCurrentUser() == null || profileImage == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        StorageReference fileRef = storageRef.child("profile_images/" + userId + ".jpg");

        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.human)
                    .into(profileImage);
        }).addOnFailureListener(e -> {
            profileImage.setImageResource(R.drawable.human);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePhotoFromCamera();
        } else if (requestCode == CAMERA_PERMISSION_CODE) {
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRattrapagesNotifications(String profId, Runnable onComplete) {
        db.collection("rattrapages")
                .whereEqualTo("profId", profId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    for (DocumentSnapshot doc : querySnapshot) {
                        try {
                            String matiere = doc.getString("matiere");
                            String groupe = doc.getString("groupe");
                            String classe = doc.getString("classe");
                            String dateStr = doc.getString("date");
                            String heure = doc.getString("heure");

                            if (dateStr != null && matiere != null) {
                                Date rattrapageDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(dateStr);
                                if (rattrapageDate != null && rattrapageDate.compareTo(today.getTime()) >= 0) {
                                    long daysDiff = (rattrapageDate.getTime() - today.getTimeInMillis()) / (24 * 60 * 60 * 1000);
                                    String priority, title;

                                    if (daysDiff == 0) {
                                        priority = "urgent";
                                        title = "🔴 Rattrapage AUJOURD'HUI";
                                    } else if (daysDiff == 1) {
                                        priority = "urgent";
                                        title = "🟡 Rattrapage DEMAIN";
                                    } else if (daysDiff <= 3) {
                                        priority = "warning";
                                        title = "🟠 Rattrapage dans " + daysDiff + " jours";
                                    } else if (daysDiff <= 7) {
                                        priority = "info";
                                        title = "🔵 Rattrapage à venir";
                                    } else {
                                        continue;
                                    }

                                    String description = matiere + " - " + classe + " (" + groupe + ")\n" +
                                            dateStr + " à " + (heure != null ? heure : "heure non définie");

                                    notifications.add(new NotificationItem(title, description, priority, rattrapageDate, "rattrapage"));
                                }
                            }
                        } catch (ParseException e) {
                            Log.e("Home", "Error parsing rattrapage date", e);
                        }
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Error loading rattrapages", e);
                    onComplete.run();
                });
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void loadSeancesNotifications(String profId, Runnable onComplete) {
        db.collection("seances")
                .whereEqualTo("profId", profId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    for (DocumentSnapshot doc : querySnapshot) {
                        try {
                            String matiere = doc.getString("matiere");
                            String groupe = doc.getString("groupe");
                            String classe = doc.getString("classe");
                            String dateStr = doc.getString("date");
                            String heure = doc.getString("heure");

                            if (dateStr != null && matiere != null) {
                                Date seanceDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr);

                                if (seanceDate != null && isSameDay(seanceDate, today.getTime())) {
                                    String title = "📘 Séance aujourd'hui - " + matiere;
                                    String description = matiere + " - " + classe + " (" + groupe + ") | " + dateStr + " à " + (heure != null ? heure : "Heure non définie");

                                    notifications.add(new NotificationItem(title, description, "info", seanceDate, "seance"));
                                }
                            }
                        } catch (ParseException e) {
                            Log.e("Home", "Erreur lors du parsing de la date de séance", e);
                        }
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Erreur lors du chargement des séances", e);
                    onComplete.run();
                });
    }

    private void loadAbsencesNotifications(String profId, Runnable onComplete) {
        db.collection("absences")
                .whereEqualTo("profId", profId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Calendar thirtyDaysAgo = Calendar.getInstance();
                    thirtyDaysAgo.add(Calendar.DAY_OF_MONTH, -30);

                    for (DocumentSnapshot doc : querySnapshot) {
                        try {
                            String dateStr = doc.getString("date");
                            String classe = doc.getString("class");
                            String groupe = doc.getString("group");
                            String remarque = doc.getString("remarque");

                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> studentsData = (List<Map<String, Object>>) doc.get("students");

                            if (dateStr != null && studentsData != null) {
                                Date absenceDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr);

                                if (absenceDate != null && absenceDate.after(thirtyDaysAgo.getTime())) {
                                    int absentCount = 0;
                                    for (Map<String, Object> student : studentsData) {
                                        Boolean present = (Boolean) student.get("present");
                                        if (present != null && !present) absentCount++;
                                    }

                                    if (absentCount > 0) {
                                        String priority = absentCount >= 5 ? "urgent" : "warning";
                                        String title = "📊 " + absentCount + " absence" + (absentCount > 1 ? "s" : "") + " enregistrée" + (absentCount > 1 ? "s" : "");
                                        String description = classe + " (" + groupe + ") - " + dateStr;
                                        if (remarque != null && !remarque.trim().isEmpty()) {
                                            description += "\n" + remarque;
                                        }

                                        notifications.add(new NotificationItem(title, description, priority, absenceDate, "absence"));
                                    }
                                }
                            }
                        } catch (ParseException e) {
                            Log.e("Home", "Error parsing absence date", e);
                        }
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Error loading absences", e);
                    onComplete.run();
                });
    }

    private void loadNotificationsFromFirebase() {
        notifications.clear();
        String currentUserId = mAuth.getUid();
        if (currentUserId == null) {
            displayNotifications();
            return;
        }

        // Counter to ensure all loadings are finished before displaying
        final int[] loadingCount = {3}; // 3 types of notifications to load

        // Helper method to decrement counter and display when everything is loaded
        Runnable checkAndDisplay = () -> {
            loadingCount[0]--;
            if (loadingCount[0] == 0) {
                displayNotifications();
            }
        };

        // Load seances
        loadSeancesNotifications(currentUserId, checkAndDisplay);

        // Load rattrapages
        loadRattrapagesNotifications(currentUserId, checkAndDisplay);

        // Load absences
        loadAbsencesNotifications(currentUserId, checkAndDisplay);
    }

    private void displayNotifications() {
        if (notificationsContainer != null) {
            notificationsContainer.removeAllViews();
        }

        if (notifications.isEmpty()) {
            if (notificationsPopup != null) {
                notificationsPopup.setVisibility(View.GONE);
            }
            if (notificationIcon != null) {
                notificationIcon.setVisibility(View.GONE);
            }
            return;
        }

        if (notificationIcon != null) {
            notificationIcon.setVisibility(View.VISIBLE);
        }
        if (notificationsPopup != null) {
            notificationsPopup.setVisibility(notificationsVisible ? View.VISIBLE : View.GONE);
        }

        // Sort notifications by priority and date
        notifications.sort((n1, n2) -> {
            int priorityCompare = getPriorityWeight(n2.priority) - getPriorityWeight(n1.priority);
            return priorityCompare != 0 ? priorityCompare : n1.date.compareTo(n2.date);
        });

        for (NotificationItem notification : notifications) {
            addNotificationView(notification);
        }
    }

    private int getPriorityWeight(String priority) {
        switch (priority) {
            case "urgent": return 3;
            case "warning": return 2;
            case "info": return 1;
            default: return 0;
        }
    }

    private void addNotificationView(NotificationItem notification) {
        if (notificationsContainer == null) return;

        LinearLayout notificationItem = new LinearLayout(this);
        notificationItem.setOrientation(LinearLayout.HORIZONTAL);
        notificationItem.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        itemParams.setMargins(0, dpToPx(4), 0, dpToPx(4));
        notificationItem.setLayoutParams(itemParams);

        // Set background color based on priority
        int bgColor;

        bgColor = android.R.color.darker_gray;

        notificationItem.setBackgroundColor(ContextCompat.getColor(this, bgColor));

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView titleText = new TextView(this);
        titleText.setText(notification.title);
        titleText.setTextSize(14);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        TextView descText = new TextView(this);
        descText.setText(notification.description);
        descText.setTextSize(12);
        descText.setPadding(0, dpToPx(4), 0, dpToPx(4));
        descText.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        TextView timeText = new TextView(this);
        timeText.setText(getTimeAgo(notification.date));
        timeText.setTextSize(10);
        timeText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

        contentLayout.addView(titleText);
        contentLayout.addView(descText);
        contentLayout.addView(timeText);

        notificationItem.addView(contentLayout);

        notificationItem.setOnClickListener(v -> {
            if (notificationsPopup != null) {
                notificationsPopup.setVisibility(View.GONE);
            }
            notificationsVisible = false;
            switch (notification.type) {
                case "rattrapage": openRattrapages(); break;
                case "absence": openAbsences(); break;
                case "seance": openTimetable(); break;
                case "assignment":
                case "exam": openDocuments(); break;
            }
        });

        notificationsContainer.addView(notificationItem);

        // Add divider between notifications
        if (notifications.indexOf(notification) < notifications.size() - 1) {
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
            divider.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            notificationsContainer.addView(divider);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String getTimeAgo(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();
        long mins = diff / (60 * 1000);
        long hrs = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (days > 0) return days + " jour" + (days > 1 ? "s" : "");
        if (hrs > 0) return hrs + " heure" + (hrs > 1 ? "s" : "");
        if (mins > 0) return mins + " minute" + (mins > 1 ? "s" : "");
        return "À l'instant";
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotificationsFromFirebase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear notifications to prevent memory leaks
        if (notifications != null) {
            notifications.clear();
        }
    }

    // Navigation Methods
    private void openMaps() {
        try {
            startActivity(new Intent(this, maps.class));
        } catch (Exception e) {
            Log.e("Home", "Error opening maps", e);
            Toast.makeText(this, "Unable to open maps", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDocuments() {
        try {
            startActivity(new Intent(this, doc.class));
        } catch (Exception e) {
            Log.e("Home", "Error opening documents", e);
            Toast.makeText(this, "Unable to open documents", Toast.LENGTH_SHORT).show();
        }
    }

    private void openTimetable() {
        try {
            startActivity(new Intent(this, time.class));
        } catch (Exception e) {
            Log.e("Home", "Error opening timetable", e);
            Toast.makeText(this, "Unable to open timetable", Toast.LENGTH_SHORT).show();
        }
    }

    private void openRattrapages() {
        try {
            startActivity(new Intent(this, RattrapagesActivity.class));
        } catch (Exception e) {
            Log.e("Home", "Error opening rattrapages", e);
            Toast.makeText(this, "Unable to open rattrapages", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAbsences() {
        try {
            startActivity(new Intent(this, abs.class));
        } catch (Exception e) {
            Log.e("Home", "Error opening absences", e);
            Toast.makeText(this, "Unable to open absences", Toast.LENGTH_SHORT).show();
        }
    }

    private void openChatbot() {
        try {
            startActivity(new Intent(this, AssistantVirtuel.class));
        } catch (Exception e) {
            Log.e("Home", "Error opening chatbot", e);
            Toast.makeText(this, "Unable to open chatbot", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSettings() {
        try {
            startActivity(new Intent(this, SettingsActivity.class));
        } catch (Exception e) {
            Log.e("Home", "Error opening settings", e);
            Toast.makeText(this, "Unable to open settings", Toast.LENGTH_SHORT).show();
        }
    }

    // NotificationItem class
    public static class NotificationItem {
        public String title, description, priority, type;
        public Date date;

        public NotificationItem(String title, String description, String priority, Date date, String type) {
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.date = date;
            this.type = type;
        }
    }
}