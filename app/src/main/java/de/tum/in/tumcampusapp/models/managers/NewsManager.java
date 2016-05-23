package de.tum.in.tumcampusapp.models.managers;

import android.content.Context;
import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

import de.tum.in.tumcampusapp.auxiliary.Const;
import de.tum.in.tumcampusapp.auxiliary.NetUtils;
import de.tum.in.tumcampusapp.auxiliary.Utils;
import de.tum.in.tumcampusapp.cards.Card;
import de.tum.in.tumcampusapp.cards.NewsCard;
import de.tum.in.tumcampusapp.models.News;

/**
 * News Manager, handles database stuff, external imports
 */
public class NewsManager extends AbstractManager implements Card.ProvidesCard {

    private static final int TIME_TO_SYNC = 1800; // 1/2 hour
    private final Context mContext;

    private static final String NEWS_URL = "https://tumcabe.in.tum.de/Api/news/";
    private static final String NEWS_SOURCES_URL = NEWS_URL + "sources";

    /**
     * Constructor, open/create database, create table if necessary
     *
     * @param context Context
     */
    public NewsManager(Context context) {
        super(context);
        mContext = context;

        // create news sources table
        db.execSQL("CREATE TABLE IF NOT EXISTS news_sources (id INTEGER PRIMARY KEY, icon VARCHAR, title VARCHAR)");

        // create table if needed
        db.execSQL("CREATE TABLE IF NOT EXISTS news (id INTEGER PRIMARY KEY, src INTEGER, title TEXT, link VARCHAR, "
                + "image VARCHAR, date VARCHAR, created VARCHAR, dismissed INTEGER)");
    }

    /**
     * Removes all old items (older than 3 months)
     */
    void cleanupDb() {
        db.execSQL("DELETE FROM news WHERE date < date('now','-3 month')");
    }

