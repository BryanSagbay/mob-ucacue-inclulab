package ucacue.edu.udipsai.Services;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ucacue.edu.udipsai.Model.Patient;
import ucacue.edu.udipsai.R;
import ucacue.edu.udipsai.UI.patient.PatientHome;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PacienteViewHolder> {
    private List<Patient> listaPacientes;
    private Context context;
    private FirebaseFirestore db;

    // Referencias a los overlays de la actividad principal
    private FrameLayout loadingOverlay;
    private FrameLayout errorOverlay;
    private TextView errorMessage;
    private ImageView loadingGif, errorloadingGif;

    public PatientAdapter(List<Patient> listaPacientes, Context context) {
        this.listaPacientes = listaPacientes;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();

        if (context instanceof PatientHome) {
            PatientHome activity = (PatientHome) context;
            loadingOverlay = activity.findViewById(R.id.loadingOverlay);
            errorOverlay = activity.findViewById(R.id.errorOverlay);
            errorMessage = activity.findViewById(R.id.errorMessage);
            loadingGif = activity.findViewById(R.id.loadingGif);
            errorloadingGif = activity.findViewById(R.id.errorloadingGif);

            Glide.with(activity).asGif().load(R.drawable.ic_check).into(loadingGif);
            Glide.with(activity).asGif().load(R.drawable.ic_error_login).into(errorloadingGif);
        }
    }

    @NonNull
    @Override
    public PacienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listado_pacientes, parent, false);
        return new PacienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PacienteViewHolder holder, int position) {
        Patient paciente = listaPacientes.get(position);

        holder.txtNombre.setText(paciente.getNombre() + " " + paciente.getApellido());
        holder.txtTelefono.setText("Tel: " + paciente.getTelefono());

        // Formatear y mostrar la fecha de registro
        long fechaMillis = paciente.getFechaRegistro();
        Date fecha = new Date(fechaMillis);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String fechaFormateada = sdf.format(fecha);
        holder.txtFecha.setText("Registrado: " + fechaFormateada);

        // Botón editar
        holder.btnEditar.setOnClickListener(v -> mostrarDialogoEditar(paciente));

        // Botón eliminar
        holder.btnEliminar.setOnClickListener(v -> eliminarPaciente(paciente));
    }

    @Override
    public int getItemCount() {
        return listaPacientes.size();
    }

    /**
     * ViewHolder de cada paciente
     */
    public static class PacienteViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtTelefono, txtFecha;
        ImageButton btnEditar, btnEliminar;

        public PacienteViewHolder(View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombre);
            txtTelefono = itemView.findViewById(R.id.txtTelefono);
            txtFecha = itemView.findViewById(R.id.txtFecha);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }

    /**
     * Mostrar diálogo para editar
     */
    private void mostrarDialogoEditar(Patient paciente) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.modal_paciente, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText edtNombre = dialogView.findViewById(R.id.edtNombre);
        EditText edtApellido = dialogView.findViewById(R.id.edtApellido);
        EditText edtTelefono = dialogView.findViewById(R.id.edtTelefono);
        EditText edtDireccion = dialogView.findViewById(R.id.edtDireccion);
        EditText edtEdad = dialogView.findViewById(R.id.edtEdad);
        Button btnActualizar = dialogView.findViewById(R.id.btnAgregar);

        edtNombre.setText(paciente.getNombre());
        edtApellido.setText(paciente.getApellido());
        edtTelefono.setText(paciente.getTelefono());
        edtDireccion.setText(paciente.getDireccion());
        edtEdad.setText(String.valueOf(paciente.getEdad()));
        btnActualizar.setText("Actualizar");

        btnActualizar.setOnClickListener(v -> {
            String nombre = edtNombre.getText().toString().trim();
            String apellido = edtApellido.getText().toString().trim();
            String telefono = edtTelefono.getText().toString().trim();
            String direccion = edtDireccion.getText().toString().trim();
            String edadStr = edtEdad.getText().toString().trim();

            if (nombre.isEmpty() || apellido.isEmpty() || telefono.isEmpty() || direccion.isEmpty() || edadStr.isEmpty()) {
                mostrarAlertaError("Todos los campos son obligatorios");
                return;
            }

            int edad = Integer.parseInt(edadStr);
            dialog.dismiss();
            mostrarAlertaCargando();

            db.collection("pacientes").document(String.valueOf(paciente.getId()))
                    .update("nombre", nombre, "apellido", apellido, "telefono", telefono, "direccion", direccion, "edad", edad)
                    .addOnSuccessListener(aVoid -> {
                        mostrarAlertaCorrecto("Paciente actualizado con éxito");
                        notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> mostrarAlertaError("Error al actualizar: " + e.getMessage()));
        });
    }

    /**
     * Confirmación y eliminación
     */
    private void eliminarPaciente(Patient paciente) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.alert_patient, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        Button btnConfirmar = dialogView.findViewById(R.id.btnConfirmarEliminar);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelarEliminar);

        btnConfirmar.setOnClickListener(v -> {
            db.collection("pacientes").document(String.valueOf(paciente.getId()))
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        mostrarAlertaCorrecto("Paciente eliminado con éxito");
                        listaPacientes.remove(paciente);
                        notifyDataSetChanged();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> mostrarAlertaError("Error al eliminar paciente"));
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
    }

    // Métodos para mostrar overlays
    private void mostrarAlertaCargando() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
            loadingGif.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarAlertaCorrecto(String mensaje) {
        if (context instanceof PatientHome) {
            ((PatientHome) context).mostrarAlertaCorrecto(mensaje);
        }
    }

    private void mostrarAlertaError(String mensaje) {
        if (context instanceof PatientHome) {
            ((PatientHome) context).mostrarAlertaError(mensaje);
        }
    }
}
