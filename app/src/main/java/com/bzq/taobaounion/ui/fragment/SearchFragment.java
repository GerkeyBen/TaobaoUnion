package com.bzq.taobaounion.ui.fragment;

import android.content.Intent;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bzq.taobaounion.R;
import com.bzq.taobaounion.base.BaseFragment;
import com.bzq.taobaounion.model.DTO.Histories;
import com.bzq.taobaounion.model.DTO.SearchRecommend;
import com.bzq.taobaounion.model.DTO.SearchResult;
import com.bzq.taobaounion.presenter.ISearchPresenter;
import com.bzq.taobaounion.presenter.ITicketPresenter;
import com.bzq.taobaounion.ui.activity.TicketActivity;
import com.bzq.taobaounion.ui.adapter.SearchResultAdapter;
import com.bzq.taobaounion.ui.custom.TextFlowLayout;
import com.bzq.taobaounion.utils.KeyBoardUtils;
import com.bzq.taobaounion.utils.LogUtils;
import com.bzq.taobaounion.utils.PresenterManager;
import com.bzq.taobaounion.utils.SizeUtils;
import com.bzq.taobaounion.utils.ToastUtils;
import com.bzq.taobaounion.view.ISearchPageCallback;
import com.lcodecore.tkrefreshlayout.RefreshListenerAdapter;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;

/**
 * @author Gerkey
 * Created on 2021/6/27
 */
public class SearchFragment extends BaseFragment implements ISearchPageCallback, TextFlowLayout.OnFlowTextItemClickListener {

    @BindView(R.id.search_history_view)
    public TextFlowLayout mHistoryView;

    @BindView(R.id.search_recommend_view)
    public TextFlowLayout mRecommendView;

    @BindView(R.id.search_history_container)
    public View mHistoryContainer;

    @BindView(R.id.search_recommend_container)
    public View mRecommendContainer;

    @BindView(R.id.search_history_delete)
    public ImageView mHistoryDelete;

    @BindView(R.id.search_result_list)
    public RecyclerView mSearchList;

    @BindView(R.id.search_result_container)
    public TwinklingRefreshLayout mRefreshContainer;

    @BindView(R.id.search_input_box)
    public EditText mSearchInputBox;

    @BindView(R.id.search_cancel_btn)
    public ImageView mCancelInputBtn;

    @BindView(R.id.search_btn)
    public TextView mSearchBtn;


    private ISearchPresenter mSearchPresenter;
    private SearchResultAdapter mSearchResultAdapter;

