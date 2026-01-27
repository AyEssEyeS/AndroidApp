package cod.ayesseye.textron0;

import android.app.Application;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

// Clasa Singleton care gestioneaza ciclul de viata al aplicatiei si coada de retea
public class AppController extends  Application{
    public static final String TAG = AppController.class.getSimpleName();

    private static AppController mInstance;
    private RequestQueue mRequestQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    // Returneaza instanta curenta a aplicatiei pentru acces global
    public static synchronized AppController getInstance() {
        return mInstance;
    }

    // Initializeaza si returneaza coada de cereri Volley (Singleton pattern)
    // Asigura ca exista o singura coada de retea pentru toata aplicatia
    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return mRequestQueue;
    }

    // Adauga o noua cerere de retea (API call) in coada globala de executie
    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

}