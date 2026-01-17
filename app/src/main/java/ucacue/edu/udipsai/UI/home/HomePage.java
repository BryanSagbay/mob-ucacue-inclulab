package ucacue.edu.udipsai.UI.home;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import ucacue.edu.udipsai.R;
import ucacue.edu.udipsai.UI.listpage.ListPatient;
import ucacue.edu.udipsai.UI.login.LoginActivity;
import ucacue.edu.udipsai.UI.patient.PatientHome;
import ucacue.edu.udipsai.UI.pdf.Pdf;

public class HomePage extends AppCompatActivity {
    private ImageView eagleImage, logout, iconTest, iconPatient, iconPdf;
    private TextView universityText, mottoText;
    private Handler animationHandler = new Handler();
    FirebaseAuth auth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        // Referencias a los elementos
        eagleImage = findViewById(R.id.eagleImage);
        universityText = findViewById(R.id.universityText);
        mottoText = findViewById(R.id.mottoText);
        iconTest = findViewById(R.id.iconTest);
        iconPatient = findViewById(R.id.iconPacientes);
        iconPdf = findViewById(R.id.iconPdf);
        logout = findViewById(R.id.logoutIcon);
        auth = FirebaseAuth.getInstance();

        user = auth.getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(HomePage.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Iniciar la animación en bucle
        startAnimationLoop();

        // Evento click para redirigir a PatientActivity
        iconPatient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePage.this, PatientHome.class);
                startActivity(intent);
            }
        });

        // Evento click para redirigir al TestActivity
        iconTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePage.this, ListPatient.class);
                startActivity(intent);
            }
        });

        // Evento click para redirigir al PdfActivity
        iconPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePage.this, Pdf.class);
                startActivity(intent);
            }
        });
    }

    private void startAnimationLoop() {
        // Animaciones de fade-in (aparición gradual)
        ObjectAnimator fadeInEagle = ObjectAnimator.ofFloat(eagleImage, "alpha", 0f, 1f);
        fadeInEagle.setDuration(2000);

        ObjectAnimator fadeInText = ObjectAnimator.ofFloat(universityText, "alpha", 0f, 1f);
        fadeInText.setDuration(2000);

        ObjectAnimator fadeInMotto = ObjectAnimator.ofFloat(mottoText, "alpha", 0f, 1f);
        fadeInMotto.setDuration(2200);

        // Animaciones de desplazamiento de entrada
        ObjectAnimator slideEagle = ObjectAnimator.ofFloat(eagleImage, "translationX", -500f, 0f);
        slideEagle.setDuration(2000);
        slideEagle.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator slideText = ObjectAnimator.ofFloat(universityText, "translationX", 300f, 0f);
        slideText.setDuration(2000);
        slideText.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator slideMotto = ObjectAnimator.ofFloat(mottoText, "translationX", 300f, 0f);
        slideMotto.setDuration(2200);
        slideMotto.setInterpolator(new AccelerateDecelerateInterpolator());

        // Animación de flotación (ligero movimiento arriba-abajo)
        ObjectAnimator floatUp = ObjectAnimator.ofFloat(eagleImage, "translationY", 0f, -20f);
        floatUp.setDuration(1000);
        floatUp.setRepeatMode(ObjectAnimator.REVERSE);
        floatUp.setRepeatCount(2);

        // Animaciones de fade-out (desaparición)
        ObjectAnimator fadeOutEagle = ObjectAnimator.ofFloat(eagleImage, "alpha", 1f, 0f);
        fadeOutEagle.setDuration(2000);

        ObjectAnimator fadeOutText = ObjectAnimator.ofFloat(universityText, "alpha", 1f, 0f);
        fadeOutText.setDuration(2000);

        ObjectAnimator fadeOutMotto = ObjectAnimator.ofFloat(mottoText, "alpha", 1f, 0f);
        fadeOutMotto.setDuration(2200);

        // Agrupar entrada + flotación
        AnimatorSet entrySet = new AnimatorSet();
        entrySet.playTogether(fadeInEagle, fadeInText, fadeInMotto, slideEagle, slideText, slideMotto);
        entrySet.play(floatUp).after(4000); // Flota después de la entrada

        // Agrupar flotación + salida
        AnimatorSet exitSet = new AnimatorSet();
        exitSet.playTogether(fadeOutEagle, fadeOutText, fadeOutMotto);
        exitSet.setStartDelay(4000); // Esperar 3 segundos antes de desaparecer

        // Ejecutar entrada -> flotación -> salida en secuencia
        AnimatorSet fullAnimation = new AnimatorSet();
        fullAnimation.playSequentially(entrySet, exitSet);
        fullAnimation.start();

        // Repetir la animación después de 8 segundos
        animationHandler.postDelayed(this::startAnimationLoop, 8000);
    }
}
