/*
 * Copyright 2020  Vimal CVS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.kael.stickers_app.ui;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.orhanobut.hawk.Hawk;
import dev.kael.stickers_app.Manager.PrefManager;
import dev.kael.stickers_app.R;
import dev.kael.stickers_app.Sticker;
import dev.kael.stickers_app.StickerPack;
import dev.kael.stickers_app.adapter.StickerAdapter;
import dev.kael.stickers_app.api.apiClient;
import dev.kael.stickers_app.api.apiRest;
import dev.kael.stickers_app.entity.PackApi;
import dev.kael.stickers_app.entity.StickerApi;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SearchActivity extends AppCompatActivity {



    // lists
    ArrayList<StickerPack> stickerPacks = new ArrayList<>();
    List<Sticker> mStickers;
    List<String> mEmojis,mDownloadFiles;
    // Object
    StickerAdapter adapter;
    // stattis variables
    private static final String TAG = HomeActivity.class.getSimpleName();
    // views
    private RecyclerView recycler_view_list;
    private LinearLayout linear_layout_layout_error;
    private ImageView image_view_empty_list;
    private SwipeRefreshLayout swipe_refresh_layout_list;
    private Button button_try_again;
    private RelativeLayout relative_layout_load_more;
    private LinearLayoutManager layoutManager;

    // variables
    private Integer type_ads = 0;
    private Integer page = 0;
    private Integer position = 0;
    private boolean loading = true;
    private boolean loaded = false;
    private int pastVisiblesItems, visibleItemCount, totalItemCount;
    private String query;

    private Integer item = 0 ;
    private Integer lines_beetween_ads = 8 ;
    private Boolean native_ads_enabled = false ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Bundle bundle = getIntent().getExtras() ;
        this.query =  bundle.getString("query");

        stickerPacks = new ArrayList<>();
        mStickers = new ArrayList<>();
        mEmojis = new ArrayList<>();
        mDownloadFiles = new ArrayList<>();
        mEmojis.add("");

        initView();
        initAction();

        showAdsBanner();
        loaded=true;
        item = 0;
        page = 0;
        loading = true;
        LoadPackes();
    }

    private void initAction() {
        swipe_refresh_layout_list.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                item = 0;
                page = 0;
                loading = true;

                LoadPackes();
            }
        });
        button_try_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                item = 0;
                page = 0;
                loading = true;
                LoadPackes();
            }
        });

    }

    private void initView() {


        PrefManager prefManager= new PrefManager(getApplicationContext());

        if (!prefManager.getString("ADMIN_NATIVE_TYPE").equals("FALSE")){
            native_ads_enabled=true;
            lines_beetween_ads=Integer.parseInt(prefManager.getString("ADMIN_NATIVE_LINES"));
        }
        if (prefManager.getString("SUBSCRIBED").equals("TRUE")) {
            native_ads_enabled=false;
        }


        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle(query);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        relative_layout_load_more   = findViewById(R.id.relative_layout_load_more);
        button_try_again            = findViewById(R.id.button_try_again);
        swipe_refresh_layout_list   = findViewById(R.id.swipe_refresh_layout_list);
        image_view_empty_list       = findViewById(R.id.image_view_empty_list);
        linear_layout_layout_error  = findViewById(R.id.linear_layout_layout_error);
        recycler_view_list          = findViewById(R.id.recycler_view_list);

        adapter = new StickerAdapter(this, stickerPacks);
        layoutManager = new LinearLayoutManager(this.getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        recycler_view_list.setHasFixedSize(true);
        recycler_view_list.setAdapter(adapter);
        recycler_view_list.setLayoutManager(layoutManager);
        recycler_view_list.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                if(dy > 0) //check for scroll down
                {

                    visibleItemCount    = layoutManager.getChildCount();
                    totalItemCount      = layoutManager.getItemCount();
                    pastVisiblesItems   = layoutManager.findFirstVisibleItemPosition();

                    if (loading)
                    {
                        if ( (visibleItemCount + pastVisiblesItems) >= totalItemCount)
                        {
                            loading = false;
                            LoadNextPackes();
                        }
                    }
                }else{

                }
            }
        });

    }
    public void LoadNextPackes(){
        relative_layout_load_more.setVisibility(View.VISIBLE);
        Retrofit retrofit = apiClient.getClient();
        apiRest service = retrofit.create(apiRest.class);
        Call<List<PackApi>> call =  service.packsByQuery(page,query);
        call.enqueue(new Callback<List<PackApi>>() {
            @Override
            public void onResponse(Call<List<PackApi>> call, final Response<List<PackApi>> response) {
                apiClient.FormatData(SearchActivity.this,response);
                PrefManager prefManager= new PrefManager(getApplicationContext());

                if (response.isSuccessful()) {
                    for (int i = 0; i < response.body().size(); i++) {
                        PackApi packApi= response.body().get(i);
                        stickerPacks.add(new StickerPack(
                                packApi.getIdentifier()+"",
                                packApi.getName(),
                                packApi.getPublisher(),
                                getLastBitFromUrl(packApi.getTrayImageFile()).replace(" ","_"),
                                packApi.getTrayImageFile(),
                                packApi.getSize(),
                                packApi.getDownloads(),
                                packApi.getPremium(),
                                packApi.getTrusted(),
                                packApi.getCreated(),
                                packApi.getUser(),
                                packApi.getUserimage(),
                                packApi.getUserid(),
                                packApi.getPublisherEmail(),
                                packApi.getPublisherWebsite(),
                                packApi.getPrivacyPolicyWebsite(),
                                packApi.getLicenseAgreementWebsite()
                        ));
                        List<StickerApi> stickerApiList =  packApi.getStickers();
                        for (int j = 0; j < stickerApiList.size(); j++) {
                            StickerApi stickerApi = stickerApiList.get(j);
                            mStickers.add(new Sticker(
                                    stickerApi.getImageFileThum(),
                                    stickerApi.getImageFile(),
                                    getLastBitFromUrl(stickerApi.getImageFile()).replace(".png",".webp"),
                                    mEmojis
                            ));
                            mDownloadFiles.add(stickerApi.getImageFile());
                        }
                        Hawk.put(packApi.getIdentifier()+"", mStickers);
                        stickerPacks.get(position).setStickers(Hawk.get(packApi.getIdentifier()+"",new ArrayList<Sticker>()));
                        stickerPacks.get(position).packApi = packApi;
                        mStickers.clear();
                        position++;

                        if (native_ads_enabled){
                            item++;
                            if (item == lines_beetween_ads ){
                                item= 0;
                                if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("ADMOB")) {
                                    stickerPacks.add(new StickerPack().setViewType(6));
                                    position++;
                                }else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("FACEBOOK")){
                                    stickerPacks.add(new StickerPack().setViewType(4));
                                    position++;
                                } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("BOTH")){
                                    if (type_ads == 0) {
                                        stickerPacks.add(new StickerPack().setViewType(6));
                                        position++;
                                        type_ads = 1;
                                    }else if (type_ads == 1){
                                        stickerPacks.add(new StickerPack().setViewType(4));
                                        position++;
                                        type_ads = 0;
                                    }
                                }
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    page++;
                    loading=true;

                }
                relative_layout_load_more.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<List<PackApi>> call, Throwable t) {
                relative_layout_load_more.setVisibility(View.GONE);
            }
        });
    }
    public void LoadPackes(){

        recycler_view_list.setVisibility(View.VISIBLE);
        linear_layout_layout_error.setVisibility(View.GONE);
        image_view_empty_list.setVisibility(View.GONE);
        swipe_refresh_layout_list.setRefreshing(true);

        Retrofit retrofit = apiClient.getClient();
        apiRest service = retrofit.create(apiRest.class);
        Call<List<PackApi>> call = service.packsByQuery(page,query);
        call.enqueue(new Callback<List<PackApi>>() {
            @Override
            public void onResponse(Call<List<PackApi>> call, final Response<List<PackApi>> response) {
                if (response.isSuccessful()) {
                    PrefManager prefManager= new PrefManager(getApplicationContext());

                    if (response.body().size()!=0) {
                        position = 0;
                        stickerPacks.clear();
                        mStickers.clear();
                        mEmojis.clear();
                        mDownloadFiles.clear();
                        mEmojis.add("");
                        adapter.notifyDataSetChanged();

                        for (int i = 0; i < response.body().size(); i++) {
                            PackApi packApi = response.body().get(i);
                            stickerPacks.add(new StickerPack(
                                    packApi.getIdentifier() + "",
                                    packApi.getName(),
                                    packApi.getPublisher(),
                                    getLastBitFromUrl(packApi.getTrayImageFile()).replace(" ", "_"),
                                    packApi.getTrayImageFile(),
                                    packApi.getSize(),
                                    packApi.getDownloads(),
                                    packApi.getPremium(),
                                    packApi.getTrusted(),
                                    packApi.getCreated(),
                                    packApi.getUser(),
                                    packApi.getUserimage(),
                                    packApi.getUserid(),
                                    packApi.getPublisherEmail(),
                                    packApi.getPublisherWebsite(),
                                    packApi.getPrivacyPolicyWebsite(),
                                    packApi.getLicenseAgreementWebsite()
                            ));
                            List<StickerApi> stickerApiList = packApi.getStickers();
                            for (int j = 0; j < stickerApiList.size(); j++) {
                                StickerApi stickerApi = stickerApiList.get(j);
                                mStickers.add(new Sticker(
                                        stickerApi.getImageFileThum(),
                                        stickerApi.getImageFile(),
                                        getLastBitFromUrl(stickerApi.getImageFile()).replace(".png", ".webp"),
                                        mEmojis
                                ));
                                mDownloadFiles.add(stickerApi.getImageFile());
                            }
                            Hawk.put(packApi.getIdentifier() + "", mStickers);
                            stickerPacks.get(position).setStickers(Hawk.get(packApi.getIdentifier() + "", new ArrayList<Sticker>()));
                            stickerPacks.get(position).packApi = packApi;

                            mStickers.clear();
                            position++;

                            if (native_ads_enabled){
                                item++;
                                if (item == lines_beetween_ads ){
                                    item= 0;
                                    if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("ADMOB")) {
                                        stickerPacks.add(new StickerPack().setViewType(6));
                                        position++;
                                    }else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("FACEBOOK")){
                                        stickerPacks.add(new StickerPack().setViewType(4));
                                        position++;
                                    } else if (prefManager.getString("ADMIN_NATIVE_TYPE").equals("BOTH")){
                                        if (type_ads == 0) {
                                            stickerPacks.add(new StickerPack().setViewType(6));
                                            position++;
                                            type_ads = 1;
                                        }else if (type_ads == 1){
                                            stickerPacks.add(new StickerPack().setViewType(4));
                                            position++;
                                            type_ads = 0;
                                        }
                                    }
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        page++;

                        recycler_view_list.setVisibility(View.VISIBLE);
                        image_view_empty_list.setVisibility(View.GONE);
                        linear_layout_layout_error.setVisibility(View.GONE);
                    }else{
                        recycler_view_list.setVisibility(View.GONE);
                        image_view_empty_list.setVisibility(View.VISIBLE);
                        linear_layout_layout_error.setVisibility(View.GONE);
                    }
                } else {
                    recycler_view_list.setVisibility(View.GONE);
                    image_view_empty_list.setVisibility(View.GONE);
                    linear_layout_layout_error.setVisibility(View.VISIBLE);
                }
                swipe_refresh_layout_list.setRefreshing(false);

            }

            @Override
            public void onFailure(Call<List<PackApi>> call, Throwable t) {
                swipe_refresh_layout_list.setRefreshing(false);
                recycler_view_list.setVisibility(View.GONE);
                image_view_empty_list.setVisibility(View.GONE);
                linear_layout_layout_error.setVisibility(View.VISIBLE);
            }
        });
    }
    private static String getLastBitFromUrl(final String url) {
        return url.replaceFirst(".*/([^/?]+).*", "$1");
    }
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
        return;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
        }
        return super.onOptionsItemSelected(item);
    }
    public boolean checkSUBSCRIBED(){
        PrefManager prefManager= new PrefManager(getApplicationContext());
        if (!prefManager.getString("SUBSCRIBED").equals("TRUE")) {
            return false;
        }
        return true;
    }
    public void showAdsBanner() {
        if (!checkSUBSCRIBED()) {
            showAdmobBanner();
        }

    }
    public void showAdmobBanner(){
        PrefManager prefManager= new PrefManager(getApplicationContext());
        LinearLayout linear_layout_ads =  (LinearLayout) findViewById(R.id.linear_layout_ads);
        final AdView mAdView = new AdView(this);
        mAdView.setAdSize(AdSize.SMART_BANNER);
        mAdView.setAdUnitId(prefManager.getString("ADMIN_BANNER_ADMOB_ID"));
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mAdView.loadAd(adRequest);
        linear_layout_ads.addView(mAdView);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                mAdView.setVisibility(View.VISIBLE);
            }
        });
    }
}
