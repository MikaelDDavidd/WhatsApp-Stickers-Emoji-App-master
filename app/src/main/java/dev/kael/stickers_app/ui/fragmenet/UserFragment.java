package dev.kael.stickers_app.ui.fragmenet;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

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
import dev.kael.stickers_app.ui.HomeActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * A simple {@link Fragment} subclass.
 */
public class UserFragment extends Fragment {



    // lists
    ArrayList<StickerPack> stickerPacks = new ArrayList<>();
    List<Sticker> mStickers;
    List<String> mEmojis,mDownloadFiles;
    // Object
    StickerAdapter adapter;
    // stattis variables
    private static final String TAG = HomeActivity.class.getSimpleName();
    // views
    private View view;
    private RecyclerView recycler_view_list;
    private LinearLayout linear_layout_layout_error;
    private ImageView image_view_empty_list;
    private SwipeRefreshLayout swipe_refresh_layout_list;
    private Button button_try_again;
    private RelativeLayout relative_layout_load_more;
    private LinearLayoutManager layoutManager;
    private Integer type_ads = 0;

    // variables
    private Integer item = 0;
    private Integer page = 0;
    private Integer position = 0;
    private boolean loading = true;
    private boolean loaded = false;
    private int pastVisiblesItems, visibleItemCount, totalItemCount;

    private int me = -1;
    private Integer user;
    private String type;
    private boolean native_ads_enabled = false;
    private int lines_beetween_ads = 8;


    public UserFragment() {
        // Required empty public constructor
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        user = getArguments().getInt("user");
        type = getArguments().getString("type");
        PrefManager prf= new PrefManager(getActivity().getApplicationContext());
        if (prf.getString("LOGGED").toString().equals("TRUE")) {
            me = Integer.parseInt(prf.getString("ID_USER"));
        }
        this.view =  inflater.inflate(R.layout.fragment_user, container, false);

        stickerPacks = new ArrayList<>();
        mStickers = new ArrayList<>();
        mEmojis = new ArrayList<>();
        mDownloadFiles = new ArrayList<>();
        mEmojis.add("");

        initView();
        initAction();

        LoadPackes();
        return view;
    }


    private void initAction() {
        swipe_refresh_layout_list.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                page = 0;
                item = 0;
                loading = true;

                LoadPackes();
            }
        });
        button_try_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                page = 0;
                item = 0;
                loading = true;
                LoadPackes();
            }
        });

    }

    private void initView() {
        PrefManager prefManager= new PrefManager(getActivity().getApplicationContext());

        if (!prefManager.getString("ADMIN_NATIVE_TYPE").equals("FALSE")){
            native_ads_enabled=true;
            lines_beetween_ads=Integer.parseInt(prefManager.getString("ADMIN_NATIVE_LINES"));
        }
        if (prefManager.getString("SUBSCRIBED").equals("TRUE")) {
            native_ads_enabled=false;
        }

        relative_layout_load_more   = view.findViewById(R.id.relative_layout_load_more);
        button_try_again            = view.findViewById(R.id.button_try_again);
        swipe_refresh_layout_list   = view.findViewById(R.id.swipe_refresh_layout_list);
        image_view_empty_list       = view.findViewById(R.id.image_view_empty_list);
        linear_layout_layout_error  = view.findViewById(R.id.linear_layout_layout_error);
        recycler_view_list          = view.findViewById(R.id.recycler_view_list);

        adapter = new StickerAdapter(getActivity(), stickerPacks);
        layoutManager = new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false);
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
        Call<List<PackApi>> call = service.packsByUser(page,"created",user);
        if (me == user ){
            call = service.packsByMe(page,user);
        }
        call.enqueue(new Callback<List<PackApi>>() {
            @Override
            public void onResponse(Call<List<PackApi>> call, final Response<List<PackApi>> response) {
                apiClient.FormatData(getActivity(),response);
                PrefManager prefManager= new PrefManager(getActivity().getApplicationContext());

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
                        stickerPacks.get(position).review = packApi.getReview();
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
        Call<List<PackApi>> call = service.packsByUser(page,"created",user);

        if (me == user ){
            call = service.packsByMe(page,user);
        }
        call.enqueue(new Callback<List<PackApi>>() {
            @Override
            public void onResponse(Call<List<PackApi>> call, final Response<List<PackApi>> response) {
                PrefManager prefManager= new PrefManager(getActivity().getApplicationContext());

                if (response.isSuccessful()) {
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
                            stickerPacks.get(position).review = packApi.getReview();
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
}
