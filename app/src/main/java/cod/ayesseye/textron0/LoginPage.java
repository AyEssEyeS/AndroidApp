package cod.ayesseye.textron0;

import android.content.Intent;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.View;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import cod.ayesseye.textron0.databinding.ActivityLoginPageBinding;


public class LoginPage extends AppCompatActivity {

    private ActivityLoginPageBinding binding;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    boolean loginbool = false;
    boolean registerbool = false;
    boolean modeCreate = false;

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

    }

    private void setupEdgeToEdge () {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupInitialUI () {
        binding.backgroundImage.setVisibility(View.VISIBLE);
        binding.blankSpace.setVisibility(View.VISIBLE);
        binding.loginButton.setVisibility(View.VISIBLE);
        binding.registerButton.setVisibility(View.VISIBLE);
        binding.loginButton.setTranslationY(0);
        binding.registerButton.setTranslationY(0);

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
        binding.loginButton.setTranslationY(100);

        binding.loginTitle.setText("Welcome back! Glad to see you, Again!");
        binding.loginTitle.setVisibility(View.VISIBLE);
        binding.loginMail.setVisibility(View.VISIBLE);
        binding.loginPassword.setVisibility(View.VISIBLE);
        binding.forgotPass.setVisibility(View.VISIBLE);
        binding.differentLogin.setVisibility(View.VISIBLE);
        binding.backButton.setVisibility(View.VISIBLE);
    }

    private void showRegisterUI () {
        registerbool = true;
        binding.blankSpace.setVisibility(View.GONE);
        binding.backgroundImage.setVisibility(View.GONE);
        binding.loginButton.setVisibility(View.GONE);

        binding.loginTitle.setText("Hello! Register to get started");
        binding.loginTitle.setVisibility(View.VISIBLE);
        binding.backButton.setVisibility(View.VISIBLE);
        binding.registerUsername.setVisibility(View.VISIBLE);
        binding.registerMail.setVisibility(View.VISIBLE);
        binding.registerPass.setVisibility(View.VISIBLE);
        binding.registerConfirm.setVisibility(View.VISIBLE);
        binding.registerButton.setTranslationY(400);
        binding.registerButton.setVisibility(View.VISIBLE);
        binding.differentRegister.setVisibility(View.VISIBLE);
    }
}



//
//        //Setare buton de creare cont
//        createUser.setOnClickListener(v -> {
//            modeCreate = true;
//            promptUser.setText(clearText);             // Eliberare camp
//            promptUser.setHint("Enter Email");
//            promptPass.setHint("Enter Password");
//            btn.setText(R.string.create_account);
//            Toast.makeText(this, "Create new account", Toast.LENGTH_LONG).show();
//        });
//        //Setare buton de logare
//        signIn.setOnClickListener(v -> {
//            modeCreate = false;
//
//            promptPass.setText(clearText);             //Eliberare camp pe apasare
//            promptUser.setHint("Email");
//            promptPass.setHint("Password");
//            btn.setText(R.string.login_button);
//
//            Toast.makeText(this, "Sign in to your account", Toast.LENGTH_SHORT).show();
//        });
//
//        //Creare functionalitate buton si cerere completare field in cazul in care nu sunt completate
//        btn.setOnClickListener(v -> {
//            String email = promptUser.getText().toString().trim();
//            String password = promptPass.getText().toString().trim();
//
//            if (email.isEmpty() || password.isEmpty()){
//                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_LONG).show();
//                return;
//            }if (modeCreate) {
//                createAccount(email, password);
//            } else {
//                signInUser(email,password);
//            }
//
//        });





//    private void createAccount(String email, String password) {
//        mAuth.createUserWithEmailAndPassword(email, password)
//                .addOnCompleteListener(task -> {
//                   if (task.isSuccessful()) {
//                       FirebaseUser user = mAuth.getCurrentUser();
//                       Toast.makeText(this, "Account created" + user.getEmail(), Toast.LENGTH_LONG).show();
//
//                       //Schimbare pe login
//                       modeCreate = false;
//                       promptUser.setHint("Email");
//                       promptPass.setHint("Password");
//                       btn.setText(R.string.login_button);
//                       promptPass.setText(clearText);
//                   } else {
//                       Toast.makeText(this, "Error" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
//                       promptPass.setText(clearText);
//                   }
//                });
//    }
//
//
//    private void signInUser(String email, String password) {
//        mAuth.signInWithEmailAndPassword(email, password)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        FirebaseUser user = mAuth.getCurrentUser();
//                        Toast.makeText(this, "Signed in: " + user.getEmail(), Toast.LENGTH_SHORT).show();
//                        goToMainPage();        //Adaugam intent to main page
//                    } else {
//                        Toast.makeText(this, "Logging in failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
//                        promptPass.setText(clearText);
//                    }
//                });
//    }
//
//    private void goToMainPage(){
//        Intent myIntent = new Intent(this, MainActivity.class);
//        startActivity(myIntent);
//    }

