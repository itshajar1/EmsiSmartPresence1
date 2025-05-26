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
        notificationsPopup = findViewById(R.id.notifications_popup);
        notificationsContainer = findViewById(R.id.notifications_container);
        notificationIcon = findViewById(R.id.notification_icon);
        profileImage = findViewById(R.id.profile_image);
        addPhotoButton = findViewById(R.id.btn_add_photo);

        // Set up user info
        FirebaseUser currentUser = mAuth.getCurrentUser();
        TextView userNameTextView = findViewById(R.id.user_name);
        if (currentUser != null && currentUser.getDisplayName() != null) {
            userNameTextView.setText(currentUser.getDisplayName());
        } else {
            userNameTextView.setText("No Name Set");
        }
        userNameTextView.setTypeface(Typeface.SANS_SERIF);

        // Set up click listeners
        findViewById(R.id.btnSignOut).setOnClickListener(v -> signOut());
        setupFeatureButtons();
        setupChatbotButton();
        setupSettingsButton();

        // Profile image handling
        profileImage.setOnClickListener(v -> openSettings());
        addPhotoButton.setOnClickListener(v -> showImagePickerDialog());
        loadProfileImage();

        // Notifications setup
        notificationIcon.setOnClickListener(v -> toggleNotifications());
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
            notificationsPopup.setVisibility(View.GONE);
            return;
        }

        if (notificationsVisible) {
            notificationsPopup.setVisibility(View.GONE);
        } else {
            notificationsPopup.setVisibility(View.VISIBLE);
        }
        notificationsVisible = !notificationsVisible;
    }

    private void setupFeatureButtons() {
        findViewById(R.id.map).setOnClickListener(v -> openMaps());
        findViewById(R.id.doc).setOnClickListener(v -> openDocuments());
        findViewById(R.id.time).setOnClickListener(v -> openTimetable());
        findViewById(R.id.ratt).setOnClickListener(v -> openRattrapages());
        findViewById(R.id.abs).setOnClickListener(v -> openAbsences());
    }

    private void setupChatbotButton() {
        findViewById(R.id.btnChatbot).setOnClickListener(v -> openChatbot());
    }

    private void setupSettingsButton() {
        findViewById(R.id.btnSettings).setOnClickListener(v -> openSettings());
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
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        } else {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
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
                profileImage.setImageBitmap(photo);
                uploadBitmapToFirebase(photo);
            }
        }
    }

    private void uploadImageToFirebase() {
        if (imageUri != null) {
            String userId = mAuth.getCurrentUser().getUid();
            StorageReference fileRef = storageRef.child("profile_images/" + userId + ".jpg");

            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            Glide.with(this).load(uri).into(profileImage);
                            Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void uploadBitmapToFirebase(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        String userId = mAuth.getCurrentUser().getUid();
        StorageReference fileRef = storageRef.child("profile_images/" + userId + ".jpg");

        fileRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        Glide.with(this).load(uri).into(profileImage);
                        Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfileImage() {
        String userId = mAuth.getCurrentUser().getUid();
        if (userId == null) return;

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
        if (requestCode == CAMERA_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePhotoFromCamera();
        }
    }

    // Notification Methods (keep your existing notification code here)
    private void loadNotificationsFromFirebase() {
        notifications.clear();
        String currentUserId = mAuth.getUid();
        if (currentUserId == null) {
            displayNotifications();
            return;
        }
        loadRattrapagesNotifications(currentUserId);
        loadAbsencesNotifications(currentUserId);
    }

    private void loadRattrapagesNotifications(String profId) {
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
                                Date rattrapageDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr);
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
                                    } else continue;

                                    String description = matiere + " - " + classe + " (" + groupe + ")\n" +
                                            dateStr + " à " + (heure != null ? heure : "heure non définie");

                                    notifications.add(new NotificationItem(title, description, priority, rattrapageDate, "rattrapage"));
                                }
                            }
                        } catch (ParseException e) {
                            Log.e("Home", "Error parsing rattrapage date", e);
                        }
                    }
                    displayNotifications();
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Error loading rattrapages", e);
                    displayNotifications();
                });
    }

    private void loadAbsencesNotifications(String profId) {
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
                    displayNotifications();
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Error loading absences", e);
                    displayNotifications();
                });
    }

    private void displayNotifications() {
        notificationsContainer.removeAllViews();

        if (notifications.isEmpty()) {
            notificationsPopup.setVisibility(View.GONE);
            notificationIcon.setVisibility(View.GONE);
            return;
        }

        notificationIcon.setVisibility(View.VISIBLE);
        notificationsPopup.setVisibility(notificationsVisible ? View.VISIBLE : View.GONE);

        notifications.sort((n1, n2) -> {
            int priorityCompare = getPriorityWeight(n2.priority) - getPriorityWeight(n1.priority);
            return priorityCompare != 0 ? priorityCompare : n1.date.compareTo(n2.date);
        });

        for (NotificationItem notification : notifications) {
            addNotificationView(notification);
        }
    }

    private void addNotificationView(NotificationItem notification) {
        LinearLayout notificationItem = new LinearLayout(this);
        notificationItem.setOrientation(LinearLayout.HORIZONTAL);
        notificationItem.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        itemParams.setMargins(0, dpToPx(4), 0, dpToPx(4));
        notificationItem.setLayoutParams(itemParams);

        int bgColor;
        switch (notification.priority) {
            case "urgent":
                bgColor = android.R.color.holo_red_light;
                break;
            case "warning":
                bgColor = android.R.color.holo_orange_light;
                break;
            case "info":
                bgColor = android.R.color.holo_blue_light;
                break;
            default:
                bgColor = android.R.color.white;
        }
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
            notificationsPopup.setVisibility(View.GONE);
            notificationsVisible = false;
            switch (notification.type) {
                case "rattrapage": openRattrapages(); break;
                case "absence": openAbsences(); break;
                case "assignment":
                case "exam": openDocuments(); break;
            }
        });

        notificationsContainer.addView(notificationItem);

        if (notifications.indexOf(notification) < notifications.size() - 1) {
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
            divider.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            notificationsContainer.addView(divider);
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

    // Navigation Methods
    private void openMaps() {
        startActivity(new Intent(this, maps.class));
    }

    private void openDocuments() {
        startActivity(new Intent(this, doc.class));
    }

    private void openTimetable() {
        startActivity(new Intent(this, time.class));
    }

    private void openRattrapages() {
        startActivity(new Intent(this, RattrapagesActivity.class));
    }

    private void openAbsences() {
        startActivity(new Intent(this, abs.class));
    }

    private void openChatbot() {
        startActivity(new Intent(this, AssistantVirtuel.class));
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

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