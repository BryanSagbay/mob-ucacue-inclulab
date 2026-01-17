package ucacue.edu.udipsai.Services;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ucacue.edu.udipsai.Model.Patient;
import ucacue.edu.udipsai.R;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
    private List<Patient> listaPacientes;
    private OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(Patient patient);
    }

    public ListAdapter(List<Patient> listaPacientes, OnPatientClickListener listener) {
        this.listaPacientes = listaPacientes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Patient paciente = listaPacientes.get(position);
        holder.txtNombre.setText(paciente.getNombre() + " " + paciente.getApellido());
        holder.itemView.setOnClickListener(v -> listener.onPatientClick(paciente));
    }

    @Override
    public int getItemCount() {
        return listaPacientes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombrePaciente);
        }
    }
}
