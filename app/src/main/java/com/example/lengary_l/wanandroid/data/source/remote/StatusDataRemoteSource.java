package com.example.lengary_l.wanandroid.data.source.remote;

import android.support.annotation.NonNull;

import com.example.lengary_l.wanandroid.data.LoginDetailData;
import com.example.lengary_l.wanandroid.data.Status;
import com.example.lengary_l.wanandroid.data.source.StatusDataSource;
import com.example.lengary_l.wanandroid.realm.RealmHelper;
import com.example.lengary_l.wanandroid.retrofit.RetrofitClient;
import com.example.lengary_l.wanandroid.retrofit.RetrofitService;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;

public class StatusDataRemoteSource implements StatusDataSource {
    @NonNull
    private static StatusDataRemoteSource INSTANCE;
    private StatusDataRemoteSource() {

    }

    public static StatusDataRemoteSource getInstance(){
        if (INSTANCE == null) {
            INSTANCE = new StatusDataRemoteSource();
        }
        return INSTANCE;
    }


    @Override
    public Observable<Status> collectArticle(final int userId, final int id) {
        return RetrofitClient.getInstance()
                .create(RetrofitService.class)
                .collectArticle(id)
                .filter(new Predicate<Status>() {
                    @Override
                    public boolean test(Status status) throws Exception {
                        return status.getErrorCode() != -1;
                    }
                })
                .doOnNext(new Consumer<Status>() {
                    @Override
                    public void accept(Status status) throws Exception {
                        Realm realm = Realm.getInstance(new RealmConfiguration.Builder()
                                .deleteRealmIfMigrationNeeded()
                                .name(RealmHelper.DATABASE_NAME)
                                .build());
                        LoginDetailData data = realm.copyFromRealm(
                                realm.where(LoginDetailData.class)
                                        .equalTo("id", userId)
                                        .findFirst()
                        );
                        RealmList<Integer> collectIds = data.getCollectIds();
                        if (!checkIsFavorite(id, collectIds)) {
                            collectIds.add(id);
                            data.setCollectIds(collectIds);
                            realm.beginTransaction();
                            realm.copyToRealmOrUpdate(data);
                            realm.commitTransaction();
                            realm.close();
                        }
                    }
                });
    }

    @Override
    public Observable<Status> uncollectArticle(final int userId, final int originId) {
        return RetrofitClient.getInstance()
                .create(RetrofitService.class)
                .uncollectArticle(originId)
                .filter(new Predicate<Status>() {
                    @Override
                    public boolean test(Status status) throws Exception {
                        return status.getErrorCode() != -1;
                    }
                })
                .doOnNext(new Consumer<Status>() {
                    @Override
                    public void accept(Status status) throws Exception {
                        Realm realm = Realm.getInstance(new RealmConfiguration.Builder()
                                .deleteRealmIfMigrationNeeded()
                                .name(RealmHelper.DATABASE_NAME)
                                .build());
                        LoginDetailData data = realm.copyFromRealm(
                                realm.where(LoginDetailData.class)
                                        .equalTo("id", userId)
                                        .findFirst()
                        );
                        RealmList<Integer> collectIds = data.getCollectIds();
                        if (checkIsFavorite(originId, collectIds)) {
                            collectIds.remove(originId);
                            data.setCollectIds(collectIds);
                            realm.beginTransaction();
                            realm.copyToRealmOrUpdate(data);
                            realm.commitTransaction();
                            realm.close();
                        }
                    }
                });
    }

    private boolean checkIsFavorite(int articleIds, RealmList<Integer> collectIds) {
        if (collectIds.isEmpty()) {
            return false;
        }
        for (Integer integer : collectIds) {
            if (integer == articleIds) {
                return true;
            }
        }
        return false;
    }

}
