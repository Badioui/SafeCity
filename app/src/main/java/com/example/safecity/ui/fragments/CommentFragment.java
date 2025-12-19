package com.example.safecity.ui.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment de type BottomSheet pour afficher et ajouter des commentaires.
 * Cette version coulissante permet une navigation plus fluide.
 */
public class CommentFragment extends BottomSheetDialogFragment {

    private static final String ARG_INCIDENT_ID = "incident_id";

    private String incidentId;
    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private EditText etInput;
    private ImageButton btnSend;
    private TextView tvEmpty;

    private FirestoreRepository repo;
    private ListenerRegistration listenerRegistration;
    private Utilisateur currentUserData;

    public static CommentFragment newInstance(String incidentId) {
        CommentFragment fragment = new CommentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INCIDENT_ID, incidentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Style pour avoir des coins arrondis en haut du BottomSheet
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
        if (getArguments() != null) {
            incidentId = getArguments().getString(ARG_INCIDENT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comment, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        // Force l'ouverture du BottomSheet en plein écran ou état déplié
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                BottomSheetBehavior.from(bottomSheet).setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_comments);
        etInput = view.findViewById(R.id.et_comment_input);
        btnSend = view.findViewById(R.id.btn_send_comment);
        tvEmpty = view.findViewById(R.id.tv_no_comments);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CommentAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        repo = new FirestoreRepository();
        loadCurrentUserInfo();
        startListeningComments();

        btnSend.setOnClickListener(v -> postComment());
    }

    private void loadCurrentUserInfo() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            repo.getUser(uid, new FirestoreRepository.OnUserLoadedListener() {
                @Override
                public void onUserLoaded(Utilisateur utilisateur) {
                    currentUserData = utilisateur;
                }
                @Override
                public void onError(Exception e) {}
            });
        }
    }

    private void startListeningComments() {
        listenerRegistration = repo.getCommentsRealtime(incidentId, new FirestoreRepository.OnCommentsLoadedListener() {
            @Override
            public void onCommentsLoaded(List<Comment> comments) {
                if (!isAdded()) return;
                adapter.updateData(comments);
                tvEmpty.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                if (!comments.isEmpty()) {
                    recyclerView.smoothScrollToPosition(comments.size() - 1);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Erreur de chargement", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postComment() {
        String text = etInput.getText().toString().trim();
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();

        if (text.isEmpty()) return;
        if (fbUser == null || currentUserData == null) {
            Toast.makeText(getContext(), "Veuillez patienter...", Toast.LENGTH_SHORT).show();
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
        if (listenerRegistration != null) listenerRegistration.remove();
    }
}