package com.nfc.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

public class NFCTicTacToeActivity extends Activity {

	private static final String TAG = "NFC_Tic_Tac_ToeActivity";
	private static final String DELIMETER = "-";
	private static final boolean SHOW_PROMPT = false;

	private static final String PACKAGE_NAME = "com.nfc.sample";

	private Context mCtx = null;
	static int CELL_USED = 100;

	static int FREE = 0;
	static int TICK = 1;
	static int TOCK = 2;

	private static final int MESSAGE_SENT = 1;

	int moveCount = 0;

	int[] mGameMatrix = new int[9];

	private TableLayout mTl = null;

	private boolean mResumed = false;
	private boolean mWriteMode = false;

	private NfcAdapter mNfcAdapter = null;
	private PendingIntent mNfcPendingIntent;
	private IntentFilter[] mWriteTagFilters;
	private IntentFilter[] mNdefExchangeFilters;

	private void initNFC() {
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

		// Handle all of our received NFC intents in this activity.
		mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		// Intent filters for reading a note from a tag or exchanging over p2p.
		IntentFilter ndefDetected = new IntentFilter(
				NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			ndefDetected.addDataType("state/data");
		} catch (MalformedMimeTypeException e) {
			// Handle Properly
		}
		mNdefExchangeFilters = new IntentFilter[] { ndefDetected };

		// Intent filters for writing to a tag
		IntentFilter tagDetected = new IntentFilter(
				NfcAdapter.ACTION_TAG_DISCOVERED);
		mWriteTagFilters = new IntentFilter[] { tagDetected };

	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mTl = (TableLayout) findViewById(R.id.TL);
		mCtx = this;
		try {
			initNFC();
			if (!mNfcAdapter.isEnabled()) {
				toast("Turn on NFC in Settings -> Wireles & Networks");
				finish();
			}
		} catch (Exception e) {
			toast("NFC Not Enabled! App terminated");
			finish();
		}
		//
		NdefMessage ndefMsg = NFCUtil.createNdefMsg(mGameMatrix,
				NdefRecord.createApplicationRecord(PACKAGE_NAME));

		// Used to send static information of the activity.
		mNfcAdapter.setNdefPushMessage(ndefMsg, this, this);

		// Register callback to send current state of the app.
		mNfcAdapter
				.setNdefPushMessageCallback(mCreateNdefMessageCallback, this);

		// Register callback to listen for message-sent success
		mNfcAdapter.setOnNdefPushCompleteCallback(mOnNdefPushCompleteCallback,
				this);
	}

	private CreateNdefMessageCallback mCreateNdefMessageCallback = new CreateNdefMessageCallback() {

		@Override
		public NdefMessage createNdefMessage(NfcEvent arg0) {
			NdefMessage ndefMsg = NFCUtil.createNdefMsg(mGameMatrix,
					NdefRecord.createApplicationRecord(PACKAGE_NAME));
			return ndefMsg;
		}
	};

	private OnNdefPushCompleteCallback mOnNdefPushCompleteCallback = new OnNdefPushCompleteCallback() {

		@Override
		public void onNdefPushComplete(NfcEvent arg0) {
			// A handler is needed to send messages to the activity when this
			// callback occurs, because it happens from a binder thread
			mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();

		}
	};

