package com.DGSD.Teexter.Activity;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.DGSD.Teexter.BroadcastType;
import com.DGSD.Teexter.Extra;
import com.DGSD.Teexter.R;
import com.DGSD.Teexter.Fragment.ComposeFragment;
import com.DGSD.Teexter.Fragment.ComposeFragment.OnDeleteListener;
import com.DGSD.Teexter.Fragment.ComposeFragment.OnFavouriteListener;
import com.DGSD.Teexter.Service.DatabaseService;
import com.DGSD.Teexter.Service.DatabaseService.RequestType;
import com.DGSD.Teexter.Utils.IntentUtils;
import com.DGSD.Teexter.Utils.ToastUtils;

public class ComposeActivity extends BaseReceiverActivity implements OnDeleteListener, OnFavouriteListener,
		OnClickListener {

	private static final int CONFIRM_DELETE_DIALOG = 0;

	private static final String KEY_COMPOSE_FRAGMENT = "_compose_fragment";
	private ActionBar mActionBar;
	private FragmentManager mFragmentManager;
	private ComposeFragment mComposeFragment;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_with_single_fragment);

		// Set up the action bar
		mActionBar = getActionBar();
		mActionBar.setTitle(R.string.compose);

		// The icon in the action bar should take us home!
		mActionBar.setDisplayHomeAsUpEnabled(true);

		// Present our fragment
		mFragmentManager = getFragmentManager();

		if (savedInstanceState != null) {
			mComposeFragment = (ComposeFragment) mFragmentManager.getFragment(savedInstanceState, KEY_COMPOSE_FRAGMENT);
		}

		if (mComposeFragment == null) {
			final Intent intent = getIntent();
			mComposeFragment = ComposeFragment.newInstance(intent.getIntExtra(Extra.ID, -1),
					intent.getBooleanExtra(Extra.IS_REPLY, false), intent.getBooleanExtra(Extra.IS_FORWARD, false));

			mFragmentManager.beginTransaction().replace(R.id.container, mComposeFragment).commit();
		}

		mComposeFragment.setOnDeleteListener(this);
		mComposeFragment.setOnFavouriteListener(this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mComposeFragment != null && mComposeFragment.isAdded()) {
			mFragmentManager.putFragment(outState, KEY_COMPOSE_FRAGMENT, mComposeFragment);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				IntentUtils.goHome(this);
				return true;
		}

		return false;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();

		switch (intent.getIntExtra(Extra.DATA_TYPE, -1)) {
			case RequestType.DELETE_INBOX: {
				if (action.equals(BroadcastType.SUCCESS)) {
					ToastUtils.show(this, "Message Deleted", Toast.LENGTH_SHORT);
					finish();
				} else {
					ToastUtils.show(this, "Error deleting message", Toast.LENGTH_SHORT);
				}
				break;
			}
			case RequestType.TOGGLE_FAVOURITE: {
				if (action.equals(BroadcastType.SUCCESS)) {
					ToastUtils.show(this, "Message added to favourites", Toast.LENGTH_SHORT);
					finish();
				} else {
					ToastUtils.show(this, "Error adding message to favourites", Toast.LENGTH_SHORT);
				}
				break;
			}
		}

		hideProgressBar();
	}

	@Override
	public Dialog onCreateDialog(int which) {
		Dialog dialog = null;
		switch (which) {
			case CONFIRM_DELETE_DIALOG:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.are_you_sure);
				builder.setMessage(R.string.message_delete_are_you_sure);
				builder.setPositiveButton(R.string.yes, this);
				builder.setNegativeButton(R.string.cancel, this);
				dialog = builder.create();
				break;
		}

		return dialog;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onDelete(int id) {
		// id can only be the same as getIntent().getIntExtra(Extra.ID, -1)
		showDialog(CONFIRM_DELETE_DIALOG);
	}

	@Override
	public void onToggleFavourite(int id) {
		showProgressBar();
		DatabaseService.requestToggleFavourite(this, id, mComposeFragment.isFavourite());
	}
	
	@Override
	public void onClick(DialogInterface dialog, int id) {
		switch (id) {
			case DialogInterface.BUTTON_POSITIVE:
				DatabaseService.requestDeleteInboxMessage(this, getIntent().getIntExtra(Extra.ID, -1));
				showProgressBar();
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				// fall through
				break;
		}

		dialog.dismiss();
	}
}
