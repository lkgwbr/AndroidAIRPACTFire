package edu.wsu.lar.airpact_fire.data.realm.object;

import java.util.Date;

import edu.wsu.lar.airpact_fire.Reference;
import edu.wsu.lar.airpact_fire.data.manager.DataManager;
import edu.wsu.lar.airpact_fire.data.object.PostObject;
import edu.wsu.lar.airpact_fire.data.object.UserObject;
import edu.wsu.lar.airpact_fire.data.realm.model.Coordinate;
import edu.wsu.lar.airpact_fire.data.realm.model.Post;
import edu.wsu.lar.airpact_fire.data.realm.model.User;
import edu.wsu.lar.airpact_fire.debug.manager.DebugManager;
import io.realm.Realm;

/**
 * @see UserObject
 */
public class RealmUserObject implements UserObject {

    private Realm mRealm;
    private String mUsername;
    private DataManager mDataManager;
    private DebugManager mDebugManager;

    public RealmUserObject(Realm realm, String username, DataManager dataManager,
                           DebugManager debugManager) {
        mRealm = realm;
        mUsername = username;
        mDataManager = dataManager;
        mDebugManager = debugManager;
    }

    public RealmUserObject(Realm realm, User userModel, DataManager dataManager,
                           DebugManager debugManager) {
        this(realm, userModel.username, dataManager, debugManager);
    }

    @Override
    public String getUsername() {
        return mUsername;
    }

    @Override
    public String getPassword() {
        return mRealm.where(User.class).equalTo("username", mUsername).findFirst().password;
    }

    @Override
    // TODO: Forward to target selection activity when there is an active draft post
    public boolean getHasDraftPost() {
        return false;
    }

    @Override
    public void setHasDraftPost(boolean value) {
        // TODO
    }

    @Override
    public PostObject[] getPosts() {
        return new PostObject[0];
    }

    @Override
    public PostObject getLastPost() {
        Post post = mRealm.where(Post.class).equalTo("user.username", mUsername)
                .findAllSorted("date").first();
        return new RealmPostObject(mRealm, post, mDataManager, mDebugManager);
    }

    @Override
    public PostObject createPost() {
        mRealm.beginTransaction();
        mDebugManager.printLog("before postModel");
        mDebugManager.printLog("" + mDataManager.generatePostId());
        Post postModel = mRealm.createObject(Post.class, mDataManager.generatePostId());
        mDebugManager.printLog("after postModel");
        postModel.mode = Reference.PostMode.DRAFTED.ordinal();
        mRealm.commitTransaction();
        return new RealmPostObject(mRealm, postModel, mDataManager, mDebugManager);
    }

    @Override
    public int getDistanceMetric() {
        return mRealm.where(User.class).equalTo("username", mUsername).findFirst().distanceMetric;
    }

    @Override
    public void setDistanceMetric(int value) {
        mRealm.beginTransaction();
        mRealm.where(User.class).equalTo("username", mUsername).findFirst().distanceMetric = value;
        mRealm.commitTransaction();
    }

    @Override
    public Date getFirstLoginDate() {
        return null;
    }

    @Override
    public void getFirstLoginDate(Date value) {

    }
}