	/** This handler receives a message from onNdefPushComplete */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_SENT:
				Toast.makeText(getApplicationContext(), "Message sent!",
						Toast.LENGTH_LONG).show();
				break;
			}
		}
	};

	public void onMove(View v) {
		ImageView imgv = (ImageView) v;
		int idx = Integer.parseInt((String) v.getTag());
		if (idx != CELL_USED) {
			mGameMatrix[idx] = 0 == moveCount % 2 ? TICK : TOCK;
			imgv.setImageResource(0 == moveCount % 2 ? R.drawable.tick
					: R.drawable.cross);
			imgv.setTag(CELL_USED + "");
			moveCount++;

			// if (mResumed) {
			// NdefMessage ndefMsg = NFCUtil.getIntArrayAsNdef(
			// NdefRecord.TNF_MIME_MEDIA, "state/data", "",
			// mGameMatrix, "-");
			//
			// mNfcAdapter.enableForegroundNdefPush(NFCTicTacToeActivity.this,
			// ndefMsg);
			// }

		}
	}

	public void onReset(View v) {
		TableRow tr = null;
		ImageView imgV = null;
		for (int i = 0; i < 3; i++) {
			tr = (TableRow) mTl.getChildAt(i);
			for (int j = 0; j < 3; j++) {
				imgV = (ImageView) tr.getChildAt(j);
				imgV.setImageResource(R.drawable.free);
				imgV.setTag((i * 3 + j) + "");
				// System.out.println((i * 3 + j));
				mGameMatrix[i * 3 + j] = FREE;
			}
		}
	}

	public void onShare(View v) {
		mWriteMode = true;

		// Write to a tag for as long as the dialog is shown.
		mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
				mWriteTagFilters, null);

		new AlertDialog.Builder(NFCTicTacToeActivity.this)
				.setTitle("Touch tag to write the state of the game!")
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						mNfcAdapter.enableForegroundDispatch((Activity) mCtx,
								mNfcPendingIntent, mNdefExchangeFilters, null);
						mWriteMode = false;
					}
				}).create().show();
	}

	private void promptForContent(final String[] state) {
		new AlertDialog.Builder(this)
				.setTitle("Replace current content?")
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								updateUI(state);
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {

					}
				}).show();
	}

	public void updateUI(String[] state) {
		TableRow tr = null;
		ImageView imgV = null;

		moveCount = 0;
		int stateIdx = -1;
		for (int i = 0; i < 3; i++) {
			tr = (TableRow) mTl.getChildAt(i);
			for (int j = 0; j < 3; j++) {
				stateIdx = Integer.parseInt(state[i * 3 + j]);
				imgV = (ImageView) tr.getChildAt(j);
				System.out.println("i = j = idx : " + i + " = " + j + " = "
						+ stateIdx);
				if (FREE == stateIdx) {
					imgV.setImageResource(R.drawable.free);
					imgV.setTag((i * 3 + j) + "");
					mGameMatrix[i * 3 + j] = FREE;
				} else if (TICK == stateIdx) {
					imgV.setImageResource(R.drawable.tick);
					imgV.setTag(CELL_USED + "");
					mGameMatrix[i * 3 + j] = TICK;
					moveCount++;
				} else if (TOCK == stateIdx) {
					imgV.setImageResource(R.drawable.cross);
					imgV.setTag(CELL_USED + "");
					mGameMatrix[i * 3 + j] = TOCK;
					moveCount++;
				}
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mResumed = true;
		// NDEF received from Android
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			NdefMessage[] messages = NFCUtil.getNdefMessages(getIntent());
			String body = new String(messages[0].getRecords()[0].getPayload());
			String[] state = body.split(DELIMETER);
			if (SHOW_PROMPT) {
				promptForContent(state);
			} else {
				updateUI(state);
			}
			setIntent(new Intent());
		}

		mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
				mNdefExchangeFilters, null);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mResumed = false;
		mNfcAdapter.disableForegroundDispatch(this);

	}

	@Override
	protected void onNewIntent(Intent intent) {
		// NDEF exchange mode
		if (!mWriteMode
				&& NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
			NdefMessage[] msgs = NFCUtil.getNdefMessages(intent);
			String body = new String(msgs[0].getRecords()[0].getPayload());
			String[] state = body.split(DELIMETER);
			if (SHOW_PROMPT) {
				promptForContent(state);
			} else {
				updateUI(state);
			}
		}
		// Tag writing mode
		if (mWriteMode
				&& NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
			NdefMessage ndefMsg = NFCUtil.createNdefMsg(mGameMatrix,
					NdefRecord.createApplicationRecord(PACKAGE_NAME));

			Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			NFCUtil.writeTag(ndefMsg, detectedTag, this);
		}
	}

	private void toast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}
}