package io.github.yahia_hassan.popularmoviesstage2;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import io.github.yahia_hassan.popularmoviesstage2.POJOs.Movie;
import io.github.yahia_hassan.popularmoviesstage2.data.MovieContract;
import io.github.yahia_hassan.popularmoviesstage2.databinding.ActivityDetailsBinding;
import io.github.yahia_hassan.popularmoviesstage2.loaders.ReviewAsyncTaskLoader;
import io.github.yahia_hassan.popularmoviesstage2.loaders.VideosAsyncTaskLoader;

public class DetailsActivity extends AppCompatActivity {



    public static final int REVIEW_LOADER_ID = 40;
    public static final int VIDEO_LOADER_ID = 41;

    private static final String TAG = DetailsActivity.class.getSimpleName();


    Movie mClickedMovie;

    private LinearLayoutManager mUserReviewLinearLayoutManager;
    private LinearLayoutManager mVideoLinearLayoutManager;
    ActivityDetailsBinding mActivityDetailsBinding;
    private int mExtraMessageCode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        mActivityDetailsBinding = DataBindingUtil.setContentView(this, R.layout.activity_details);
        Intent intent = getIntent();
        mExtraMessageCode = intent.getIntExtra(UriConstants.EXTRA_MESSAGE, UriConstants.MAIN_ACTIVITY_ASYNCTASK_LOADER);

        mUserReviewLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mVideoLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);


        mClickedMovie = intent.getParcelableExtra(UriConstants.PARCELABLE_EXTRA_MESSAGE);

        mActivityDetailsBinding.setMovie(mClickedMovie);

        if (mExtraMessageCode == UriConstants.MAIN_ACTIVITY_CURSOR_LOADER) {
            mActivityDetailsBinding.videosRecyclerView.setVisibility(View.GONE);
            mActivityDetailsBinding.usersReviewRecyclerView.setVisibility(View.GONE);
            mActivityDetailsBinding.videosLabel.setVisibility(View.GONE);
            mActivityDetailsBinding.userReviewLabel.setVisibility(View.GONE);

            Uri.Builder builder = new Uri.Builder();
            builder.scheme(UriConstants.SCHEME)
                    .authority(UriConstants.IMAGE_AUTHORITY)
                    .appendPath(UriConstants.IMAGE_T_PATH)
                    .appendPath(UriConstants.IMAGE_P_PATH)
                    .appendPath(UriConstants.IMAGE_WIDTH_PATH)
                    .appendEncodedPath(mClickedMovie.getMoviePosterBackdrop());
            final String imageUrl = builder.build().toString();

            // https://stackoverflow.com/a/34051356
            Picasso.with(getBaseContext())
                    .load(imageUrl)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(mActivityDetailsBinding.moviePosterIv, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError() {
                            //Try again online if cache failed
                            Picasso.with(getBaseContext())
                                    .load(imageUrl)
                                    .error(R.color.placeholder_grey)
                                    .into(mActivityDetailsBinding.moviePosterIv, new Callback() {
                                        @Override
                                        public void onSuccess() {

                                        }

                                        @Override
                                        public void onError() {
                                            Log.v("FavoriteAdapter Picasso", "Could not fetch image");
                                        }
                                    });
                        }
                    });

        }


        if (checkIfInFavoritesList()) {
            mActivityDetailsBinding.favoriteFab.setImageResource(R.drawable.ic_favorite);
        }

        if (Helper.isNetworkAvailable(this) && mExtraMessageCode == UriConstants.MAIN_ACTIVITY_ASYNCTASK_LOADER) {

            makeNetworkRequest();


        } else if (!Helper.isNetworkAvailable(this) && mExtraMessageCode == UriConstants.MAIN_ACTIVITY_ASYNCTASK_LOADER) {
            showNoNetworkError();
        }

        mActivityDetailsBinding.detailsActivityRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartLoaders();
            }
        });

        mActivityDetailsBinding.favoriteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAndDeleteMoviesFromTheDatabase();
            }
        });
    }



    private void restartLoaders() {
        if (Helper.isNetworkAvailable(this) && mExtraMessageCode == UriConstants.MAIN_ACTIVITY_ASYNCTASK_LOADER) {
            showData();
            makeNetworkRequest();
        } else if (!Helper.isNetworkAvailable(this) && mExtraMessageCode == UriConstants.MAIN_ACTIVITY_ASYNCTASK_LOADER) {
            showNoNetworkError();
        }
    }

    private void makeNetworkRequest() {
        VideosAsyncTaskLoader videosAsyncTaskLoader = new VideosAsyncTaskLoader(this,
                mClickedMovie,
                mActivityDetailsBinding.videosRecyclerView,
                mVideoLinearLayoutManager);

        getSupportLoaderManager().initLoader(VIDEO_LOADER_ID, null, videosAsyncTaskLoader);


        ReviewAsyncTaskLoader reviewAsyncTaskLoader = new ReviewAsyncTaskLoader(this,
                mClickedMovie,
                mActivityDetailsBinding.usersReviewRecyclerView,
                mUserReviewLinearLayoutManager);

        getSupportLoaderManager().initLoader(REVIEW_LOADER_ID, null, reviewAsyncTaskLoader);




        /*
         * I searched online how to set the title of the activity, found the answer on this
          * Stack Overflow answer ( https://stackoverflow.com/a/2198569/5255289 )
         */
        setTitle(mActivityDetailsBinding.getMovie().getMovieTitle());



        Uri.Builder builder = new Uri.Builder();
        builder.scheme(UriConstants.SCHEME)
                .authority(UriConstants.IMAGE_AUTHORITY)
                .appendPath(UriConstants.IMAGE_T_PATH)
                .appendPath(UriConstants.IMAGE_P_PATH)
                .appendPath(UriConstants.IMAGE_WIDTH_PATH)
                .appendEncodedPath(mClickedMovie.getMoviePosterBackdrop());
        String url = builder.build().toString();

        Picasso.with(this)
                .load(url)
                .placeholder(R.color.placeholder_grey)
                .into(mActivityDetailsBinding.moviePosterIv);

        Log.d(TAG, "Image URL is: " + url);

    }

    private void showNoNetworkError() {
        mActivityDetailsBinding.originalTitleTv.setVisibility(View.GONE);
        mActivityDetailsBinding.moviePosterIv.setVisibility(View.GONE);
        mActivityDetailsBinding.plotSynopsisTv.setVisibility(View.GONE);
        mActivityDetailsBinding.userRatingTv.setVisibility(View.GONE);
        mActivityDetailsBinding.releaseDateTv.setVisibility(View.GONE);


        mActivityDetailsBinding.usersReviewRecyclerView.setVisibility(View.GONE);
        mActivityDetailsBinding.videosRecyclerView.setVisibility(View.GONE);


        mActivityDetailsBinding.userRatingLabel.setVisibility(View.GONE);
        mActivityDetailsBinding.releaseDateLabel.setVisibility(View.GONE);
        mActivityDetailsBinding.plotSynopsisLabel.setVisibility(View.GONE);
        mActivityDetailsBinding.videosLabel.setVisibility(View.GONE);
        mActivityDetailsBinding.userReviewLabel.setVisibility(View.GONE);

        mActivityDetailsBinding.detailsActivityNoNetworkTv.setVisibility(View.VISIBLE);
        mActivityDetailsBinding.detailsActivityRetryButton.setVisibility(View.VISIBLE);
    }

    private void showData() {
        mActivityDetailsBinding.originalTitleTv.setVisibility(View.VISIBLE);
        mActivityDetailsBinding.moviePosterIv.setVisibility(View.VISIBLE);
        mActivityDetailsBinding.plotSynopsisTv.setVisibility(View.VISIBLE);
        mActivityDetailsBinding.userRatingTv.setVisibility(View.VISIBLE);
        mActivityDetailsBinding.releaseDateTv.setVisibility(View.VISIBLE);


        mActivityDetailsBinding.usersReviewRecyclerView.setVisibility(View.VISIBLE);
        mActivityDetailsBinding.videosRecyclerView.setVisibility(View.VISIBLE);


        mActivityDetailsBinding.userRatingLabel.setVisibility(View.VISIBLE);
        mActivityDetailsBinding.releaseDateLabel.setVisibility(View.VISIBLE);
        mActivityDetailsBinding.plotSynopsisLabel.setVisibility(View.VISIBLE);
        mActivityDetailsBinding.videosLabel.setVisibility(View.VISIBLE);
        mActivityDetailsBinding.userReviewLabel.setVisibility(View.VISIBLE);

        mActivityDetailsBinding.detailsActivityNoNetworkTv.setVisibility(View.GONE);
        mActivityDetailsBinding.detailsActivityRetryButton.setVisibility(View.GONE);
    }

    private boolean checkIfInFavoritesList() {
        String[] projection = {MovieContract.MovieEntry.COLUMN_MOVIE_TITLE};
        String[] selectionArgs = {mClickedMovie.getMovieTitle()};
        Uri uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.PATH_MOVIES);
        Cursor cursor = getContentResolver().query(
                uri,
                projection,
                MovieContract.MovieEntry.COLUMN_MOVIE_TITLE + "=?",
                selectionArgs,
                null
        );
        if (cursor.getCount() <= 0) {
            cursor.close();
            return false;
        }
        cursor.close();
        return true;

    }

    private void addAndDeleteMoviesFromTheDatabase() {
        if (checkIfInFavoritesList()) {
            Uri uri = Uri.parse(MovieContract.BASE_CONTENT_URI + "/" + MovieContract.PATH_MOVIES + "/" + mClickedMovie.getMovieId());
            getContentResolver().delete(uri, null, null);
            mActivityDetailsBinding.favoriteFab.setImageResource(R.drawable.ic_favorite_border);
        } else {
            ContentValues values = new ContentValues();

            values.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, mClickedMovie.getMovieId());
            values.put(MovieContract.MovieEntry.COLUMN_MOVIE_TITLE, mClickedMovie.getMovieTitle());
            values.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, mClickedMovie.getMoviePoster());
            values.put(MovieContract.MovieEntry.COLUMN_BACKDROP_PATH, mClickedMovie.getMoviePosterBackdrop());
            values.put(MovieContract.MovieEntry.COLUMN_PLOT_SYNOPSIS, mClickedMovie.getPlotSynopsis());
            values.put(MovieContract.MovieEntry.COLUMN_USER_RATING, mClickedMovie.getUserRating());
            values.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, mClickedMovie.getReleaseDate());

            getContentResolver().insert(MovieContract.BASE_CONTENT_URI, values);

            mActivityDetailsBinding.favoriteFab.setImageResource(R.drawable.ic_favorite);
        }
    }

}
