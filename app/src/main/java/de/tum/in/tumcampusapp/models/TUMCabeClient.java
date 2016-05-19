package de.tum.in.tumcampusapp.models;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.tum.in.tumcampusapp.auxiliary.AuthenticationManager;
import de.tum.in.tumcampusapp.auxiliary.Const;
import okhttp3.CertificatePinner;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public class TUMCabeClient {

    private static final String API_HOSTNAME = Const.API_HOSTNAME;
    private static final String API_BASEURL = "/Api/";
    private static final String API_CHAT = "chat/";
    private static final String API_CHAT_ROOMS = API_CHAT + "rooms/";
    private static final String API_CHAT_MEMBERS = API_CHAT + "members/";
    private static final String API_SESSION = "session/";
    private static final String API_NEWS = "news/";
    private static final String API_MENSA = "mensen/";
    private static final String API_CURRICULA = "curricula/";
    private static final String API_REPORT = "report/";
    private static final String API_STATISTICS = "statistics/";
    private static final String API_CINEMA = "kino/";
    private static final String API_NOTIFICATIONS = "notifications/";
    private static final String API_LOCATIONS = "locations/";
    private static final String API_DEVICE = "device/";


    private static TUMCabeClient instance = null;
    private static Context context = null;
    final Interceptor requestInterceptor = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            return chain.proceed(original.newBuilder()
                    .addHeader("X-DEVICE-ID", AuthenticationManager.getDeviceID(TUMCabeClient.context))
                    .build()
            );
        }
    };
    private TUMCabeAPIService service = null;

    private TUMCabeClient() {
        //Pin our known fingerprints, which I retrieved on 28. June 2015
        final CertificatePinner certificatePinner = new CertificatePinner.Builder()
                .add(API_HOSTNAME, "sha1/eeoui1Gne7kkDN/6HlgoxHkD18s=") //Fakultaet fuer Informatik
                .add(API_HOSTNAME, "sha1/AC508zHZltt8Aa1ZpUg5C9tMNJ8=") //Technische Universitaet Muenchen
                .add(API_HOSTNAME, "sha1/7+NhGLCLRZ1RDbncIhu3ksHeOok=") //DFN-Verein PCA Global
                .add(API_HOSTNAME, "sha1/8GO6fJoWdEqc21TsI81nKY58SU0=") //Deutsche Telekom Root CA 2
                .build();
        final OkHttpClient client = new OkHttpClient.Builder()
                .certificatePinner(certificatePinner)
                .addInterceptor(requestInterceptor)
                .build();

        Retrofit restAdapter = new Retrofit.Builder()
                .client(client)
                .baseUrl("https://" + API_HOSTNAME + API_BASEURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = restAdapter.create(TUMCabeAPIService.class);
    }

    public static TUMCabeClient getInstance(Context c) {
        TUMCabeClient.context = c.getApplicationContext();
        if (instance == null) {
            instance = new TUMCabeClient();
        }
        return instance;
    }

    public void createRoom(ChatRoom chatRoom, ChatVerification verification, Callback<ChatRoom> cb) {
        verification.setData(chatRoom);
        service.createRoom(verification).enqueue(cb);
    }

    public ChatRoom createRoom(ChatRoom chatRoom, ChatVerification verification) throws IOException {
        verification.setData(chatRoom);
        return service.createRoom(verification).execute().body();
    }

    public ChatRoom getChatRoom(int id) throws IOException {
        return service.getChatRoom(id).execute().body();
    }

    public ChatMember createMember(ChatMember chatMember) throws IOException {
        return service.createMember(chatMember).execute().body();
    }

    public void leaveChatRoom(ChatRoom chatRoom, ChatVerification verification, Callback<ChatRoom> cb) {
        service.leaveChatRoom(chatRoom.getId(), verification).enqueue(cb);
    }

    public ChatMessage sendMessage(int roomId, ChatMessage chatMessageCreate) throws IOException {
        return service.sendMessage(roomId, chatMessageCreate).execute().body();
    }

    public ChatMessage updateMessage(int roomId, ChatMessage message) throws IOException {
        return service.updateMessage(roomId, message.getId(), message).execute().body();
    }

    public ArrayList<ChatMessage> getMessages(int roomId, long messageId, @Body ChatVerification verification) throws IOException {
        return service.getMessages(roomId, messageId, verification).execute().body();
    }

    public ArrayList<ChatMessage> getNewMessages(int roomId, @Body ChatVerification verification) throws IOException {
        return service.getNewMessages(roomId, verification).execute().body();
    }

    public ChatPublicKey uploadPublicKey(int memberId, ChatPublicKey publicKey) throws IOException {
        return service.uploadPublicKey(memberId, publicKey).execute().body();
    }

    public List<ChatRoom> getMemberRooms(int memberId, ChatVerification verification) throws IOException {
        return service.getMemberRooms(memberId, verification).execute().body();
    }

    public void getPublicKeysForMember(ChatMember member, Callback<List<ChatPublicKey>> cb) {
        service.getPublicKeysForMember(member.getId()).enqueue(cb);
    }

    public void uploadRegistrationId(int memberId, ChatRegistrationId regId, Callback<ChatRegistrationId> cb) {
        service.uploadRegistrationId(memberId, regId).enqueue(cb);
    }

    public GCMNotification getNotification(int notification) throws IOException {
        return service.getNotification(notification).execute().body();
    }

    public void confirm(int notification) {
        service.confirm(notification);
    }

    public List<GCMNotificationLocation> getAllLocations() throws IOException {
        return service.getAllLocations().execute().body();
    }

    public GCMNotificationLocation getLocation(int locationId) throws IOException {
        return service.getLocation(locationId).execute().body();
    }

    public List<String> putBugReport(BugReport r) throws IOException {
        return service.putBugReport(r).execute().body();
    }

    public void putStatistics(Statistics s) {
        try {
            service.putStatistics(s).execute();
        } catch (IOException e) {
            //We don't care about any responses or failures
        }
    }

    public void deviceRegister(DeviceRegister verification, Callback<TUMCabeStatus> cb) {
        service.deviceRegister(verification).enqueue(cb);
    }

    public void deviceUploadGcmToken(DeviceUploadGcmToken verification, Callback<TUMCabeStatus> cb) {
        service.deviceUploadGcmToken(verification).enqueue(cb);
    }

    private interface TUMCabeAPIService {

        //Group chat
        @POST(API_CHAT_ROOMS)
        Call<ChatRoom> createRoom(@Body ChatVerification verification);

        @GET(API_CHAT_ROOMS + "{room}")
        Call<ChatRoom> getChatRoom(@Path("room") int id);

        @POST(API_CHAT_ROOMS + "{room}/leave/")
        Call<ChatRoom> leaveChatRoom(@Path("room") int roomId, @Body ChatVerification verification);

        //Get/Update single message
        @PUT(API_CHAT_ROOMS + "{room}/message/")
        Call<ChatMessage> sendMessage(@Path("room") int roomId, @Body ChatMessage message);

        @PUT(API_CHAT_ROOMS + "{room}/message/{message}/")
        Call<ChatMessage> updateMessage(@Path("room") int roomId, @Path("message") int messageId, @Body ChatMessage message);

        //Get all recent messages or older ones
        @POST(API_CHAT_ROOMS + "{room}/messages/{page}/")
        Call<ArrayList<ChatMessage>> getMessages(@Path("room") int roomId, @Path("page") long messageId, @Body ChatVerification verification);

        @POST(API_CHAT_ROOMS + "{room}/messages/")
        Call<ArrayList<ChatMessage>> getNewMessages(@Path("room") int roomId, @Body ChatVerification verification);

        @POST(API_CHAT_MEMBERS)
        Call<ChatMember> createMember(@Body ChatMember chatMember);

        @GET(API_CHAT_MEMBERS + "{lrz_id}/")
        Call<ChatMember> getMember(@Path("lrz_id") String lrzId);

        @POST(API_CHAT_MEMBERS + "{memberId}/pubkeys/")
        Call<ChatPublicKey> uploadPublicKey(@Path("memberId") int memberId, @Body ChatPublicKey publicKey);

        @POST(API_CHAT_MEMBERS + "{memberId}/rooms/")
        Call<List<ChatRoom>> getMemberRooms(@Path("memberId") int memberId, @Body ChatVerification verification);

        @GET(API_CHAT_MEMBERS + "{memberId}/pubkeys/")
        Call<List<ChatPublicKey>> getPublicKeysForMember(@Path("memberId") int memberId);

        @POST(API_CHAT_MEMBERS + "{memberId}/registration_ids/add_id")
        Call<ChatRegistrationId> uploadRegistrationId(@Path("memberId") int memberId, @Body ChatRegistrationId regId);

        @GET(API_NOTIFICATIONS + "{notification}/")
        Call<GCMNotification> getNotification(@Path("notification") int notification);

        @GET(API_NOTIFICATIONS + "confirm/{notification}/")
        Call<String> confirm(@Path("notification") int notification);

        //Locations
        @GET(API_LOCATIONS)
        Call<List<GCMNotificationLocation>> getAllLocations();

        @GET(API_LOCATIONS + "{locationId}/")
        Call<GCMNotificationLocation> getLocation(@Path("locationId") int locationId);

        //Bug Reports
        @PUT(API_REPORT)
        Call<List<String>> putBugReport(@Body BugReport r);

        //Statistics
        @PUT(API_STATISTICS)
        Call<String> putStatistics(@Body Statistics r);

        //Device
        @POST(API_DEVICE + "register/")
        Call<TUMCabeStatus> deviceRegister(@Body DeviceRegister verification);

        @POST(API_DEVICE + "addGcmToken/")
        Call<TUMCabeStatus> deviceUploadGcmToken(@Body DeviceUploadGcmToken verification);
    }
}