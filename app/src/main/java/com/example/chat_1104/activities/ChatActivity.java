//import package
package com.example.chat_1104.activities;

//import statements
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.Preference;
import android.util.Base64;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chat_1104.R;
import com.example.chat_1104.adapters.ChatAdapter;
import com.example.chat_1104.databinding.ActivityChatBinding;
import com.example.chat_1104.models.ChatMessage;
import com.example.chat_1104.models.User;
import com.example.chat_1104.utilities.Constants;
import com.example.chat_1104.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// Main class for ChatActivity
public class ChatActivity extends AppCompatActivity {

    //initialize binding, connect to chat ActivityChat
    private ActivityChatBinding binding;
    //initialize receiverUser
    private User receiverUser;
    //initialize chatMessages, chatAdapter, preferenceManager, database
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;

    // onCreate function to initialize functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadReceiverDetails();
        setListeners();
        init();
        ListenMessage();

    }

    // Initialize user information
    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)

        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    // Function to sendMessages using a HashMap to store user information
    private void sendMessages(){
        HashMap<String, Object> message = new HashMap<>();

        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);

        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        binding.inputMessage.setText(null);
    }

    // Create ListenMessage to display and load messages from user and other users
    private void ListenMessage(){
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,
                        preferenceManager.getString
                (Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverUser.id)
                .addSnapshotListener(eventListener);

        database.collection(Constants.KEY_COLLECTION_CHAT).
                whereEqualTo(Constants.KEY_SENDER_ID,
                        receiverUser.id).
                whereEqualTo(Constants.KEY_RECEIVER_ID,
                        preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);

    }
    // Create EventListener for Firebase, count messages, sort messages
    private final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
        if(error != null){
            return;
        }
        if(value != null){
            int count = chatMessages.size();
            for (DocumentChange documentChange:value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDataTime(
                            documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));

                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if(count ==  0){
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeChanged(chatMessages.size(),chatMessages.size());

                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size()-1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
    });


    // Get encoded image
    private Bitmap getBitmapFromEncodedString(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);

    }

    // Load in receiver details
    private void loadReceiverDetails(){
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);

    }

    // Go back to previous if button is clicked
    private void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());

        binding.layoutSend.setOnClickListener(v -> sendMessages());
    }

    // Define Data
    private String getReadableDataTime(Date date){
        return new SimpleDateFormat("MMM dd, yyyy - hh:mm a",
                Locale.getDefault()).format(date);
    }

}