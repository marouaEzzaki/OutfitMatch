package com.example.outfitmatch;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outfitmatch.adaptador.AdaptadorFavorito;
import com.example.outfitmatch.modelo.entidad.Prenda;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import me.ibrahimsn.lib.SmoothBottomBar;

public class Favorito extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdaptadorFavorito adapter;
    private List<Prenda> savedOutfits = new ArrayList<>();

    private FirebaseFirestore db;
    private String userId;

    private SmoothBottomBar bottomBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorito);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = findViewById(R.id.recyclerViewClothes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar adaptador con la lista vacía
        adapter = new AdaptadorFavorito(this, savedOutfits, userId, () -> {
            recyclerView.setVisibility(View.GONE); // Ocultar RecyclerView
        });

        recyclerView.setAdapter(adapter);

        loadSavedOutfitsFromFirestore();

        bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setOnItemSelectedListener(new Function1<Integer, Unit>() {
            @Override
            public Unit invoke(Integer index) {
                if (index == 4) return Unit.INSTANCE;

                Class<?> destination = null;
                switch (index) {
                    case 0:
                        destination = Home.class;
                        break;
                    case 1:
                        destination = Clothes.class;
                        break;
                    case 2:
                        destination = AddClothesAlbum.class;
                        break;
                    case 3:
                        destination = Perfil.class;
                        break;
                    case 4:
                        destination = GenerarOutfit.class;
                        break;
                }

                if (destination != null) {
                    startActivity(new Intent(getApplicationContext(), destination));
                    overridePendingTransition(0, 0);
                }
                return Unit.INSTANCE;
            }
        });
    }

    private void loadSavedOutfitsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).collection("favoritos")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Error al escuchar cambios", e);
                        return;
                    }

                    if (querySnapshot != null) {
                        savedOutfits.clear();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            String documentId = document.getId();
                            String imagenUrl = document.getString("imagenUrl");
                            String talla = document.getString("talla");
                            String material = document.getString("material");
                            String color = document.getString("color");
                            String tipo = document.getString("tipo");

                            // Filtrar para no incluir "shoe" ni "accesorio"
                            if (tipo != null && !tipo.equalsIgnoreCase("shoes") && !tipo.equalsIgnoreCase("accessories")) {
                                Prenda prenda = new Prenda(talla, material, color, tipo);
                                prenda.setImagenUrl(imagenUrl);
                                prenda.setId(documentId);

                                savedOutfits.add(prenda);
                            }
                        }

                        // Ocultar RecyclerView si no hay favoritos
                        if (savedOutfits.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                        Log.i("test", "loadSavedOutfitsFromFirestore: ");
                        adapter.notifyDataSetChanged();
                    }
                });
    }



}
