package com.DGSD.Teexter.Activity;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.DGSD.Teexter.Extra;
import com.DGSD.Teexter.R;
import com.DGSD.Teexter.Fragment.ComposeFragment;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ComposeActivity extends SherlockFragmentActivity {

	private static final String KEY_COMPOSE_FRAGMENT = "_compose_fragment";
	private ActionBar mActionBar;
	private FragmentManager mFragmentManager;
	private ComposeFragment mComposeFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_with_single_fragment);
		
		//Set up the action bar
		mActionBar = getSupportActionBar();
		mActionBar.setTitle(R.string.compose);
		
		//The icon in the action bar should take us home!
		mActionBar.setDisplayHomeAsUpEnabled(true);
		
		//Present our fragment
		mFragmentManager = getSupportFragmentManager();
		
		if(savedInstanceState != null) {
			mComposeFragment = (ComposeFragment) mFragmentManager.getFragment(savedInstanceState, KEY_COMPOSE_FRAGMENT);
        }

        if(mComposeFragment == null) {
        	mComposeFragment = ComposeFragment.newInstance(getIntent().getIntExtra(Extra.ID, -1));

            mFragmentManager.beginTransaction()
			                .replace(R.id.container, mComposeFragment)
			                .commit();
        }
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		if(mComposeFragment != null && mComposeFragment.isAdded()) {
            mFragmentManager.putFragment(outState, KEY_COMPOSE_FRAGMENT, mComposeFragment);
        }
	}
}
