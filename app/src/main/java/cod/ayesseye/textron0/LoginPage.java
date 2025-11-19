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

    import cod.ayesseye.textron0.databinding.ActivityLoginPageBinding;



    public class LoginPage extends AppCompatActivity {

        private ActivityLoginPageBinding binding;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        GoogleSignInClient mGoogleSignInClient;

        boolean loginbool = false;
        boolean registerbool = false;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);

            FirebaseApp.initializeApp(this);
            mAuth = FirebaseAuth.getInstance();

            binding = ActivityLoginPageBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                                    .build();

            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

            setupEdgeToEdge();
            setupInitialUI();
            setupButtonListeners();

            binding.finalRegister.setOnClickListener(v -> {
                performRegister();
            });

            binding.finalLogin.setOnClickListener(v -> {
                performLogin();
            });
            binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());

            gitHubLogin(binding.btnGithubLogin);
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
            binding.loginShow.setVisibility(View.GONE);
            binding.registerShow.setVisibility(View.GONE);
            binding.btnGithubLogin.setVisibility(View.GONE);
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

            /// Creates logic for show password checkbox
            binding.loginShow.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    binding.loginPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    binding.loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                //sets cursor to the spot it last was on
                binding.loginPassword.setSelection(binding.loginPassword.getText().length());
            });


        }

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

        private void performRegister () {
            String username = binding.registerUsername.getText().toString().trim();
            String email = binding.registerMail.getText().toString().trim();
            String password = binding.registerPass.getText().toString().trim();
            String confirm = binding.registerConfirm.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_LONG).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            assert mAuth.getCurrentUser() != null;
                            String uid = mAuth.getCurrentUser().getUid();

                            Map<String, Object> user = new HashMap<>();
                            user.put("username" , username);
                            user.put("email", email);

                            db.collection("users").document(uid)
                                    .set(user)
                                    .addOnSuccessListener(unused -> {
                                        runOnUiThread(() ->{
                                            Toast.makeText(this, "Account created!", Toast.LENGTH_LONG).show();
                                            setupInitialUI();
                                            showLoginUI();
                                        });

                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
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

        private final int RC_SIGN_IN = 9001;

        private void signInWithGoogle() {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
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

        private void firebaseAuthWithGoogle(String idToken) {
            AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(this, "Google login successful: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                            changePage();
                        } else {
                            Toast.makeText(this, "Firebase auth failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }


        private void gitHubLogin(Button button) {
            button.setOnClickListener(v -> {
                OAuthProvider.Builder provider = OAuthProvider.newBuilder("github.com");
                provider.addCustomParameter("scope", "user:email");


                FirebaseAuth.getInstance()
                        .startActivityForSignInWithProvider(this, provider.build())
                        .addOnSuccessListener(authResult -> {
                            FirebaseUser user = authResult.getUser();
                            Log.d("GitHubLogin", "Succes! User: " + user.getUid());

                            String email = user.getEmail();
                            String name = user.getDisplayName();

                            if (email == null) {
                                Log.w("GitHubLogin", "Email not returned. User has a private GitHub email.");
                            }

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


