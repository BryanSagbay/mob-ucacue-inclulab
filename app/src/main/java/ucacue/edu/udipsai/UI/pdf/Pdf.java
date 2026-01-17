package ucacue.edu.udipsai.UI.pdf;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.io.OutputStream;
import java.util.*;

import ucacue.edu.udipsai.R;
import ucacue.edu.udipsai.Services.AuthService;
import ucacue.edu.udipsai.Services.FirestoreService;
import ucacue.edu.udipsai.Services.PDFGenerator;
import ucacue.edu.udipsai.UI.home.HomePage;

public class Pdf extends AppCompatActivity {

    private TextView textViewCorreo, textViewFechaSeleccionada, errorMessage;
    private Button btnGenerarPDF;
    private ImageButton btnSeleccionarFecha, backButton, btnSeleccionarPaciente;
    private DatePicker datePicker;
    private FirestoreService firestoreService;
    private String correoUsuario;
    private String fechaSeleccionada;
    private FrameLayout loadingOverlay, errorOverlay;
    private ImageView loadingGif, errorloadingGif;
    private Spinner spinnerPacientes;
    private List<String> listaPacientes = new ArrayList<>();
    private String pacienteSeleccionado = "";


    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pdf_home);

        // Vincular vistas
        textViewCorreo = findViewById(R.id.textViewCorreo);
        textViewFechaSeleccionada = findViewById(R.id.textViewFechaSeleccionada);
        btnGenerarPDF = findViewById(R.id.btnGenerarPDF);
        datePicker = findViewById(R.id.datePicker);
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha);
        backButton = findViewById(R.id.back_button);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        errorOverlay = findViewById(R.id.errorOverlay);
        errorMessage = findViewById(R.id.errorMessage);
        loadingGif = findViewById(R.id.loadingGif);
        errorloadingGif = findViewById(R.id.errorloadingGif);
        spinnerPacientes = findViewById(R.id.spinnerPacientes);
        btnSeleccionarPaciente = findViewById(R.id.btnMore);

        // Cargar GIFs con Glide
        Glide.with(this).asGif().load(R.drawable.ic_carpeta).into(loadingGif);
        Glide.with(this).asGif().load(R.drawable.ic_error_login).into(errorloadingGif);

        // Inicializar FirestoreService
        try {
            firestoreService = new FirestoreService();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Obtener el correo del usuario autenticado
        correoUsuario = AuthService.getUserEmail();
        if (correoUsuario != null) {
            textViewCorreo.setText(correoUsuario);
        } else {
            textViewCorreo.setText("No autenticado");
            btnGenerarPDF.setEnabled(false);
        }

        // Ocultar DatePicker y Spinner por defecto
        datePicker.setVisibility(View.GONE);
        spinnerPacientes.setVisibility(View.GONE);

        // Configurar DatePicker
        configurarDatePicker();

        // Cargar pacientes en el Spinner
        cargarPacientes();

        // Configurar el botón para mostrar/ocultar el Spinner
        btnSeleccionarPaciente.setOnClickListener(v -> {
            if (spinnerPacientes.getVisibility() == View.GONE) {
                spinnerPacientes.setVisibility(View.VISIBLE);
            } else {
                spinnerPacientes.setVisibility(View.GONE);
            }
        });

        // Eventos de clic
        btnSeleccionarFecha.setOnClickListener(v -> mostrarDatePicker());
        btnGenerarPDF.setOnClickListener(v -> verificarPermisosYGenerarPDF());

        // Botón para regresar
        backButton.setOnClickListener(v -> {
            Intent homeIntent = new Intent(Pdf.this, HomePage.class);
            startActivity(homeIntent);
            finish();
        });

        // Ocultar overlays por defecto
        loadingOverlay.setVisibility(View.GONE);
        errorOverlay.setVisibility(View.GONE);
    }
    private void mostrarDatePicker() {
        if (datePicker.getVisibility() == View.GONE) {
            datePicker.setVisibility(View.VISIBLE);
        } else {
            datePicker.setVisibility(View.GONE);
        }
    }

    private void configurarDatePicker() {
        Calendar calendario = Calendar.getInstance();
        int año = calendario.get(Calendar.YEAR);
        int mes = calendario.get(Calendar.MONTH);
        int día = calendario.get(Calendar.DAY_OF_MONTH);

        datePicker.init(año, mes, día, (view, year, monthOfYear, dayOfMonth) -> {
            fechaSeleccionada = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
            textViewFechaSeleccionada.setText(fechaSeleccionada);
            datePicker.setVisibility(View.GONE);

        });

        // Deshabilitar fechas futuras
        datePicker.setMaxDate(calendario.getTimeInMillis());
    }

    private void verificarPermisosYGenerarPDF() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                generarPDF();
            }
        } else {
            generarPDF();
        }
    }

    private void generarPDF() {
        if (correoUsuario.equals("No autenticado")) {
            mostrarError("No autenticado. Inicie sesión.");
            return;
        }

        mostrarLoading(true);
        btnGenerarPDF.setEnabled(false);

        Log.d("PDF", "Generando PDF: Email=" + correoUsuario +
                ", Fecha=" + (fechaSeleccionada == null ? "null" : fechaSeleccionada) +
                ", Paciente=" + (pacienteSeleccionado == null ? "null" : pacienteSeleccionado));

        new Thread(() -> {
            try {
                List<Map<String, Object>> resultados = firestoreService.getAllDataByEmailDateAndPaciente(
                        correoUsuario,
                        fechaSeleccionada,  // Puede estar vacío o nulo
                        pacienteSeleccionado // Puede estar vacío o nulo
                );

                Log.d("PDF", "Resultados obtenidos: " + resultados.size());

                if (!resultados.isEmpty()) {
                    Uri pdfUri = guardarPDF(correoUsuario,
                            fechaSeleccionada != null && !fechaSeleccionada.isEmpty() ?
                                    fechaSeleccionada : "Todas", resultados);
                    runOnUiThread(() -> {
                        if (pdfUri != null) {
                            Toast.makeText(this, "PDF guardado en Descargas", Toast.LENGTH_SHORT).show();
                            resetearFiltros();
                        } else {
                            mostrarError("Error al guardar el PDF");
                        }
                    });
                } else {
                    mostrarError("No hay datos disponibles.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                mostrarError("Error generando PDF: " + e.getMessage());
            } finally {
                runOnUiThread(() -> {
                    mostrarLoading(false);
                    btnGenerarPDF.setEnabled(true);
                });
            }
        }).start();
    }
    private void mostrarLoading(boolean mostrar) {
        runOnUiThread(() -> {
            if (mostrar) {
                loadingOverlay.setVisibility(View.VISIBLE);
                loadingGif.setVisibility(View.VISIBLE);
            } else {
                loadingOverlay.setVisibility(View.GONE);
                loadingGif.setVisibility(View.GONE);
            }
        });
    }

    private void mostrarError(String mensaje) {
        runOnUiThread(() -> {
            errorMessage.setText(mensaje);
            errorOverlay.setVisibility(View.VISIBLE);
            errorloadingGif.setVisibility(View.VISIBLE);

            // Ocultar el error después de 3 segundos
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                errorOverlay.setVisibility(View.GONE);
                errorloadingGif.setVisibility(View.GONE);
            }, 3000);
        });
    }

    private Uri guardarPDF(String email, String date, List<Map<String, Object>> dataList) {
        String fileName = "Reporte_" + email + "_" + (date.equals("Todas") ? "Todos" : date) + ".pdf";

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        ContentResolver resolver = getContentResolver();
        Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);

        if (uri != null) {
            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                PDFGenerator.generatePDF(outputStream, email, date, dataList, pacienteSeleccionado);
                return uri;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generarPDF();
            } else {
                mostrarError("Se requieren permisos para guardar el PDF");
            }
        }
    }

    private void cargarPacientes() {
        firestoreService.getPacientesPorUsuario(correoUsuario, new FirestoreService.PacientesCallback() {
            @Override
            public void onCallback(List<String> pacientes) {
                runOnUiThread(() -> {
                    listaPacientes = new ArrayList<>();
                    // Agregar una opción "Todos los pacientes" al principio de la lista
                    listaPacientes.add("Todos los pacientes");
                    // Agregar el resto de los pacientes
                    listaPacientes.addAll(pacientes);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            Pdf.this,
                            R.layout.spinner_item,
                            listaPacientes
                    );
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                    spinnerPacientes.setAdapter(adapter);

                    // Manejar selección del paciente
                    spinnerPacientes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String seleccion = listaPacientes.get(position);
                            if (seleccion.equals("Todos los pacientes")) {
                                pacienteSeleccionado = "";
                            } else {
                                pacienteSeleccionado = seleccion;
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            pacienteSeleccionado = "";
                        }
                    });
                });
            }
        });
    }
    private void resetearFiltros() {
        runOnUiThread(() -> {
            // Restablecer la fecha
            fechaSeleccionada = "";
            textViewFechaSeleccionada.setText("Seleccionar fecha");
            datePicker.setVisibility(View.GONE);

            // Restablecer el paciente seleccionado
            pacienteSeleccionado = "";
            if (spinnerPacientes.getAdapter() != null) {
                spinnerPacientes.setSelection(0); // Volver a "Todos"
            }
            spinnerPacientes.setVisibility(View.GONE);

            // Habilitar el botón nuevamente
            btnGenerarPDF.setEnabled(true);
        });
    }


}