package com.example.safecity.ui.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safecity.R;
import com.example.safecity.model.Comment;
import com.example.safecity.model.Utilisateur;
import com.example.safecity.ui.adapters.CommentAdapter;
import com.example.safecity.utils.FirestoreRepository;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment optimisé de type BottomSheet pour l'espace de discussion.
 * Fusion des versions : Sécurités Firestore (ID incident) + Forçage de l'état étendu.
 */
public class CommentFragment extends BottomSheetDialogFragment {

    private static final String ARG_INCIDENT_ID = "incident_id";

    private String incidentId;
    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private EditText etInput;
    private FloatingActionButton btnSend;
    private TextView tvEmpty;

    private FirestoreRepository repo;
    private ListenerRegistration listenerRegistration;
    private Utilisateur currentUserData;

    /**
     * Méthode statique pour créer une instance du fragment avec l'ID de l'incident concerné.
     */
    public static CommentFragment newInstance(String incidentId) {
        CommentFragment fragment = new CommentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INCIDENT_ID, incidentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comment, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Force l'ouverture complète du volet dès le début
        Dialog dialog = getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Liaison des composants UI
        recyclerView = view.findViewById(R.id.recycler_comments);
        etInput = view.findViewById(R.id.et_comment_input);
        btnSend = view.findViewById(R.id.btn_send_comment);
        tvEmpty = view.findViewById(R.id.tv_no_comments);

        // Configuration de la liste
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CommentAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        repo = new FirestoreRepository();

        if (getArguments() != null) {
            incidentId = getArguments().getString(ARG_INCIDENT_ID);
        }

        // Sécurité : Si l'ID est nul, on ferme le fragment pour éviter les erreurs Firestore
        if (incidentId == null) {
            dismiss();
            return;
        }

        loadCurrentUserInfo();
        startListeningComments();

        // Listener d'envoi
        btnSend.setOnClickListener(v -> postComment());
    }

    private void loadCurrentUserInfo() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            repo.getUser(uid, new FirestoreRepository.OnUserLoadedListener() {
                @Override
                public void onUserLoaded(Utilisateur utilisateur) {
                    if (isAdded()) {
                        currentUserData = utilisateur;
                    }
                }
                @Override
                public void onError(Exception e) {}
            });
        }
    }

    /**
     * Écoute en temps réel les nouveaux commentaires sur Firestore.
     */
    private void startListeningComments() {
        if (incidentId == null) return;

        listenerRegistration = repo.getCommentsRealtime(incidentId, new FirestoreRepository.OnCommentsLoadedListener() {
            @Override
            public void onCommentsLoaded(List<Comment> comments) {
                if (!isAdded()) return;

                // Mise à jour de l'adapter avant de changer la visibilité pour éviter les sauts visuels
                adapter.updateData(comments != null ? comments : new ArrayList<>());

                if (comments != null && !comments.isEmpty()) {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    // Scroll automatique pour voir le dernier message
                    recyclerView.smoothScrollToPosition(comments.size() - 1);
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Impossible de charger les messages", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void postComment() {
        String text = etInput.getText().toString().trim();
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();

        if (text.isEmpty() || incidentId == null) return;

        if (fbUser == null || currentUserData == null) {
            Toast.makeText(getContext(), "Initialisation du profil...", Toast.LENGTH_SHORT).show();
            return;
        }

        Comment comment = new Comment(
                incidentId,
                fbUser.getUid(),
                currentUserData.getNom(),
                currentUserData.getPhotoProfilUrl(),
                text
        );

        btnSend.setEnabled(false);

        repo.addComment(comment, new FirestoreRepository.OnFirestoreTaskComplete() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    etInput.setText("");
                    btnSend.setEnabled(true);
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    btnSend.setEnabled(true);
                    Toast.makeText(getContext(), "Échec de l'envoi", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}