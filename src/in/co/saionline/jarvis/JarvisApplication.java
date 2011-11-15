package in.co.saionline.jarvis;

import greendroid.app.GDApplication;

public class JarvisApplication extends GDApplication {

    @Override
     public Class<?> getHomeActivityClass() {
        return DashboardActivity.class;
    }
}
