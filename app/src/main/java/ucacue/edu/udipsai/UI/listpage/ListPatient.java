package ucacue.edu.udipsai.UI.listpage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import ucacue.edu.udipsai.Model.Patient;
import ucacue.edu.udipsai.R;
import ucacue.edu.udipsai.Services.ListAdapter;
import ucacue.edu.udipsai.UI.home.HomePage;
import ucacue.edu.udipsai.UI.test.HomeTest;

public class ListPatient extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView txtNoDatos;
    private FirebaseFirestore db;
    private List<Patient> listaPacientes;
    private ListAdapter adapter;
    private Button btnCancelar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_patient);

        recyclerView = findViewById(R.id.recyclerViewPacientes);
        txtNoDatos = findViewById(R.id.txtNoDatos);
        btnCancelar = findViewById(R.id.btnCancelar);

        db = FirebaseFirestore.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaPacientes = new ArrayList<>();
        adapter = new ListAdapter(listaPacientes, this::onPatientSelected);
        recyclerView.setAdapter(adapter);

        // Botón para regresar
        btnCancelar.setOnClickListener(v -> {
            Intent homeIntent = new Intent(ListPatient.this, HomePage.class);
            startActivity(homeIntent);
            finish();
        });

        cargarPacientes();
    }

    private void cargarPacientes() {
        db.collection("pacientes").orderBy("fechaRegistro", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        listaPacientes.clear();
                        for (DocumentSnapshot doc : task.getResult()) {
                            Patient paciente = doc.toObject(Patient.class);
                            if (paciente != null) {
                                listaPacientes.add(paciente);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        txtNoDatos.setVisibility(listaPacientes.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Toast.makeText(this, "Error al cargar pacientes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onPatientSelected(Patient patient) {
        String nombrePaciente = patient.getNombre() + " " + patient.getApellido();

        // Guardar en SharedPreferences para mantener el paciente seleccionado
        getSharedPreferences("PatientPrefs", MODE_PRIVATE)
                .edit()
                .putString("patient_name", nombrePaciente)
                .apply();

        Intent intent = new Intent(this, HomeTest.class);
        intent.putExtra("patient_name", nombrePaciente);
        startActivity(intent);
    }
}