package cod.ayesseye.textron0;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import cod.ayesseye.textron0.databinding.ActivityLoginPageBinding;

// Activitatea principala pentru gestionarea autentificarii (Email, Google, GitHub)
public class LoginPage extends AppCompatActivity {

    private ActivityLoginPageBinding binding;

    // Instantiere servicii Firebase: Auth pentru login si Firestore pentru date utilizator
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    GoogleSignInClient mGoogleSignInClient;

    // Variabile de stare pentru a urmari in ce meniu se afla utilizatorul
    boolean loginbool = false;
    boolean registerbool = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        // Verifica daca utilizatorul este deja logat; daca da, redirectioneaza direct
        if (mAuth.getCurrentUser() != null) {
            changePage();
            finish();
            return;
        }

        binding = ActivityLoginPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configurare optiuni pentru Google Sign-In (solicita email si ID token)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        setupEdgeToEdge();
        setupInitialUI(); // Pregateste interfata vizuala de start
        setupButtonListeners(); // Activeaza logica butoanelor

        // Listener pentru butonul final de inregistrare
        binding.finalRegister.setOnClickListener(v -> {
            performRegister();
        });

        // Listener pentru butonul final de logare
        binding.finalLogin.setOnClickListener(v -> {
            performLogin();
        });

        // Listener pentru Google Sign-In
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        // Initializare logica pentru GitHub Sign-In
        gitHubLogin(binding.btnGithubLogin);
    }

    // Configureaza afisarea corecta edge-to-edge (padding pentru barele de sistem)
    private void setupEdgeToEdge () {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Reseteaza UI-ul la ecranul de alegere (Login vs Register) si ascunde campurile de input
    private void setupInitialUI () {
        registerbool = false;
        loginbool = false;

        binding.backgroundImage.setVisibility(View.VISIBLE);
        binding.blankSpace.setVisibility(View.VISIBLE);
        binding.loginButton.setVisibility(View.VISIBLE);
        binding.registerButton.setVisibility(View.VISIBLE);

        // Ascunde toate elementele specifice formularelor detaliate
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
        binding.loginShow.setVisibility(View.GONE);
        binding.registerShow.setVisibility(View.GONE);
        binding.btnGithubLogin.setVisibility(View.GONE);
    }

    // Navigarea intre starile UI (Catre Login, Catre Register, sau Inapoi)
    private void setupButtonListeners() {
        binding.loginButton.setOnClickListener(v -> showLoginUI());
        binding.registerButton.setOnClickListener(v -> showRegisterUI());
        binding.backButton.setOnClickListener(v -> setupInitialUI());
    }

    // Afiseaza formularul de Login si ascunde meniul principal
    private void showLoginUI () {
        loginbool = true;

        binding.blankSpace.setVisibility(View.GONE);
        binding.backgroundImage.setVisibility(View.GONE);
        binding.registerButton.setVisibility(View.GONE);
        binding.loginButton.setVisibility(View.GONE);

        binding.loginTitle.setText(R.string.login_title);
        binding.loginTitle.setVisibility(View.VISIBLE);
        binding.loginMail.setVisibility(View.VISIBLE);
        binding.loginPassword.setVisibility(View.VISIBLE);
        binding.forgotPass.setVisibility(View.VISIBLE);
        binding.differentLogin.setVisibility(View.VISIBLE);
        binding.backButton.setVisibility(View.VISIBLE);
        binding.finalLogin.setVisibility(View.VISIBLE);
        binding.loginShow.setVisibility(View.VISIBLE);
        binding.btnGithubLogin.setVisibility(View.VISIBLE);

        // Logica pentru checkbox-ul "Arata parola" (Show Password)
        binding.loginShow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.loginPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                binding.loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            // Mentine cursorul la finalul textului
            binding.loginPassword.setSelection(binding.loginPassword.getText().length());
        });
    }

    // Afiseaza formularul de Register si ascunde meniul principal
    private void showRegisterUI () {
        registerbool = true;

        binding.blankSpace.setVisibility(View.GONE);
        binding.backgroundImage.setVisibility(View.GONE);
        binding.loginButton.setVisibility(View.GONE);
        binding.registerButton.setVisibility(View.GONE);

        binding.loginTitle.setText(R.string.register_title);
        binding.loginTitle.setVisibility(View.VISIBLE);
        binding.backButton.setVisibility(View.VISIBLE);
        binding.registerUsername.setVisibility(View.VISIBLE);
        binding.registerMail.setVisibility(View.VISIBLE);
        binding.registerPass.setVisibility(View.VISIBLE);
        binding.registerConfirm.setVisibility(View.VISIBLE);
        binding.finalRegister.setVisibility(View.VISIBLE);
        binding.differentRegister.setVisibility(View.VISIBLE);
        binding.registerShow.setVisibility(View.VISIBLE);
        binding.btnGithubLogin.setVisibility(View.VISIBLE);

        // Logica pentru afisarea parolei in ambele campuri (parola si confirmare)
        binding.registerShow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.registerPass.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.registerConfirm.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                binding.registerPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.registerConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }

            binding.registerPass.setSelection(binding.registerPass.getText().length());
            binding.registerConfirm.setSelection(binding.registerConfirm.getText().length());
        });
    }

    // Logica de inregistrare: Validare input, Creare cont Auth, Salvare User in Firestore
    private void performRegister () {
        String username = binding.registerUsername.getText().toString().trim();
        String email = binding.registerMail.getText().toString().trim();
        String password = binding.registerPass.getText().toString().trim();
        String confirm = binding.registerConfirm.getText().toString().trim();

        // Validare campuri goale
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validare potrivire parole
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_LONG).show();
            return;
        }

        // Creare utilizator in Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        assert mAuth.getCurrentUser() != null;
                        String uid = mAuth.getCurrentUser().getUid();

                        // Pregatire date utilizator pentru baza de date
                        Map<String, Object> user = new HashMap<>();
                        user.put("username" , username);
                        user.put("email", email);

                        // Salvare date in colectia "users" din Firestore
                        db.collection("users").document(uid)
                                .set(user)
                                .addOnSuccessListener(unused -> {
                                    runOnUiThread(() ->{
                                        Toast.makeText(this, "Account created!", Toast.LENGTH_LONG).show();
                                        setupInitialUI();
                                        showLoginUI(); // Redirectionare la login dupa succes
                                    });

                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                    } else {
                        Exception e = task.getException();
                        String message = (e != null) ? e.getMessage() : "Unknown error occurred";
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Logica de autentificare: Suporta atat Email cat si Username
    private void performLogin () {
        String identifier = binding.loginMail.getText().toString().trim();
        String password = binding.loginPassword.getText().toString().trim();


        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill al fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Daca inputul contine '@', tratam ca Email
        if (identifier.contains("@")) {
            loginWithEmail(identifier, password);
            return;
        }

        // Daca nu e email, cautam Username-ul in baza de date pentru a obtine email-ul
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

    // Metoda helper pentru autentificarea efectiva folosind Email si Parola
    private void loginWithEmail (String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login succesfull", Toast.LENGTH_SHORT).show();
                        changePage();
                    } else {
                        Exception e = task.getException();
                        String message = (e != null) ? e.getMessage() : "Unknown error occurred";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Navigare catre activitatea principala a aplicatiei (MainActivity)
    private void changePage() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private final int RC_SIGN_IN = 9001;

    // Initiaza intent-ul de logare cu Google
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Gestioneaza rezultatul intors de Google Sign-In
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }
    }

    // Autentifica utilizatorul in Firebase folosind token-ul de la Google
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();


                        Toast.makeText(this,
                                "Google login successful: " + Objects.requireNonNull(user).getEmail(),
                                Toast.LENGTH_SHORT).show();

                        changePage();
                    } else {
                        Exception e = task.getException();
                        String message = (e != null) ? e.getMessage() : "Unknown error occurred";

                        Toast.makeText(this,
                                message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // Logica pentru autentificarea cu GitHub folosind OAuthProvider
    private void gitHubLogin(Button button) {
        button.setOnClickListener(v -> {
            OAuthProvider.Builder provider = OAuthProvider.newBuilder("github.com");
            provider.addCustomParameter("scope", "user:email");


            FirebaseAuth.getInstance()
                    .startActivityForSignInWithProvider(this, provider.build())
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = authResult.getUser();
                        Log.d("GitHubLogin", "Succes! User: " + Objects.requireNonNull(user).getUid());

                        String email = user.getEmail();
                        String name = user.getDisplayName();

                        if (email == null) {
                            Log.w("GitHubLogin", "Email not returned. User has a private GitHub email.");
                        }

                        // Salvare date utilizator GitHub in Firestore
                        Map<String, Object> data = new HashMap<>();
                        data.put("email", email);
                        data.put("username", name);

                        db.collection("users")
                                .document(user.getUid())
                                .set(data)
                                .addOnSuccessListener(unused ->  Log.d("Firestore", "GitHub user saved"))
                                .addOnFailureListener(e -> Log.e("Firestore", "Error" + e));
                        Toast.makeText(this, "Logged in as " + name, Toast.LENGTH_LONG).show();

                        changePage();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("GitHubLogin", "Error: " + e.getMessage());
                        Toast.makeText(this, "GitHub Login Failed", Toast.LENGTH_LONG).show();
                    });
        });
    }
}