    @Override
    protected void initView(View rootView) {
        setUpState(State.SUCCESS);
        mSearchList.setLayoutManager(new LinearLayoutManager(getContext()));
        mSearchResultAdapter = new SearchResultAdapter();
        mSearchList.setAdapter(mSearchResultAdapter);
        mSearchList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull @NotNull Rect outRect, @NonNull @NotNull View view, @NonNull @NotNull RecyclerView parent, @NonNull @NotNull RecyclerView.State state) {
                outRect.top = SizeUtils.dp2px(Objects.requireNonNull(getContext()), 2);
                outRect.bottom = SizeUtils.dp2px(Objects.requireNonNull(getContext()), 2);
            }
        });

        mRefreshContainer.setEnableLoadmore(true);
        mRefreshContainer.setEnableRefresh(false);
        mRefreshContainer.setEnableOverScroll(true);

    }

    @Override
    protected void initPresenter() {
        mSearchPresenter = PresenterManager.getInstance().getSearchPresenter();
        mSearchPresenter.registerViewCallback(this);
        // ?????????????????????
        mSearchPresenter.getRecommendWords();
        mSearchPresenter.getHistories();
//        mSearchPresenter.doSearch("??????");
    }

    @Override
    protected void initListener() {
        mHistoryView.setOnFlowTextItemClickListener(this);
        mRecommendView.setOnFlowTextItemClickListener(this);
        mSearchBtn.setOnClickListener(v -> {
            // ?????????????????????????????????????????????????????????
            if (hasContent(false)) {
                // ????????????
                if (mSearchPresenter != null) {
                    String keyWord = mSearchInputBox.getText().toString().trim();
                    KeyBoardUtils.hide(Objects.requireNonNull(getContext()), v);
                    toSearch(keyWord);
                }
            } else {
                KeyBoardUtils.hide(Objects.requireNonNull(getContext()), v);
                mSearchInputBox.setText("");
                switch2HistoryPage();
            }
        });

        // ???????????????????????????
        mCancelInputBtn.setOnClickListener(v -> {
            mSearchInputBox.setText("");
            // ????????????????????????????????????
            switch2HistoryPage();
        });

        // ?????????????????????????????????
        mSearchInputBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ?????????????????????
                // ??????????????????0???????????????
                // ????????????
                // ???????????????null????????? ??? ?????????????????????
                mCancelInputBtn.setVisibility(hasContent(true) ? View.VISIBLE : View.GONE);
                mSearchBtn.setText(hasContent(false) ? "??????" : "??????");
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mSearchInputBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH && mSearchPresenter != null) {
                String keyword = v.getText().toString().trim();
                if (TextUtils.isEmpty(keyword)) {
                    return false;
                }
                toSearch(keyword);
            }
            return false;
        });

        mHistoryDelete.setOnClickListener(v -> {
            // ????????????
            mSearchPresenter.delHistories();
        });

        mSearchResultAdapter.setOnListenItemClickListener(item -> {
            // ????????????
            String title = item.getTitle();
            // ????????????????????????????????????????????????????????????????????????
            String url = item.getCoupon_share_url();
            if (url == null) {
                url = item.getUrl();
            }
            String cover = item.getPict_url();
            // ?????? ticketPresenter ???????????????
            ITicketPresenter ticketPresenter = PresenterManager.getInstance().getTicketPresenter();
            ticketPresenter.getTicket(title, url, cover);
            startActivity(new Intent(getContext(), TicketActivity.class));
        });

        mRefreshContainer.setOnRefreshListener(new RefreshListenerAdapter() {
            @Override
            public void onLoadMore(TwinklingRefreshLayout refreshLayout) {
                if (mSearchPresenter != null) {
                    mSearchPresenter.loadMore();
                }
            }
        });
    }

    private void switch2HistoryPage() {
        setUpState(State.SUCCESS);
        mRefreshContainer.setVisibility(View.GONE);
        if (mSearchPresenter != null) {
            mSearchPresenter.getHistories();
        }
        if (mRecommendView.getContentSize() != 0) {
            mRecommendContainer.setVisibility(View.VISIBLE);
        } else {
            mRecommendContainer.setVisibility(View.GONE);
        }

    }

    boolean hasContent(boolean containSpace) {
        if (containSpace) {
            return mSearchInputBox.getText().toString().length() > 0;
        } else {
            return mSearchInputBox.getText().toString().trim().length() > 0;
        }
    }

    @Override
    protected void onRetryClick() {
        // ????????????
        if (mSearchPresenter != null) {
            LogUtils.d(this, "onRetryClick...");
            mSearchPresenter.research();
        }
    }

    @Override
    protected View loadRootView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return inflater.inflate(R.layout.fragment_search_layout, container, false);
    }

    @Override
    protected int getRootViewResId() {
        return R.layout.fragment_search;
    }

    @Override
    protected void release() {
        if (mSearchPresenter != null) {
            mSearchPresenter.unregisterViewCallback(this);
        }
    }

    @Override
    public void onHistoriesLoaded(Histories histories) {
        LogUtils.d(this, "histories --- > " + histories);
        if (histories == null || histories.getHistories().size() == 0) {
            mHistoryContainer.setVisibility(View.GONE);
        } else {
            mHistoryContainer.setVisibility(View.VISIBLE);
            mHistoryView.setTextList(histories.getHistories());
        }
    }

    @Override
    public void onHistoriesDeleted() {
        if (mSearchPresenter != null) {
            mSearchPresenter.getHistories();
        }
    }

    @Override
    public void onSearchSuccess(SearchResult result) {
        setUpState(State.SUCCESS);
        // ??????????????????????????????
        mRecommendContainer.setVisibility(View.GONE);
        mHistoryContainer.setVisibility(View.GONE);
        // ??????????????????
        mRefreshContainer.setVisibility(View.VISIBLE);
        // ????????????
        mSearchResultAdapter.setData(result);
    }

    @Override
    public void onMoreLoaded(SearchResult result) {
        List<SearchResult.DataDTO.TbkDgMaterialOptionalResponseDTO.ResultListDTO.MapDataDTO> resultData = result.getData().getTbk_dg_material_optional_response().getResult_list().getMap_data();
        mSearchResultAdapter.addData(resultData);
        ToastUtils.showToast("??????????????????");
        mRefreshContainer.finishLoadmore();
    }

    @Override
    public void onMoreLoadError() {
        mRefreshContainer.finishLoadmore();
        ToastUtils.showToast("??????????????????????????????...");
    }

    @Override
    public void onMoreLoadEmpty() {
        mRefreshContainer.finishLoadmore();
        ToastUtils.showToast("?????????????????????...");
    }

    @Override
    public void onRecommendWordLoaded(List<SearchRecommend.DataDTO> recommendWords) {
        List<String> recommendKeyWords = new ArrayList<>();
        for (SearchRecommend.DataDTO item : recommendWords) {
            LogUtils.d(this, "recommendWords size --  > " + recommendWords.get(0).getKeyword());
            recommendKeyWords.add(item.getKeyword());
        }
        if (recommendWords == null || recommendKeyWords.size() == 0) {
            mRecommendContainer.setVisibility(View.GONE);
        } else {
            mRecommendContainer.setVisibility(View.VISIBLE);
        }
        mRecommendView.setTextList(recommendKeyWords);
    }

    @Override
    public void onLoading() {
        setUpState(State.LOADING);
    }

    @Override
    public void onError() {
        setUpState(State.ERROR);
    }

    @Override
    public void onEmpty() {
        setUpState(State.EMPTY);
    }

    @Override
    public void onFlowItemClick(String text) {
        // ????????????
        toSearch(text);
    }

    private void toSearch(String text) {
        if (mSearchPresenter != null) {
            mSearchList.scrollToPosition(0);
            mSearchInputBox.setText(text);
            mSearchInputBox.setFocusable(true);
            mSearchInputBox.setSelection(text.length());
            mSearchPresenter.doSearch(text);
        }
    }
}

