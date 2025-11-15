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

        createUser.setOnClickListener(v -> {
            modeCreate = true;

            promptUser.setHint("Enter Email");
            promptPass.setHint("Enter Password");
            btn.setText(R.string.create_account);

            Toast.makeText(this, "Create new account", Toast.LENGTH_LONG).show();
        });

        signIn.setOnClickListener(v -> {
            modeCreate = false;

            promptUser.setHint("Email");
            promptPass.setHint("Password");
            btn.setText(R.string.login_button);

            Toast.makeText(this, "Sign in to your account", Toast.LENGTH_SHORT).show();
        });

    }
}