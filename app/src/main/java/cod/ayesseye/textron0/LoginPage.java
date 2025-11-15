package cod.ayesseye.textron0;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class LoginPage extends AppCompatActivity {

    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    EditText promptUser, promptPass;
    TextView createUser, signIn;
    Button btn;

    boolean modeCreate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        signIn = findViewById(R.id.signIn);
        promptUser = findViewById(R.id.promptUser);
        promptPass = findViewById(R.id.promptPassword);
        createUser = findViewById(R.id.createUser);
        btn = findViewById(R.id.login_button);

        //Setare buton de creare cont
        createUser.setOnClickListener(v -> {
            modeCreate = true;

            promptUser.setHint("Enter Email");
            promptPass.setHint("Enter Password");
            btn.setText(R.string.create_account);

            Toast.makeText(this, "Create new account", Toast.LENGTH_LONG).show();
        });
        //Setare buton de logare
        signIn.setOnClickListener(v -> {
            modeCreate = false;

            promptUser.setHint("Email");
            promptPass.setHint("Password");
            btn.setText(R.string.login_button);

            Toast.makeText(this, "Sign in to your account", Toast.LENGTH_SHORT).show();
        });

        //Creare functionalitate buton si cerere completare field in cazul in care nu sunt completate
        btn.setOnClickListener(v -> {
            String email = promptUser.getText().toString().trim();
            String password = promptPass.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_LONG).show();
                return;
            }if (modeCreate) {
                createAccount(email, password);
            } else {
                signInUser(email,password);
            }

        });



    }

    private void createAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                   if (task.isSuccessful()) {
                       FirebaseUser user = mAuth.getCurrentUser();
                       Toast.makeText(this, "Account created" + user.getEmail(), Toast.LENGTH_LONG).show();
                   } else {
                       Toast.makeText(this, "Error" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                   }
                });
    }


    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Signed in: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        //Adaugam intent to main page
                    } else {
                        Toast.makeText(this, "Loging failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }

                });
    }

}