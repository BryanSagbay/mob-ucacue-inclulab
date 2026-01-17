package ucacue.edu.udipsai.UI.login;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import ucacue.edu.udipsai.R;
import ucacue.edu.udipsai.UI.home.HomePage;
import ucacue.edu.udipsai.UI.register.RegisterActivity;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, errorMessage;
    private FrameLayout loadingOverlay, errorOverlay;
    private ImageView loadingGif, errorLoadingGif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadingGif = findViewById(R.id.loadingGif);
        errorOverlay = findViewById(R.id.errorOverlay);
        errorLoadingGif = findViewById(R.id.errorloadingGif);
        errorMessage = findViewById(R.id.errorMessage);

        Glide.with(this)
                .asGif()
                .load(R.drawable.ic_accesologin)
                .into(loadingGif);

        Glide.with(this)
                .asGif()
                .load(R.drawable.ic_error_login)
                .into(errorLoadingGif);

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingrese correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isNetworkAvailable()) {
            showErrorOverlay("Rebice su conexión a Internet");
            return;
        }

        showLoadingSpinner();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    hideLoadingSpinner();
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            //Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, HomePage.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        showErrorOverlay("Error en la autenticación. Revise sus credenciales");
                    }
                });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Muestra el spinner de carga
     */
    private void showLoadingSpinner() {
        loadingOverlay.setVisibility(View.VISIBLE);
        loadingGif.setVisibility(View.VISIBLE);
    }

    /**
     * Oculta el spinner de carga
     */
    private void hideLoadingSpinner() {
        loadingOverlay.setVisibility(View.GONE);
        loadingGif.setVisibility(View.GONE);
    }

    /**
     * Muestra la alerta de error con el mensaje correspondiente
     */
    private void showErrorOverlay(String message) {
        errorMessage.setText(message);
        errorOverlay.setVisibility(View.VISIBLE);
        errorLoadingGif.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        errorOverlay.setOnClickListener(v -> hideErrorOverlay());
    }

    /**
     * Oculta la alerta de error
     */
    private void hideErrorOverlay() {
        errorOverlay.setVisibility(View.GONE);
        errorLoadingGif.setVisibility(View.GONE);
    }
}
