package com.firebase.androidchat;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.ValueEventListener;

import com.scringo.Scringo;
import com.scringo.Scringo.ScringoIcon;
import com.scringo.ScringoActivationButton;
import com.scringo.ScringoEventHandler;
import com.scringo.ScringoLikeButton;
import com.scringo.ScringoLikeButton.ScringoLikeObjectType;
import com.scringo.utils.ScringoLogger.ScringoLogLevel;


import java.util.Random;

public class MainActivity extends ListActivity {

    // TODO: change this to your own Firebase URL
    private static final String FIREBASE_URL = "https://myrippleapps.firebaseIO.com";

    private String username;
    private Firebase ref;
    private ValueEventListener connectedListener;
    private ChatListAdapter chatListAdapter;
	private Scringo scringo = new Scringo(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 
		scringo.setLogLevel(ScringoLogLevel.SCRINGO_LOG_LEVEL_DEBUG);
		Scringo.setDebugMode(true);
		scringo.setIcon(ScringoIcon.PERSON);
		scringo.init();

		((ScringoActivationButton) findViewById(R.id.activationButton)).setScringo(scringo);
		

        // Make sure we have a username
        setupUsername();

        setTitle("chat sebagai " + username);

        // Setup our Firebase ref
        ref = new Firebase(FIREBASE_URL).child("chat");

        // Setup our input methods. Enter key on the keyboard or pushing the send button
        EditText inputText = (EditText)findViewById(R.id.messageInput);
        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMessage();
                }
                return true;
            }
        });

        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

    } 
	@Override
	public void onBackPressed() {
		if (!scringo.onBackPressed()) {
			super.onBackPressed();
		}
	} 

    @Override
    public void onStart() {
        super.onStart(); 
		scringo.onStart(); 
        // Setup our view and list adapter. Ensure it scrolls to the bottom as data changes
        final ListView listView = getListView();
        // Tell our list adapter that we only want 50 messages at a time
        chatListAdapter = new ChatListAdapter(ref.limit(50), this, R.layout.chat_message, username);
        listView.setAdapter(chatListAdapter);
        chatListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatListAdapter.getCount() - 1);
            }
        });

        // Finally, a little indication of connection status
        connectedListener = ref.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = (Boolean)dataSnapshot.getValue();
                if (connected) {
                    Toast.makeText(MainActivity.this, "okey da connect", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Disconnect dlu kasi jimat bateri", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled() {
                // No-op
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop(); 
		scringo.onStop(); 
        ref.getRoot().child(".info/connected").removeEventListener(connectedListener);
        chatListAdapter.cleanup();
    }

    private void setupUsername() {
        SharedPreferences prefs = getApplication().getSharedPreferences("ChatPrefs", 0);
        username = prefs.getString("username", null);
        if (username == null) {
            Random r = new Random();
            // Assign a random user name if we don't have one saved.
            username = "TukangTest" + r.nextInt(100000);
            prefs.edit().putString("username", username).commit();
        }
    }

    private void sendMessage() {
        EditText inputText = (EditText)findViewById(R.id.messageInput);
        String input = inputText.getText().toString();
        if (!input.equals("")) {
            // Create our 'model', a Chat object
            Chat chat = new Chat(input, username);
            // Create a new, auto-generated child of that chat location, and save our chat data there
            ref.push().setValue(chat);
            inputText.setText("");
        }
    }
}
