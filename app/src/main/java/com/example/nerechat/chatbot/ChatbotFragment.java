package com.example.nerechat.chatbot;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nerechat.MainActivity;
import com.example.nerechat.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;

import com.google.common.collect.Lists;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatbotFragment extends Fragment implements BotReply {


    Toolbar toolbar;
    RecyclerView recyclerView;
    EditText mensaje;
    ImageView send;
    CircleImageView barraPerfilImg;
    TextView barraUsername,barraStado;


    private SessionsClient sessionsClient;
    private SessionName sessionName;
    private String uuid = UUID.randomUUID().toString();
    private String TAG = "mainactivity";

    ChatAdapter chatAdapter;
    List<MessageBot> messageList = new ArrayList<MessageBot>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root=inflater.inflate(R.layout.fragment_chatbot, container, false);




        recyclerView=root.findViewById(R.id.chat_recyclerViewchatbot);
        mensaje=root.findViewById(R.id.chat_mensajechatbot);
        send=root.findViewById(R.id.chat_sendchatbot);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        BottomNavigationView nv=  ((AppCompatActivity)getActivity()).findViewById(R.id.nav_view);
        nv.setVisibility(View.GONE);

        toolbar=root.findViewById(R.id.chat_toolbarchatbot);
        barraPerfilImg=root.findViewById(R.id.barraImagenPErfil);
        barraUsername=root.findViewById(R.id.barraNombreUsu);
        barraStado=root.findViewById(R.id.barraEstado);
        barraPerfilImg.setImageResource(R.mipmap.ic_fotochatbot_round);
        barraUsername.setText("Chatbot");
        barraStado.setText("Conectado");

        ChatAdapter chatAdapter = new ChatAdapter(messageList, getActivity());
        recyclerView.setAdapter(chatAdapter);

        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                String message = mensaje.getText().toString();
                if (!message.isEmpty()) {
                    messageList.add(new MessageBot(message, false));
                    mensaje.setText("");
                    sendMessageToBot(message);
                    Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                    Objects.requireNonNull(recyclerView.getLayoutManager())
                            .scrollToPosition(messageList.size() - 1);
                } else {
                    Toast.makeText(getContext(), "Please enter text!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        setUpBot();



        return root;
    }



    private void setUpBot() {
        try {
            InputStream stream = this.getResources().openRawResource(R.raw.credential);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(Lists.newArrayList(getString(R.string.googleApis)));
            String projectId = ((ServiceAccountCredentials) credentials).getProjectId();

            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(
                    FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            sessionName = SessionName.of(projectId, uuid);

            Log.d(TAG, "projectId : " + projectId);
        } catch (Exception e) {
            Log.d(TAG, "setUpBot: " + e.getMessage());
        }
    }

    private void sendMessageToBot(String message) {
        QueryInput input = QueryInput.newBuilder()
                .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build();
        new SendMessageInBackground(this, sessionName, sessionsClient, input).execute();
    }

    @Override
    public void callback(DetectIntentResponse returnResponse) {
        if(returnResponse!=null) {
            String botReply = returnResponse.getQueryResult().getFulfillmentText();
            if(!botReply.isEmpty()){
                messageList.add(new MessageBot(botReply, true));
                chatAdapter = new ChatAdapter(messageList, getActivity());
                chatAdapter.notifyDataSetChanged();
                Objects.requireNonNull(recyclerView.getLayoutManager()).scrollToPosition(messageList.size() - 1);
            }else {
                Toast.makeText(getContext(), "something went wrong", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "failed to connect!", Toast.LENGTH_SHORT).show();
        }
    }




}