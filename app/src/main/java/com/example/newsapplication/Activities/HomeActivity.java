package com.example.newsapplication.Activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newsapplication.API.ApiClient;
import com.example.newsapplication.API.ApiInterface;
import com.example.newsapplication.Adapter.NewsAdapter;
import com.example.newsapplication.Model.Article;
import com.example.newsapplication.Model.News;
import com.example.newsapplication.R;
import com.example.newsapplication.Utility.Utils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    public static final String API_KEY = "715c6e9621ba41cdb999aedd36675eb6";
    private RecyclerView rvHome;
    RecyclerView.LayoutManager layoutManager;
    private List<Article> articles = new ArrayList<>();
    private NewsAdapter newsAdapter;
    private String TAG = HomeActivity.class.getSimpleName();
    private SwipeRefreshLayout sRLHome;
    private TextView tvTopHeadLines;
    private RelativeLayout rlEP;
    private ImageView ivError;
    private TextView tvErrorMessage, tvErrorTitle;
    private Button btnRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sRLHome = findViewById(R.id.swipeRefreshLayout);
        sRLHome.setOnRefreshListener(this);
        sRLHome.setColorSchemeResources(R.color.colorAccent);


        rvHome = findViewById(R.id.recyclerViewHome);
        tvTopHeadLines = findViewById(R.id.textViewTopHeadLines);
        layoutManager = new LinearLayoutManager(HomeActivity.this);
        rvHome.setLayoutManager(layoutManager);
        rvHome.setItemAnimator(new DefaultItemAnimator());
        rvHome.setNestedScrollingEnabled(false);

        onLoadingSwipeRefresh("");

        rlEP = findViewById(R.id.relativeLayoutPE);
        ivError = findViewById(R.id.imageViewError);
        tvErrorTitle = findViewById(R.id.textViewErrorTitle);
        tvErrorMessage = findViewById(R.id.textViewErrorMessage);
        btnRetry = findViewById(R.id.buttonRetry);
    }

    public void loadJson(final String keyword) {

        rlEP.setVisibility(View.GONE);
        sRLHome.setRefreshing(true);

        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
        String country = Utils.getCountry();
        String language = Utils.getLanguage();

        Call<News> call;

        if (keyword.length() > 0) call = apiInterface.getNewsSearch(keyword, language, "publishAt", API_KEY);
        else call = apiInterface.getNews(country, API_KEY);

        call.enqueue(new Callback<News>() {
            @Override
            public void onResponse(Call<News> call, Response<News> response) {
                if ((response.isSuccessful()) && (response.body().getArticles() != null)) {
                    if (!articles.isEmpty()) articles.clear();

                    articles = response.body().getArticles();
                    newsAdapter = new NewsAdapter(articles, HomeActivity.this);
                    rvHome.setAdapter(newsAdapter);
                    newsAdapter.notifyDataSetChanged();

                    initListner();

                    tvTopHeadLines.setVisibility(View.VISIBLE);
                    sRLHome.setRefreshing(false);

                } else {
                    tvTopHeadLines.setVisibility(View.INVISIBLE);
                    sRLHome.setRefreshing(false);

                    String errorCode;
                    switch (response.code()) {
                        case 404:
                            errorCode = "404 Not Found";
                            break;
                        case 500:
                            errorCode = "505 Server Broken";
                            break;
                        default:
                            errorCode = "Unknown Error";
                            break;
                    }

                    showErrorMessage(R.drawable.no_result, "No Result", "Please try again!\n" + errorCode);
                }
            }

            @Override
            public void onFailure(Call<News> call, Throwable t) {
                tvTopHeadLines.setVisibility(View.INVISIBLE);
                sRLHome.setRefreshing(false);
                showErrorMessage(R.drawable.no_result, "Oops...", "Network Failure, Please try again\n" + t.toString());
            }
        });
    }

    private void initListner() {
        newsAdapter.setOnItemClickListener(new NewsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ImageView imageView = view.findViewById(R.id.imageViewNews);

                Intent intent = new Intent(HomeActivity.this, NewsDetailActivity.class);

                Article article = articles.get(position);
                intent.putExtra("url", article.getUrl());
                intent.putExtra("author", article.getAuthor());
                intent.putExtra("title", article.getTitle());
                intent.putExtra("image",article.getUrlToImage());
                intent.putExtra("date", article.getPublishedAt());
                intent.putExtra("source",article.getSource().getName());

                Pair<View, String> pair = Pair.create((View)imageView, ViewCompat.getTransitionName(imageView));
                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(HomeActivity.this,pair);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startActivity(intent, optionsCompat.toBundle());
                } else {
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.news_menu, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint("Search Latest News...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.length() > 2)
                    onLoadingSwipeRefresh(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchMenuItem.getIcon().setVisible(false, false);
        return true;
    }

    @Override
    public void onRefresh() { loadJson(""); }

    private void onLoadingSwipeRefresh(final String keywod) {
        sRLHome.post(new Runnable() {
            @Override
            public void run() {
                loadJson(keywod);
            }
        });
    }

    private void showErrorMessage (int imageView, String title, String message) {
        if (rlEP.getVisibility() == View.GONE) rlEP.setVisibility(View.VISIBLE);

        ivError.setImageResource(imageView);
        tvErrorTitle.setText(title);
        tvErrorMessage.setText(message);

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoadingSwipeRefresh("");
            }
        });
    }
}