    /**
     * Download news from external interface (JSON)
     *
     * @param force True to force download over normal sync period, else false
     * @throws Exception
     */
    public void downloadFromExternal(boolean force) throws Exception {

        if (!force && !SyncManager.needSync(db, this, TIME_TO_SYNC)) {
            return;
        }

        NetUtils net = new NetUtils(mContext);
        // Load all news sources
        JSONArray jsonArray = net.downloadJsonArray(NEWS_SOURCES_URL, CacheManager.VALIDITY_ONE_MONTH, force);

        if (jsonArray != null) {
            db.beginTransaction();
            try {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    replaceIntoSourcesDb(obj);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        // Load all news since the last sync
        jsonArray = net.downloadJsonArray(NEWS_URL + getLastId(), CacheManager.VALIDITY_ONE_DAY, force);

        // Delete all too old items
        cleanupDb();

        if (jsonArray == null) {
            return;
        }

        db.beginTransaction();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                replaceIntoDb(getFromJson(obj));
            }
            SyncManager.replaceIntoDb(db, this);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Convert JSON object to News and download news image
     *
     * @param json see above
     * @return News
     * @throws Exception
     */
    private static News getFromJson(JSONObject json) throws Exception {
        String id = json.getString(Const.JSON_NEWS);
        String src = json.getString(Const.JSON_SRC);
        String title = json.getString(Const.JSON_TITLE);
        String link = json.getString(Const.JSON_LINK);
        String image = json.getString(Const.JSON_IMAGE);
        Date date = Utils.getISODateTime(json.getString(Const.JSON_DATE));
        Date created = Utils.getISODateTime(json.getString(Const.JSON_CREATED));

        return new News(id, title, link, src, image, date, created);
    }

    /**
     * Get all news from the database
     *
     * @return Database cursor (_id, src, title, description, link, image, date, created, icon, source)
     */
    public Cursor getAllFromDb(Context context) {
        String selectedNewspread = Utils.getSetting(mContext, "news_newspread", "7");
        String and = "";
        Cursor c = getNewsSources();
        if (c.moveToFirst()) {
            do {
                int id = c.getInt(0);
                boolean show = Utils.getSettingBool(context, "news_source_" + id, id <= 7);
                if (show) {
                    if (!and.isEmpty())
                        and += " OR ";
                    and += "s.id=\"" + id + "\"";
                }
            } while (c.moveToNext());
        }
        c.close();
        return db.rawQuery("SELECT n.id AS _id, n.src, n.title, " +
                "n.link, n.image, n.date, n.created, s.icon, s.title AS source, n.dismissed, " +
                "(julianday('now') - julianday(date)) AS diff " +
                "FROM news n, news_sources s " +
                "WHERE n.src=s.id " + (and.isEmpty() ? "" : "AND (" + and + ") ") +
                "AND (s.id < 7 OR s.id > 13 OR s.id=?) " +
                "ORDER BY date DESC", new String[]{selectedNewspread});
    }

    /**
     * Get the index of the newest item that is older than 'now'
     *
     * @return index of the newest item that is older than 'now' - 1
     */
    public int getTodayIndex() {
        String selectedNewspread = Utils.getSetting(mContext, "news_newspread", "7");
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM news WHERE date(date)>date() AND (src < 7 OR src > 13 OR src=?)", new String[]{selectedNewspread});
        if (c.moveToFirst()) {
            int res = c.getInt(0);
            c.close();
            return res == 0 ? 0 : res - 1;
        }
        c.close();
        return 0;
    }

    private String getLastId() {
        String lastId = "";
        Cursor c = db.rawQuery("SELECT id FROM news ORDER BY id DESC LIMIT 1", null);
        if (c.moveToFirst()) {
            lastId = c.getString(0);
        }
        c.close();
        return lastId;
    }

    public Cursor getNewsSources() {
        String selectedNewspread = Utils.getSetting(mContext, "news_newspread", "7");
        return db.rawQuery("SELECT id, icon, " +
                "CASE WHEN title LIKE 'newspread%' THEN \"Newspread\" ELSE title END " +
                "FROM news_sources WHERE id < 7 OR id > 13 OR id=?", new String[]{selectedNewspread});
    }

    /**
     * Replace or Insert a event in the database
     *
     * @param n News object
     */
    void replaceIntoDb(News n) {
        db.execSQL("REPLACE INTO news (id, src, title, link, image, date, " +
                        "created, dismissed) VALUES (?, ?, ?, ?, ?, ?, ?, 0)",
                new String[]{n.id, n.src, n.title, n.link, n.image,
                        Utils.getDateTimeString(n.date), Utils.getDateTimeString(n.created)});
    }

    /**
     * Replace or Insert a news source in the database
     *
     * @param n News source object
     * @throws Exception
     */
    void replaceIntoSourcesDb(JSONObject n) throws Exception {
        db.execSQL("REPLACE INTO news_sources (id, icon, title) VALUES (?, ?, ?)",
                new String[]{n.getString(Const.JSON_SOURCE), n.has(Const.JSON_ICON) ? n.getString(Const.JSON_ICON) : "",
                        n.getString(Const.JSON_TITLE)});
    }

    public void setDismissed(String id, int d) {
        db.execSQL("UPDATE news SET dismissed=? WHERE id=?", new String[]{"" + d, id});
    }

    /**
     * Adds the newest news card
     *
     * @param context Context
     */
    @Override
    public void onRequestCard(Context context) {
        String and = "";
        Cursor c = getNewsSources();
        if (c.moveToFirst()) {
            do {
                int id = c.getInt(0);
                boolean show = Utils.getSettingBool(context, "card_news_source_" + id, true);
                if (show) {
                    if (!and.isEmpty())
                        and += " OR ";
                    and += "s.id=\"" + id + "\"";
                }
            } while (c.moveToNext());
        }
        c.close();

        //boolean showImportant = Utils.getSettingBool(context, "card_news_alert", true);
        if (!and.isEmpty()) {

            String query = "SELECT n.id AS _id, n.src, n.title, " +
                    "n.link, n.image, n.date, n.created, s.icon, s.title AS source, n.dismissed, " +
                    "ABS(julianday(date()) - julianday(n.date)) AS date_diff ";

            if (Utils.getSettingBool(context, "card_news_latest_only", true)) {
                // Limit to one entry per source
                query += "FROM (news n JOIN ( " +
                        "SELECT src, MIN(abs(julianday(date()) - julianday(date))) AS diff " +
                        "FROM news WHERE src!=\"2\" OR (julianday(date()) - julianday(date))<0 " +
                        "GROUP BY src) last ON (n.src = last.src " +
                        "AND date_diff = last.diff) " +
                        "), news_sources s ";
            } else {
                query += "FROM news n, news_sources s ";
            }

            query += "WHERE n.src = s.id AND ((" + and + ") " +
                    ") ORDER BY date_diff ASC";
            Cursor cur = db.rawQuery(query, null);

            int i = 0;
            if (cur.moveToFirst()) {
                do {
                    NewsCard card = new NewsCard(context);
                    card.setNews(cur, i);
                    card.apply();
                    i++;
                } while (cur.moveToNext());
            }
            cur.close();
        }
    }
}