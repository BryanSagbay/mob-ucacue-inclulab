package ucacue.edu.udipsai.UI.patient;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import ucacue.edu.udipsai.Model.Patient;
import ucacue.edu.udipsai.R;
import ucacue.edu.udipsai.Services.PatientAdapter;
import ucacue.edu.udipsai.UI.home.HomePage;

public class PatientHome extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView txtNoDatos;
    private ImageButton backButton;
    private FloatingActionButton fabAdd;
    private FirebaseFirestore db;
    private List<Patient> listaPacientes;
    private PatientAdapter adapter;

    // Componentes de alerta personalizada
    private FrameLayout loadingOverlay, errorOverlay;
    private TextView errorMessage;
    private ImageView loadingGif, errorloadingGif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_patient);

        // Inicialización de vistas
        recyclerView = findViewById(R.id.recyclerViewPacientes);
        txtNoDatos = findViewById(R.id.txtNoDatos);
        fabAdd = findViewById(R.id.fab_add);
        backButton = findViewById(R.id.back_button);

        // Inicializar los overlays de alerta
        loadingOverlay = findViewById(R.id.loadingOverlay);
        errorOverlay = findViewById(R.id.errorOverlay);
        errorMessage = findViewById(R.id.errorMessage);
        loadingGif = findViewById(R.id.loadingGif);
        errorloadingGif = findViewById(R.id.errorloadingGif);

        // Cargar los GIFs con Glide
        Glide.with(this).asGif().load(R.drawable.ic_carpeta).into(loadingGif);
        Glide.with(this).asGif().load(R.drawable.ic_error_login).into(errorloadingGif);

        // Inicializar Firestore y RecyclerView
        db = FirebaseFirestore.getInstance();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaPacientes = new ArrayList<>();
        adapter = new PatientAdapter(listaPacientes, this);
        recyclerView.setAdapter(adapter);

        // Acciones de botones
        backButton.setOnClickListener(v -> {
            Intent homeIntent = new Intent(PatientHome.this, HomePage.class);
            startActivity(homeIntent);
            finish();
        });

        fabAdd.setOnClickListener(v -> mostrarDialogoAgregarPaciente());

        // Cargar pacientes desde Firestore
        cargarPacientes();

        // Listeners para cerrar alertas al hacer clic en ellas
        loadingOverlay.setOnClickListener(v -> ocultarAlertaCorrecto());
        errorOverlay.setOnClickListener(v -> ocultarAlertaError());
    }

    /**
     * Carga los pacientes desde Firestore y los ordena por fecha de registro en orden descendente.
     */
    private void cargarPacientes() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            mostrarAlertaError("Usuario no autenticado.");
            return;
        }

        String correoUsuario = user.getEmail();

        db.collection("pacientes")
                .whereEqualTo("correoUsuario", correoUsuario)
                .orderBy("fechaRegistro", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        mostrarAlertaError("Error al cargar datos: " + error.getMessage());
                        return;
                    }

                    listaPacientes.clear();
                    if (value != null && !value.isEmpty()) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Patient paciente = doc.toObject(Patient.class);
                            if (paciente != null) {
                                listaPacientes.add(paciente);
                            }
                        }
                        txtNoDatos.setVisibility(View.GONE);
                    } else {
                        txtNoDatos.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                });
    }


    /**
     * Muestra un diálogo para agregar un nuevo paciente.
     */
    private void mostrarDialogoAgregarPaciente() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.modal_paciente, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText edtNombre = dialogView.findViewById(R.id.edtNombre);
        EditText edtApellido = dialogView.findViewById(R.id.edtApellido);
        RadioGroup radioGroupGenero = dialogView.findViewById(R.id.radioGroupGenero);
        EditText edtEdad = dialogView.findViewById(R.id.edtEdad);
        EditText edtDireccion = dialogView.findViewById(R.id.edtDireccion);
        EditText edtTelefono = dialogView.findViewById(R.id.edtTelefono);
        Button btnAgregar = dialogView.findViewById(R.id.btnAgregar);

        btnAgregar.setOnClickListener(v -> {
            String nombre = edtNombre.getText().toString().trim();
            String apellido = edtApellido.getText().toString().trim();
            String edadStr = edtEdad.getText().toString().trim();
            String direccion = edtDireccion.getText().toString().trim();
            String telefono = edtTelefono.getText().toString().trim();

            // Obtener género seleccionado
            int selectedId = radioGroupGenero.getCheckedRadioButtonId();
            String genero = selectedId == R.id.rbMasculino ? "Masculino" : "Femenino";

            if (nombre.isEmpty() || apellido.isEmpty() || genero.isEmpty() || edadStr.isEmpty() || direccion.isEmpty() || telefono.isEmpty()) {
                mostrarAlertaError("Todos los campos son obligatorios");
                return;
            }

            int edad = Integer.parseInt(edadStr);
            dialog.dismiss();
            agregarPaciente(nombre, apellido, genero, edad, direccion, telefono);
        });
    }

    /**
     * Agrega un nuevo paciente a Firestore.
     */
    private void agregarPaciente(String nombre, String apellido, String genero, int edad, String direccion, String telefono) {
        long timestamp = System.currentTimeMillis();

        // Obtener el usuario autenticado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String correoUsuario = (user != null) ? user.getEmail() : "No autenticado";

        // Crear el objeto Patient con el correo del usuario
        Patient paciente = new Patient(nombre, apellido, genero, edad, direccion, telefono, correoUsuario);

        mostrarAlertaCargando();

        db.collection("pacientes").document(String.valueOf(timestamp))
                .set(paciente)
                .addOnSuccessListener(aVoid -> {
                    mostrarAlertaCorrecto("Paciente agregado con éxito.");
                    cargarPacientes();
                })
                .addOnFailureListener(e -> mostrarAlertaError("Error al agregar paciente: " + e.getMessage()));
    }



    /**
     * Métodos de alertas personalizadas
     */
    public void mostrarAlertaCargando() {
        loadingOverlay.setVisibility(View.VISIBLE);
        loadingGif.setVisibility(View.VISIBLE);
    }

    public void mostrarAlertaCorrecto(String mensaje) {
        loadingOverlay.setVisibility(View.VISIBLE);
        loadingGif.setVisibility(View.VISIBLE);

        new Handler().postDelayed(this::ocultarAlertaCorrecto, 2000);
    }

    public void ocultarAlertaCorrecto() {
        loadingOverlay.setVisibility(View.GONE);
        loadingGif.setVisibility(View.GONE);
    }

    public void mostrarAlertaError(String mensaje) {
        errorMessage.setText(mensaje);
        errorOverlay.setVisibility(View.VISIBLE);
        errorloadingGif.setVisibility(View.VISIBLE);

        new Handler().postDelayed(this::ocultarAlertaError, 3000);
    }

    public void ocultarAlertaError() {
        errorOverlay.setVisibility(View.GONE);
        errorloadingGif.setVisibility(View.GONE);
    }
}
