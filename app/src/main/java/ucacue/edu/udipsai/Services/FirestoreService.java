package ucacue.edu.udipsai.Services;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QuerySnapshot;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class FirestoreService {
    private final FirebaseFirestore db;

    public FirestoreService() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Obtiene la lista de pacientes registrados por un usuario específico.
     */
    public void getPacientesPorUsuario(String usuarioEmail, PacientesCallback callback) {
        db.collection("pacientes")
                .whereEqualTo("correoUsuario", usuarioEmail) // Filtrar por correo del usuario autenticado
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> pacientes = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String nombre = doc.getString("nombre");
                        String apellido = doc.getString("apellido");
                        if (nombre != null && apellido != null) {
                            pacientes.add(nombre + " " + apellido);
                        }
                    }
                    callback.onCallback(pacientes);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreService", "Error al obtener pacientes", e);
                    callback.onCallback(Collections.emptyList());
                });
    }

    /**
     * Obtiene datos de la colección "testResultados" filtrados por el usuario autenticado y paciente.
     */
    public List<Map<String, Object>> getAllDataByEmailDateAndPaciente(
            String email,
            String selectedDate,
            String pacienteNombre
    ) {
        List<Map<String, Object>> allData = new ArrayList<>();

        try {
            CollectionReference collectionRef = db.collection("testResultados");
            Task<QuerySnapshot> queryTask;

            // Verificar si cada parámetro está realmente vacío o es nulo
            boolean fechaVacia = (selectedDate == null || selectedDate.isEmpty());
            boolean pacienteVacio = (pacienteNombre == null || pacienteNombre.isEmpty());

            Log.d("FirestoreService", "Filtros: Email=" + email + ", Fecha=" +
                    (fechaVacia ? "vacía" : selectedDate) + ", Paciente=" +
                    (pacienteVacio ? "vacío" : pacienteNombre));

            if (fechaVacia && pacienteVacio) {
                // Caso 1: No hay filtros → Obtener todos los datos del usuario
                queryTask = collectionRef.whereEqualTo("correoUsuario", email).get();
            } else if (fechaVacia) {
                // Caso 2: Filtrar solo por paciente
                queryTask = collectionRef
                        .whereEqualTo("correoUsuario", email)
                        .whereEqualTo("nombrePaciente", pacienteNombre)
                        .get();
            } else if (pacienteVacio) {
                // Caso 3: Filtrar solo por fecha (pero obteniendo todos los documentos del usuario)
                queryTask = collectionRef.whereEqualTo("correoUsuario", email).get();
            } else {
                // Caso 4: Filtrar por paciente y por fecha
                queryTask = collectionRef
                        .whereEqualTo("correoUsuario", email)
                        .whereEqualTo("nombrePaciente", pacienteNombre)
                        .get();
            }

            QuerySnapshot querySnapshot = Tasks.await(queryTask);
            Log.d("FirestoreService", "Documentos recuperados: " + querySnapshot.getDocuments().size());

            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                if (document.exists()) {
                    Map<String, Object> data = document.getData();

                    if (data != null && data.containsKey("timestamp")) {
                        long timestamp = (long) data.get("timestamp");
                        String fechaDocumento = convertirTimestampAFecha(timestamp);

                        // Filtrar por fecha solo si hay una fecha seleccionada
                        boolean incluirDocumento = fechaVacia || fechaDocumento.equals(selectedDate);

                        if (incluirDocumento) {
                            Log.d("FirestoreService", "Documento incluido: " + document.getId() +
                                    " con fecha " + fechaDocumento + " y paciente " +
                                    data.get("nombrePaciente"));
                            allData.add(data);
                        }
                    }
                }
            }

            Log.d("FirestoreService", "Datos filtrados: " + allData.size());
        } catch (ExecutionException | InterruptedException e) {
            Log.e("FirestoreService", "Error al obtener datos de Firestore", e);
        }

        return allData;
    }

    /**
     * Convierte un timestamp (milisegundos) a una fecha en formato yyyy-M-d.
     */
    private String convertirTimestampAFecha(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Interfaz para manejar la respuesta asíncrona de la lista de pacientes.
     */
    public interface PacientesCallback {
        void onCallback(List<String> pacientes);
    }
}