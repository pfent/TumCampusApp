package de.tum.in.tumcampusapp.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.activities.generic.BaseActivity;
import de.tum.in.tumcampusapp.adapters.CardsAdapter;
import de.tum.in.tumcampusapp.auxiliary.NetUtils;
import de.tum.in.tumcampusapp.cards.Card;
import de.tum.in.tumcampusapp.models.managers.CardManager;
import de.tum.in.tumcampusapp.services.SilenceService;

/**
 * Main activity displaying the cards and providing navigation with navigation drawer
 */
public class MainActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener {
    /**
     * Navigation Drawer
     */
    private ActionBarDrawerToggle mDrawerToggle;
    private boolean registered;

    /**
     * Card list
     */
    private RecyclerView mCardsView;
    private CardsAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NetUtils.isConnected(context)) {
                refreshCards();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        unregisterReceiver(connectivityChangeReceiver);
                        registered = false;
                    }
                });
            }
        }
    };

    public MainActivity() {
        super(R.layout.activity_main);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup pull to refresh
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.ptr_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.color_primary,
                R.color.tum_A100,
                R.color.tum_A200);

        // Setup card RecyclerView
        mCardsView = (RecyclerView) findViewById(R.id.cards_view);
        registerForContextMenu(mCardsView);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mCardsView.setLayoutManager(layoutManager);
        mCardsView.setHasFixedSize(true);

        //Swipe gestures
        new ItemTouchHelper(new MainActivityTouchHelperCallback()).attachToRecyclerView(mCardsView);

        // Start silence Service (if already started it will just invoke a check)
        Intent service = new Intent(this, SilenceService.class);
        this.startService(service);

        // Set the list's click listener
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                ActivityCompat.invalidateOptionsMenu(MainActivity.this); // creates call to onPrepareOptionsMenu()
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                ActivityCompat.invalidateOptionsMenu(MainActivity.this); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        if (CardManager.shouldRefresh || CardManager.getCards() == null) {
            refreshCards();
        } else {
            initAdapter();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    /**
     * Setup cards adapter
     */
    private void initAdapter() {
        mAdapter = new CardsAdapter();
        mCardsView.setAdapter(mAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registered) {
            unregisterReceiver(connectivityChangeReceiver);
            registered = false;
        }
    }

    /**
     * If drawer is expanded hide settings icon
     *
     * @param menu Menu instance
     * @return True if handled
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Sync the toggle state after onRestoreInstanceState has occurred.
     *
     * @param savedInstanceState Saved instance state bundle
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    /**
     * Let the drawer toggle handle configuration changes
     *
     * @param newConfig The new configuration
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Handle expansion of navigation drawer and settings menu item click
     *
     * @param item Clicked menu item
     * @return True if handled
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Opens the preferences screen
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, UserPreferencesActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Show progress indicator and start updating cards in background
     */
    public void refreshCards() {
        mSwipeRefreshLayout.setRefreshing(true);
        onRefresh();
    }

    /**
     * Starts updating cards in background
     * Called when {@link SwipeRefreshLayout} gets triggered.
     */
    @Override
    public void onRefresh() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                CardManager.update(MainActivity.this);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                if (mAdapter == null) {
                    initAdapter();
                } else {
                    mAdapter.notifyDataSetChanged();
                }

                mSwipeRefreshLayout.setRefreshing(false);
                if (!registered && !NetUtils.isConnected(MainActivity.this)) {
                    registerReceiver(connectivityChangeReceiver,
                            new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                    registered = true;
                }
            }
        }.execute();
    }

    /**
     * Executed when the RestoreCard is pressed
     */
    public void restoreCards(View view) {
        CardManager.restoreCards();
        refreshCards();
        showToolbar();
    }

    /**
     * Smoothly scrolls the RecyclerView to the top and dispatches nestedScrollingEvents to show
     * the Toolbar
     */
    @SuppressLint("NewApi")
    // Verified in a API 10 emulator that this works, even though AndroidLint reports otherwise
    private void showToolbar() {
        mCardsView.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
        mCardsView.dispatchNestedFling(0, Integer.MIN_VALUE, true);
        mCardsView.stopNestedScroll();
        mCardsView.getLayoutManager().smoothScrollToPosition(mCardsView, null, 0);
    }

    /**
     * A touch helper class, Handles swipe to dismiss events
     */
    private class MainActivityTouchHelperCallback extends ItemTouchHelper.SimpleCallback {

        public MainActivityTouchHelperCallback() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            Card.CardViewHolder cardViewHolder = (Card.CardViewHolder) viewHolder;
            if (!cardViewHolder.getCurrentCard().isDismissable()) {
                return 0;
            }
            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            //Moving is not allowed as per flags
            return false;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            Card.CardViewHolder cardViewHolder = (Card.CardViewHolder) viewHolder;
            final Card card = cardViewHolder.getCurrentCard();
            final int lastPos = mAdapter.remove(card);
            final View coordinatorLayoutView = findViewById(R.id.coordinator);

            Snackbar.make(coordinatorLayoutView, R.string.card_dismissed, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mAdapter.insert(lastPos, card);
                            mCardsView.getLayoutManager().smoothScrollToPosition(mCardsView, null, lastPos);
                        }

                    }).setCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    super.onDismissed(snackbar, event);
                    if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                        //DISMISS_EVENT_ACTION means, the snackbar was dismissed via the undo button
                        //and therefore, we didn't really dismiss the card
                        card.discardCard();
                    }
                }
            })
                    .show();
        }
    }
}
