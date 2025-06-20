package com.example.outfitmatch;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outfitmatch.adaptador.AdaptadorDosPiezas;
import com.example.outfitmatch.modelo.entidad.Prenda;
import com.example.outfitmatch.modelo.negocio.GestorPrenda;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DosPiezas extends AppCompatActivity {

    private RecyclerView rvCamisas, rvPantalones, rvZapatos, rvChaquetas, rvAccesorios;
    private Prenda camisaSeleccionada, pantalonSeleccionado, zapatoSeleccionado, chaquetaSeleccionada, accesorioSeleccionado;
    private FirebaseFirestore db;
    private String userId;
    private Button btnGuardarOutfit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dos_piezas);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rvCamisas = findViewById(R.id.recyclerShirts);
        rvPantalones = findViewById(R.id.recyclerPants);
        rvZapatos = findViewById(R.id.recyclerShoes);
        rvChaquetas = findViewById(R.id.recyclerJackets);
        rvAccesorios = findViewById(R.id.recyclerAccessories);
        btnGuardarOutfit = findViewById(R.id.btnGuardarOutfit);

        configurarRecycler(rvCamisas);
        configurarRecycler(rvPantalones);
        configurarRecycler(rvZapatos);
        configurarRecycler(rvChaquetas);
        configurarRecycler(rvAccesorios);

        cargarPrendas();

        btnGuardarOutfit.setOnClickListener(v -> guardarOutfit());
    }

    private void configurarRecycler(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void cargarPrendas() {
        GestorPrenda.getInstance().obtenerPrendasSoloFirebase(userId, new GestorPrenda.OnTotalPrendasListener() {
            @Override
            public void onTotalPrendas(int total, List<Prenda> prendas) {
                List<Prenda> camisas = new ArrayList<>();
                List<Prenda> pantalones = new ArrayList<>();
                List<Prenda> zapatos = new ArrayList<>();
                List<Prenda> chaquetas = new ArrayList<>();
                List<Prenda> accesorios = new ArrayList<>();

                for (Prenda p : prendas) {
                    if (p.getTipo() == null) continue;
                    String tipo = p.getTipo().toLowerCase().trim();
                    Log.d("DosPiezas", "Prenda tipo: '" + p.getTipo() + "'");

                    switch (tipo) {
                        // Camisas
                        case "camisa":
                        case "shirt":
                        case "shirts":
                            camisas.add(p);
                            break;

                        // Pantalones
                        case "pantalón":
                        case "pantalon":
                        case "pants":
                        case "trousers":
                            pantalones.add(p);
                            break;

                        // Zapatos
                        case "zapato":
                        case "zapatos":
                        case "shoes":
                            zapatos.add(p);
                            break;

                        // Chaquetas
                        case "chaqueta":
                        case "jacket":
                        case "jackets":
                        case "accessories/jackets":
                            chaquetas.add(p);
                            break;

                        // Accesorios
                        case "accesorio":
                        case "accesorios":
                        case "accessory":
                        case "accessories":
                            accesorios.add(p);
                            break;

                        default:
                            Log.w("DosPiezas", "Tipo no reconocido: '" + p.getTipo() + "'");
                            break;
                    }
                }

                rvCamisas.setAdapter(new AdaptadorDosPiezas(DosPiezas.this, camisas, prenda -> camisaSeleccionada = prenda));
                rvPantalones.setAdapter(new AdaptadorDosPiezas(DosPiezas.this, pantalones, prenda -> pantalonSeleccionado = prenda));
                rvZapatos.setAdapter(new AdaptadorDosPiezas(DosPiezas.this, zapatos, prenda -> zapatoSeleccionado = prenda));
                rvChaquetas.setAdapter(new AdaptadorDosPiezas(DosPiezas.this, chaquetas, prenda -> chaquetaSeleccionada = prenda));
                rvAccesorios.setAdapter(new AdaptadorDosPiezas(DosPiezas.this, accesorios, prenda -> accesorioSeleccionado = prenda));
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(DosPiezas.this, "Error cargando prendas: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("DosPiezas", "Error al cargar prendas", e);
            }
        });
    }

    private void guardarOutfit() {
        if (camisaSeleccionada == null || pantalonSeleccionado == null || zapatoSeleccionado == null) {
            Toast.makeText(this, "Selecciona al menos camisa, pantalón y zapatos.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Prenda> outfit = new ArrayList<>();
        outfit.add(camisaSeleccionada);
        outfit.add(pantalonSeleccionado);
        outfit.add(zapatoSeleccionado);
        if (chaquetaSeleccionada != null) outfit.add(chaquetaSeleccionada);
        if (accesorioSeleccionado != null) outfit.add(accesorioSeleccionado);

        Outfit nuevoOutfit = new Outfit(outfit);

        // Guardar en historial
        db.collection("users").document(userId).collection("saved_outfits")
                .add(nuevoOutfit)
                .addOnSuccessListener(docRef -> Log.d("DosPiezas", "Outfit guardado en historial"))
                .addOnFailureListener(e -> Log.e("DosPiezas", "Error guardando historial", e));

        // Guardar como outfit actual (sobrescribe el anterior)
        db.collection("users").document(userId)
                .collection("current_outfit").document("outfit_actual")
                .set(nuevoOutfit)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(DosPiezas.this, "Outfit actual actualizado", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(DosPiezas.this, Outfits.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(DosPiezas.this, "Error al guardar outfit actual", Toast.LENGTH_SHORT).show());
    }


    public static class Outfit {
        public List<Prenda> prendas;
        public Outfit(List<Prenda> prendas) {
            this.prendas = prendas;
        }
    }


}
