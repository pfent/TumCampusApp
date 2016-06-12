package de.tum.in.tumcampusapp.auxiliary;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.activities.CafeteriaActivity;
import de.tum.in.tumcampusapp.activities.CalendarActivity;
import de.tum.in.tumcampusapp.activities.ChatRoomsActivity;
import de.tum.in.tumcampusapp.activities.CurriculaActivity;
import de.tum.in.tumcampusapp.activities.LecturesPersonalActivity;
import de.tum.in.tumcampusapp.activities.NewsActivity;
import de.tum.in.tumcampusapp.activities.OpeningHoursListActivity;
import de.tum.in.tumcampusapp.activities.OrganisationActivity;
import de.tum.in.tumcampusapp.activities.PersonsSearchActivity;
import de.tum.in.tumcampusapp.activities.PlansActivity;
import de.tum.in.tumcampusapp.activities.RoomFinderActivity;
import de.tum.in.tumcampusapp.activities.StudyRoomsActivity;
import de.tum.in.tumcampusapp.activities.SurveyActivity;
import de.tum.in.tumcampusapp.activities.TransportationActivity;
import de.tum.in.tumcampusapp.activities.TuitionFeesActivity;

public class DrawerMenuHelper implements NavigationView.OnNavigationItemSelectedListener {

    private final Context mContext;
    private final DrawerLayout mDrawerLayout;

    public DrawerMenuHelper(Context context, DrawerLayout drawerLayout) {
        mContext = context;
        mDrawerLayout = drawerLayout;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        mDrawerLayout.closeDrawers();
        Intent intent = menuItem.getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        mContext.startActivity(intent);
        return true;
    }

    public void populateMenu(Menu navigationMenu) {
        boolean hasTUMOAccess = new AccessTokenManager(mContext).hasValidAccessToken();
        boolean chatEnabled = Utils.getSettingBool(mContext, Const.GROUP_CHAT_ENABLED, false);

        if (hasTUMOAccess) {
            SubMenu myTumMenu = navigationMenu.addSubMenu(R.string.my_tum);
            for (SideNavigationItem item : myTum) {
                if (!(item.needsChatAccess && !chatEnabled)) {
                    myTumMenu.add(item.titleRes).setIcon(item.iconRes).setIntent(new Intent(mContext, item.activity));
                }
            }
        }

        SubMenu commonTumMenu = navigationMenu.addSubMenu(R.string.tum_common);
        for (SideNavigationItem item : commonTum) {
            if (!(item.needsTUMOAccess && !hasTUMOAccess)) {
                commonTumMenu.add(item.titleRes).setIcon(item.iconRes).setIntent(new Intent(mContext, item.activity));
            }
        }
    }

    private static final SideNavigationItem[] myTum = {
            new SideNavigationItem(R.string.schedule, R.drawable.ic_calendar, CalendarActivity.class, true, false),
            new SideNavigationItem(R.string.my_lectures, R.drawable.ic_my_lectures, LecturesPersonalActivity.class, true, false),
            new SideNavigationItem(R.string.chat_rooms, R.drawable.ic_comment, ChatRoomsActivity.class, true, true),
            // new SideNavigationItem(R.string.my_grades,R.drawable.ic_my_grades, true, GradesActivity.class),
            new SideNavigationItem(R.string.tuition_fees, R.drawable.ic_money, TuitionFeesActivity.class, true, false),
    };

    private static final SideNavigationItem[] commonTum = {
            new SideNavigationItem(R.string.menues, R.drawable.ic_cutlery, CafeteriaActivity.class, false, false),
            new SideNavigationItem(R.string.news, R.drawable.ic_rss, NewsActivity.class, false, false),
            new SideNavigationItem(R.string.mvv, R.drawable.ic_mvv, TransportationActivity.class, false, false),
            new SideNavigationItem(R.string.plans, R.drawable.ic_plans, PlansActivity.class, false, false),
            new SideNavigationItem(R.string.roomfinder, R.drawable.ic_place, RoomFinderActivity.class, false, false),
            new SideNavigationItem(R.string.study_rooms, R.drawable.ic_group_work, StudyRoomsActivity.class, false, false),
            new SideNavigationItem(R.string.opening_hours, R.drawable.ic_time, OpeningHoursListActivity.class, false, false),
            new SideNavigationItem(R.string.quiz_title, R.drawable.ic_pie_chart, SurveyActivity.class, true, false),
            new SideNavigationItem(R.string.person_search, R.drawable.ic_users, PersonsSearchActivity.class, true, false),
            new SideNavigationItem(R.string.organisations, R.drawable.ic_organisations, OrganisationActivity.class, true, false),
            new SideNavigationItem(R.string.study_plans, R.drawable.ic_study_plans, CurriculaActivity.class, false, false)
    };

    public static class SideNavigationItem {
        int titleRes, iconRes;
        Class<? extends Activity> activity;
        boolean needsTUMOAccess, needsChatAccess;

        public SideNavigationItem(int titleRes, int iconRes, Class<? extends Activity> activity, boolean needsTUMOAccess, boolean needsChatAccess) {
            this.titleRes = titleRes;
            this.iconRes = iconRes;
            this.activity = activity;
            this.needsTUMOAccess = needsTUMOAccess;
            this.needsChatAccess = needsChatAccess;
        }
    }
}
