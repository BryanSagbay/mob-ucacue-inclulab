package ucacue.edu.udipsai.UI.test;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ucacue.edu.udipsai.R;
import ucacue.edu.udipsai.Services.SerialListener;
import ucacue.edu.udipsai.Services.SerialService;

public class test_Riel extends AppCompatActivity implements SerialListener, ServiceConnection {
    private SerialService service;
    private StringBuilder fullReceivedData = new StringBuilder();
    private boolean isBound = false;
    private TextView receivedDataText, tvErrores, tvTiempoEjecucion, tvTituloDatos;
    private CardView cardEspera, cardDatos;
    private ImageView gifStatusResultado, gifStatusR;
    private Button sendButton;
    private ImageButton backButton;
    private FloatingActionButton playButton, resetButton, saveButton;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_riel);

        receivedDataText = findViewById(R.id.text_datosR);
        gifStatusR = findViewById(R.id.gif_statusR);
        cardEspera = findViewById(R.id.card_esperaR);
        cardDatos = findViewById(R.id.card_datosR);
        gifStatusResultado = findViewById(R.id.gif_status_resultadoR);
        tvErrores = findViewById(R.id.tv_errores);
        tvTiempoEjecucion = findViewById(R.id.tv_tiempo_ejecucion);
        tvTituloDatos = findViewById(R.id.text_titulo_datosR);
        sendButton = findViewById(R.id.button_enviar_m1R);
        backButton = findViewById(R.id.button_regresarR);
        playButton = findViewById(R.id.button_playR);
        resetButton = findViewById(R.id.button_resetR);
        saveButton = findViewById(R.id.button_saveR);


        // Inicialmente, el botón "Enviar M1" está deshabilitado y el de reinicio está oculto
        sendButton.setEnabled(false);
        resetButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);

        // Inicialización de valores iniciales
        cardDatos.setVisibility(View.GONE);
        loadGif(gifStatusR, R.drawable.ic_reloj_de_arena);
        receivedDataText.setText("Esperando, presione Comenzar...");
        tvTituloDatos.setText("Esperando datos...");
        tvErrores.setText("-");
        tvTiempoEjecucion.setText("- seg");

        // Iniciar y vincular servicio Bluetooth
        Intent intent = new Intent(this, SerialService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

        // Botón Play: Habilita "Enviar M1" y muestra "Reinicio"
        playButton.setOnClickListener(v -> {
            sendButton.setEnabled(true);
            resetButton.setVisibility(View.VISIBLE);
            saveButton.setVisibility(View.GONE);

        });

        // Botón Enviar M1: Enviar comando y deshabilitar
        sendButton.setOnClickListener(v -> {
            sendData("M1");
            sendButton.setEnabled(false);
            loadGif(gifStatusR, R.drawable.ic_dibujo);
            receivedDataText.setText("Ejecutando el Test...");
            saveButton.setVisibility(View.GONE);
        });

        // Botón de reinicio: Enviar comando "S" y limpiar datos
        resetButton.setOnClickListener(v -> {
            sendData("S");
            receivedDataText.setText("Esperando, presione Comenzar...");
            sendButton.setEnabled(false);
            resetButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.GONE);
            tvTituloDatos.setText("Esperando datos...");
            tvErrores.setText("-");
            tvTiempoEjecucion.setText("- seg");
            resetButton.setVisibility(View.GONE);
            loadGif(gifStatusR, R.drawable.ic_reloj_de_arena);
            cardEspera.setVisibility(View.VISIBLE);
            cardDatos.setVisibility(View.GONE);
        });

        // Botón para regresar y desconectar Bluetooth
        backButton.setOnClickListener(v -> {
            disconnectBluetooth();
            Intent homeIntent = new Intent(test_Riel.this, HomeTest.class);
            startActivity(homeIntent);
            finish();
        });

        db = FirebaseFirestore.getInstance();
        // Botón para guardar los datos en firestore
        saveButton.setOnClickListener(v -> guardarDatos());

    }

    /**
     * Enviar datos al dispositivo Bluetooth
     */
    private void sendData(String message) {
        if (service != null) {
            try {
                service.write(message.getBytes("UTF-8")); // Compatible con API 18+
                Toast.makeText(this, "Mensaje enviado: " + message, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Error al enviar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No hay conexión Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Recibir y mostrar datos del dispositivo Bluetooth
     */
    @Override
    public void onSerialRead(java.util.ArrayDeque<byte[]> datas) {
        runOnUiThread(() -> {
            for (byte[] data : datas) {
                try {
                    fullReceivedData.append(new String(data, "UTF-8"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (fullReceivedData.length() > 0) {
                String receivedString = fullReceivedData.toString().trim();
                String[] values = receivedString.split(",");

                if (values.length == 2) {
                    String errores = values[0];
                    String tiempoEjecucion = values[1];

                    cardEspera.setVisibility(View.GONE);
                    cardDatos.setVisibility(View.VISIBLE);

                    tvTituloDatos.setText("Resultados del Test de Sistema de Motricidad");
                    tvErrores.setText(errores);
                    tvTiempoEjecucion.setText(tiempoEjecucion + " seg");

                    loadGif(gifStatusResultado, R.drawable.ic_check);

                    // 💡 MOSTRAR EL BOTÓN GUARDAR AL RECIBIR DATOS DEL BLUETOOTH
                    saveButton.setVisibility(View.VISIBLE);

                    fullReceivedData.setLength(0); // Limpiar buffer de datos
                }
            }
        });
    }

    // Cargar GIFs
    private void loadGif(ImageView imageView, int gifResource) {
        Glide.with(this).asGif().load(gifResource).into(imageView);
    }


    /**
     * Metodo para guardar datos en firestore
     */
    private void guardarDatos() {
        String Errores = tvErrores.getText().toString();
        String Tiempo_de_Ejecucion = tvTiempoEjecucion.getText().toString();
        String Titulo = tvTituloDatos.getText().toString();

        // Verificar si hay datos antes de guardar
        if (Errores.equals("-") || Tiempo_de_Ejecucion.equals("- seg")) {
            Toast.makeText(this, "No hay datos para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el nombre del paciente
        SharedPreferences preferences = getSharedPreferences("PatientPrefs", MODE_PRIVATE);
        String nombrePaciente = preferences.getString("patient_name", "Paciente Desconocido");

        // Obtener el correo del usuario autenticado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String correoUsuario = (user != null) ? user.getEmail() : "No autenticado";

        // Crear el objeto para guardar en Firestore
        Map<String, Object> datos = new HashMap<>();
        datos.put("timestamp", System.currentTimeMillis());
        datos.put("nombrePaciente", nombrePaciente);
        datos.put("correoUsuario", correoUsuario);
        datos.put("Título", Titulo);
        datos.put("Errores", Errores);
        datos.put("Tiempo de Ejecución", Tiempo_de_Ejecucion);


        // Guardar en Firestore en la colección "testResultados"
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("testResultados")
                .add(datos)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Manejo de conexión y errores Bluetooth
     */
    @Override
    public void onSerialConnect() {
        runOnUiThread(() -> Toast.makeText(this, "Conexión Bluetooth establecida", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onSerialConnectError(Exception e) {
        runOnUiThread(() -> Toast.makeText(this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onSerialRead(byte[] data) {
    }

    @Override
    public void onSerialIoError(Exception e) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Error de comunicación", Toast.LENGTH_SHORT).show();
            disconnectBluetooth();
        });
    }

    /**
     * Cerrar conexión Bluetooth
     */
    private void disconnectBluetooth() {
        if (service != null) {
            service.disconnect();
        }
        if (isBound) {
            unbindService(this);
            isBound = false;
        }
    }

    /**
     * Métodos para el ServiceConnection
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        isBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
        isBound = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBluetooth();
    }
}