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
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import ucacue.edu.udipsai.R;
import ucacue.edu.udipsai.Services.SerialListener;
import ucacue.edu.udipsai.Services.SerialService;

public class test_Bennett extends AppCompatActivity implements SerialListener, ServiceConnection {
    private SerialService service;
    private StringBuilder fullReceivedData = new StringBuilder();
    private boolean isBound = false;
    private TextView receivedDataText, tvErrores, tvTiempoEjecucion, tvTituloDatos, tvExtraDato;
    private CardView cardEspera, cardDatos, cardExtraDato;
    private ImageView gifStatusResultado, gifStatusB;
    private ImageButton backButton;
    private FloatingActionButton playButton, resetButton, saveButton;
    private Button sendButton1, sendButton2, sendButton3;
    private int currentStep = 0;
    private FirebaseFirestore db;

    // Variables para almacenar datos de cada fase
    private String Tiempo_de_Ejecución_Fase_1 = "0", Resultados_Fase_1 = "0";
    private String Tiempo_de_Ejecución_Fase_2 = "0", Resultados_Fase_2 = "0";
    private String Tiempo_de_Ejecución_Fase_3 = "0", Resultados_Fase_3 = "0", Tiempo_Total = "-";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_bennett);

        // Inicialización de vistas
        receivedDataText = findViewById(R.id.text_datosB);
        gifStatusB = findViewById(R.id.gif_statusB);
        cardEspera = findViewById(R.id.card_esperaB);
        cardDatos = findViewById(R.id.card_datosB);
        gifStatusResultado = findViewById(R.id.gif_status_resultadoB);
        tvErrores = findViewById(R.id.tv_errores);
        tvTiempoEjecucion = findViewById(R.id.tv_tiempo_ejecucion);
        tvTituloDatos = findViewById(R.id.text_titulo_datosB);
        tvExtraDato = findViewById(R.id.tv_extra_dato);
        cardExtraDato = findViewById(R.id.card_extra_dato);
        sendButton1 = findViewById(R.id.button_enviar_m1B);
        sendButton2 = findViewById(R.id.button_enviar_m2B);
        sendButton3 = findViewById(R.id.button_enviar_m3B);
        backButton = findViewById(R.id.button_regresarB);
        playButton = findViewById(R.id.button_playB);
        resetButton = findViewById(R.id.button_resetB);
        saveButton = findViewById(R.id.button_saveB);

        // Inicialización de valores
        sendButton1.setEnabled(false);
        sendButton2.setEnabled(false);
        sendButton3.setEnabled(false);
        resetButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        cardDatos.setVisibility(View.GONE);
        loadGif(gifStatusB, R.drawable.ic_reloj_de_arena);
        receivedDataText.setText("Esperando, presione Comenzar...");
        tvTituloDatos.setText("Esperando datos...");
        tvErrores.setText("-");
        tvTiempoEjecucion.setText("- seg");

        // Iniciar y vincular servicio Bluetooth
        Intent intent = new Intent(this, SerialService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

        // Listeners de botones
        playButton.setOnClickListener(v -> {
            sendButton1.setEnabled(true);
            resetButton.setVisibility(View.VISIBLE);
            currentStep = 1;
        });

        sendButton1.setOnClickListener(v -> {
            sendData("M1");
            sendButton1.setEnabled(false);
            currentStep = 2;
            saveButton.setVisibility(View.GONE);
            loadGif(gifStatusB, R.drawable.ic_dibujo);
            receivedDataText.setText("Ejecutando el Test...");
        });

        sendButton2.setOnClickListener(v -> {
            sendData("M2");
            sendButton2.setEnabled(false);
            currentStep = 4;
            saveButton.setVisibility(View.GONE);
            loadGif(gifStatusB, R.drawable.ic_dibujo);
            receivedDataText.setText("Ejecutando el Test...");
        });

        sendButton3.setOnClickListener(v -> {
            sendData("M3");
            sendButton3.setEnabled(false);
            currentStep = 6;
            saveButton.setVisibility(View.GONE);
        });

        resetButton.setOnClickListener(v -> {
            sendData("S");
            resetDatos();
            resetUI();
        });

        backButton.setOnClickListener(v -> {
            disconnectBluetooth();
            Intent homeIntent = new Intent(test_Bennett.this, HomeTest.class);
            startActivity(homeIntent);
            finish();
        });

        // Inicialización de Firestore
        db = FirebaseFirestore.getInstance();

        // Configurar botón de guardado
        saveButton.setOnClickListener(v -> guardarDatos());
    }

    // Método para enviar datos al dispositivo Bluetooth
    private void sendData(String message) {
        if (service != null) {
            try {
                service.write(message.getBytes("UTF-8"));
                Toast.makeText(this, "Mensaje enviado: " + message, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Error al enviar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No hay conexión Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para recibir y procesar datos del dispositivo Bluetooth
    @Override
    public void onSerialRead(ArrayDeque<byte[]> datas) {
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
                receivedString = receivedString.replace("\n", ",");
                String[] values = receivedString.split(",");

                if (values.length >= 2) {
                    saveButton.setVisibility(View.GONE);

                    switch (currentStep) {
                        case 2: // Respuesta M1
                            Tiempo_de_Ejecución_Fase_1 = values[0];
                            Resultados_Fase_1 = formatErrors(values[1]);
                            break;
                        case 4: // Respuesta M2
                            Tiempo_de_Ejecución_Fase_2 = values[0];
                            Resultados_Fase_2 = formatErrors(values[1]);
                            break;
                        case 6: // Respuesta M3
                            Tiempo_de_Ejecución_Fase_3 = values[0];
                            Resultados_Fase_3 = formatErrors(values[1]);
                            if (values.length == 3) {
                                Tiempo_Total = values[2];
                            }
                            saveButton.setVisibility(View.VISIBLE);
                            break;
                    }

                    // Actualizar UI
                    updateUI(values);
                    fullReceivedData.setLength(0);
                }
            }
        });
    }

    // Método para actualizar la interfaz de usuario
    private void updateUI(String[] values) {
        cardEspera.setVisibility(View.GONE);
        cardDatos.setVisibility(View.VISIBLE);
        tvTituloDatos.setText("Resultados del Test");

        String nuevosDatos = "Tiempo: " + values[0] + " seg | Errores: " + formatErrors(values[1]);
        if (values.length == 3) {
            nuevosDatos += " | Extra: " + values[2];
        }

        String datosAnteriores = receivedDataText.getText().toString();
        receivedDataText.setText(datosAnteriores + "\n" + nuevosDatos);

        tvTiempoEjecucion.setText(values[0] + " seg");
        tvErrores.setText(formatErrors(values[1]));

        if (values.length == 3) {
            tvExtraDato.setText(values[2]);
            cardExtraDato.setVisibility(View.VISIBLE);
        } else {
            cardExtraDato.setVisibility(View.GONE);
        }

        loadGif(gifStatusResultado, R.drawable.ic_check);

        switch (currentStep) {
            case 2:
                sendButton2.setEnabled(true);
                currentStep = 3;
                break;
            case 4:
                sendButton3.setEnabled(true);
                currentStep = 5;
                break;
            case 6:
                break;
        }
    }

    // Método para formatear errores
    private String formatErrors(String errores) {
        StringBuilder erroresFormatted = new StringBuilder();
        for (char c : errores.toCharArray()) {
            if (c == '1') {
                erroresFormatted.append("❌");
            } else if (c == '0') {
                erroresFormatted.append("✅");
            } else {
                erroresFormatted.append(c);
            }
        }
        return erroresFormatted.toString();
    }

    // Método para guardar datos en Firestore
    private void guardarDatos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String correoUsuario = (user != null) ? user.getEmail() : "No autenticado";

        // Obtener el nombre del paciente
        SharedPreferences preferences = getSharedPreferences("PatientPrefs", MODE_PRIVATE);
        String nombrePaciente = preferences.getString("patient_name", "Paciente Desconocido");

        Map<String, Object> datos = new HashMap<>();
        datos.put("timestamp", System.currentTimeMillis());
        datos.put("nombrePaciente", nombrePaciente);
        datos.put("correoUsuario", correoUsuario);
        datos.put("Tiempo de Ejecución Fase 1", Tiempo_de_Ejecución_Fase_1);
        datos.put("Resultados Fase 1", Resultados_Fase_1);
        datos.put("Tiempo de Ejecución Fase 2", currentStep >= 4 ? Tiempo_de_Ejecución_Fase_2 : "0");
        datos.put("Resultados Fase 2", currentStep >= 4 ? Resultados_Fase_2 : "0");
        datos.put("Tiempo de Ejecución Fase 3", currentStep >= 6 ? Tiempo_de_Ejecución_Fase_3 : "0");
        datos.put("Resultados Fase 3", currentStep >= 6 ? Resultados_Fase_3 : "0");
        datos.put("Tiempo Total", currentStep >= 6 ? Tiempo_Total : "-");

        db.collection("testResultados")
                .add(datos)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Datos guardados", Toast.LENGTH_SHORT).show();
                    resetDatos();
                    resetUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Método para resetear datos
    private void resetDatos() {
        Tiempo_de_Ejecución_Fase_1 = "0";
        Resultados_Fase_1 = "0";
        Tiempo_de_Ejecución_Fase_2 = "0";
        Resultados_Fase_2 = "0";
        Tiempo_de_Ejecución_Fase_3 = "0";
        Resultados_Fase_3 = "0";
        Tiempo_Total = "-";
        currentStep = 0;
        saveButton.setVisibility(View.GONE);
    }

    // Método para resetear la interfaz de usuario
    private void resetUI() {
        sendButton1.setEnabled(false);
        sendButton2.setEnabled(false);
        sendButton3.setEnabled(false);
        resetButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        receivedDataText.setText("Esperando, presione Comenzar...");
        tvTituloDatos.setText("Esperando datos...");
        tvErrores.setText("-");
        tvTiempoEjecucion.setText("- seg");
        loadGif(gifStatusB, R.drawable.ic_reloj_de_arena);
        cardEspera.setVisibility(View.VISIBLE);
        cardDatos.setVisibility(View.GONE);
    }

    // Método para cargar GIFs
    private void loadGif(ImageView imageView, int gifResource) {
        Glide.with(this).asGif().load(gifResource).into(imageView);
    }

    // Métodos de ServiceConnection y SerialListener
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

    // Método para desconectar Bluetooth
    private void disconnectBluetooth() {
        if (service != null) {
            service.disconnect();
        }
        if (isBound) {
            unbindService(this);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBluetooth();
    }
}