package cod.ayesseye.textron0;

import android.content.Intent;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import cod.ayesseye.textron0.databinding.ActivityLoginPageBinding;


public class LoginPage extends AppCompatActivity {

    private ActivityLoginPageBinding binding;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    boolean loginbool = false;
    boolean registerbool = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityLoginPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        setupEdgeToEdge();
        setupInitialUI();
        setupButtonListeners();

        binding.finalRegister.setOnClickListener(v -> {
            performRegister();
        });

        binding.finalLogin.setOnClickListener(v -> {
            performLogin();
        });
    }


    private void setupEdgeToEdge () {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupInitialUI () {
        registerbool = false;
        loginbool = false;

        binding.backgroundImage.setVisibility(View.VISIBLE);
        binding.blankSpace.setVisibility(View.VISIBLE);
        binding.loginButton.setVisibility(View.VISIBLE);
        binding.registerButton.setVisibility(View.VISIBLE);

        binding.loginTitle.setVisibility(View.GONE);
        binding.loginMail.setVisibility(View.GONE);
        binding.loginPassword.setVisibility(View.GONE);
        binding.forgotPass.setVisibility(View.GONE);
        binding.backButton.setVisibility(View.GONE);
        binding.differentLogin.setVisibility(View.GONE);
        binding.registerConfirm.setVisibility(View.GONE);
        binding.registerMail.setVisibility(View.GONE);
        binding.differentRegister.setVisibility(View.GONE);
        binding.registerPass.setVisibility(View.GONE);
        binding.registerUsername.setVisibility(View.GONE);
        binding.finalRegister.setVisibility(View.GONE);
        binding.finalLogin.setVisibility(View.GONE);
    }

    private void setupButtonListeners() {
        binding.loginButton.setOnClickListener(v -> showLoginUI());
        binding.registerButton.setOnClickListener(v -> showRegisterUI());
        binding.backButton.setOnClickListener(v -> setupInitialUI());
    }

    private void showLoginUI () {
        loginbool = true;

        binding.blankSpace.setVisibility(View.GONE);
        binding.backgroundImage.setVisibility(View.GONE);
        binding.registerButton.setVisibility(View.GONE);
        binding.loginButton.setVisibility(View.GONE);

        binding.loginTitle.setText("Welcome back! Glad to see you, Again!");
        binding.loginTitle.setVisibility(View.VISIBLE);
        binding.loginMail.setVisibility(View.VISIBLE);
        binding.loginPassword.setVisibility(View.VISIBLE);
        binding.forgotPass.setVisibility(View.VISIBLE);
        binding.differentLogin.setVisibility(View.VISIBLE);
        binding.backButton.setVisibility(View.VISIBLE);
        binding.finalLogin.setVisibility(View.VISIBLE);
    }

    private void showRegisterUI () {
        registerbool = true;

        binding.blankSpace.setVisibility(View.GONE);
        binding.backgroundImage.setVisibility(View.GONE);
        binding.loginButton.setVisibility(View.GONE);
        binding.registerButton.setVisibility(View.GONE);

        binding.loginTitle.setText("Hello! Register to get started");
        binding.loginTitle.setVisibility(View.VISIBLE);
        binding.backButton.setVisibility(View.VISIBLE);
        binding.registerUsername.setVisibility(View.VISIBLE);
        binding.registerMail.setVisibility(View.VISIBLE);
        binding.registerPass.setVisibility(View.VISIBLE);
        binding.registerConfirm.setVisibility(View.VISIBLE);
        binding.finalRegister.setVisibility(View.VISIBLE);
        binding.differentRegister.setVisibility(View.VISIBLE);
    }

    private void performRegister () {
        String username = binding.registerUsername.getText().toString().trim();
        String email = binding.registerMail.getText().toString().trim();
        String password = binding.registerPass.getText().toString().trim();
        String confirm = binding.registerConfirm.getText().toString().trim();

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();

                        Map<String, Object> user = new HashMap<>();
                        user.put("username" , username);
                        user.put("email", email);

                        db.collection("users").document(uid)
                                .set(user)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Account created!", Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                });

                    } else {
                        Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void performLogin () {
        String identifier = binding.loginMail.getText().toString().trim();
        String password = binding.loginPassword.getText().toString().trim();

        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill al fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (identifier.contains("@")) {
            loginWithEmail(identifier, password);
            return;
        }

        db.collection("users")
                .whereEqualTo("username", identifier)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        String email = snap.getDocuments().get(0).getString("email");
                        loginWithEmail(email, password);
                    } else {
                        Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loginWithEmail (String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login succesfull", Toast.LENGTH_SHORT).show();

                        changePage();
                    } else {
                        Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void changePage() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
