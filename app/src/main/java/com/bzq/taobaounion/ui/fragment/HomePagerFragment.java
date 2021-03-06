package com.bzq.taobaounion.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.bzq.taobaounion.R;
import com.bzq.taobaounion.base.BaseFragment;
import com.bzq.taobaounion.model.DTO.Categories;
import com.bzq.taobaounion.model.DTO.HomePagerContent;
import com.bzq.taobaounion.presenter.ICategoryPagerPresenter;
import com.bzq.taobaounion.presenter.ITicketPresenter;
import com.bzq.taobaounion.ui.activity.TicketActivity;
import com.bzq.taobaounion.ui.adapter.HomePagerContentAdapter;
import com.bzq.taobaounion.ui.adapter.LooperPagerAdapter;
import com.bzq.taobaounion.ui.custom.AutoLoopViewPager;
import com.bzq.taobaounion.utils.Constants;
import com.bzq.taobaounion.utils.LogUtils;
import com.bzq.taobaounion.utils.PresenterManager;
import com.bzq.taobaounion.utils.SizeUtils;
import com.bzq.taobaounion.utils.ToastUtils;
import com.bzq.taobaounion.view.ICategoryPagerCallback;
import com.lcodecore.tkrefreshlayout.RefreshListenerAdapter;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;
import com.lcodecore.tkrefreshlayout.views.TbNestedScrollView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import butterknife.BindView;

/**
 * @author Gerkey
 * Created on 2021/7/2
 */
public class HomePagerFragment extends BaseFragment implements ICategoryPagerCallback {

    private ICategoryPagerPresenter mCategoryPagePresenter;
    private int mMaterialId;
    private HomePagerContentAdapter mContentAdapter;
    private LooperPagerAdapter mLooperPagerAdapter;

    @BindView(R.id.home_pager_contain_list)
    public RecyclerView mContentList;

    @BindView(R.id.looper_pager)
    public AutoLoopViewPager looperPager;

    @BindView(R.id.home_pager_title)
    public TextView currentCategoryTitleTv;

    @BindView(R.id.looper_point_container)
    public LinearLayout LooperPointContainer;

    @BindView(R.id.home_pager_nested_scroller)
    public TbNestedScrollView homePagerNestedView;

    @BindView(R.id.home_pager_header_container)
    public LinearLayout homeHeaderContainer;

    @BindView(R.id.home_pager_refresh)
    public TwinklingRefreshLayout mTwinklingRefreshLayout;

    @BindView(R.id.home_pager_parent)
    public LinearLayout homePagerParent;

    public static HomePagerFragment newInstance(Categories.DataDTO category) {
        HomePagerFragment homePagerFragment = new HomePagerFragment();
        // ?????? Bundle ???????????????
        Bundle bundle = new Bundle();
        bundle.putString(Constants.KEY_HOME_PAGER_TITLE, category.getTitle());
        bundle.putInt(Constants.KEY_HOME_PAGER_MATERIAL_ID, category.getId());
        homePagerFragment.setArguments(bundle);
        return homePagerFragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        // ???????????????????????? loop
        looperPager.startLoop();
    }

    @Override
    public void onPause() {
        super.onPause();
        // ??????????????????????????? loop
        looperPager.stopLoop();
    }

    @Override
    protected void initView(View rootView) {
        // RecyclerView ??????????????????
        // 1.???????????????
        mContentAdapter = new HomePagerContentAdapter();
        // 2.???????????????
        mContentList.setAdapter(mContentAdapter);
        // 3.?????????????????????
        mContentList.setLayoutManager(new LinearLayoutManager(getContext()));
        // Adapter ?????????
        mContentList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull @NotNull Rect outRect, @NonNull @NotNull View view, @NonNull @NotNull RecyclerView parent, @NonNull @NotNull RecyclerView.State state) {
                outRect.bottom = SizeUtils.dp2px(getContext(), 2);
                outRect.top = SizeUtils.dp2px(getContext(), 2);
            }
        });
        // ViewPager
        // 1.???????????????
        mLooperPagerAdapter = new LooperPagerAdapter();
        // 2.???????????????
        looperPager.setAdapter(mLooperPagerAdapter);

        // ????????? Refresh ???????????????
        mTwinklingRefreshLayout.setEnableRefresh(false);
        mTwinklingRefreshLayout.setEnableLoadmore(true);
    }

    @Override
    protected void initListener() {
        mLooperPagerAdapter.setOnLooperPageClickListener(new LooperPagerAdapter.OnLooperPageClickListener() {
            @Override
            public void onLooperItemClick(HomePagerContent.DataDTO item) {
                // ?????????????????????
                 LogUtils.d(HomePagerFragment.this, "looper item click --- > " + item.getTitle());
                handleItemClick(item);
            }
        });

        mContentAdapter.setOnListenItemClickListener(new HomePagerContentAdapter.OnListenItemClickListener() {
            @Override
            public void onItemClick(HomePagerContent.DataDTO item) {
                // ????????????????????????
                LogUtils.d(HomePagerFragment.this, "list item click ---- > " + item.getTitle());
                handleItemClick(item);
            }
        });

        // ??????????????????
        homePagerParent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (homeHeaderContainer == null) {
                    return;
                }
                int headerHeight = homeHeaderContainer.getMeasuredHeight();
                homePagerNestedView.setHeaderHeight(headerHeight);
                int measuredHeight = homePagerParent.getMeasuredHeight();
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mContentList.getLayoutParams();
                layoutParams.height = measuredHeight;
                mContentList.setLayoutParams(layoutParams);
                if (measuredHeight != 0) {
                    // ??????????????????????????? Listener ????????? ViewHolder?????????????????????
                    homePagerParent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });

        looperPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (mLooperPagerAdapter.getDataSize() == 0) {
                    return;
                }
                int targetPosition = position % mLooperPagerAdapter.getDataSize();
                // ???????????????
                upDataLooperIndicator(targetPosition);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mTwinklingRefreshLayout.setOnRefreshListener(new RefreshListenerAdapter() {
            @Override
            public void onLoadMore(TwinklingRefreshLayout refreshLayout) {
                if (mCategoryPagePresenter != null) {
                    mCategoryPagePresenter.loadMore(mMaterialId);
                }
            }
        });
    }

    @Override
    protected void initPresenter() {
        mCategoryPagePresenter = PresenterManager.getInstance().getCategoryPagerPresenter();
        mCategoryPagePresenter.registerViewCallback(this);
    }

    @Override
    protected void loadData() {
        Bundle arguments = getArguments();
        // ????????????????????????????????? ViewPager ?????????????????????????????????(??????????????????)
        String title = arguments.getString(Constants.KEY_HOME_PAGER_TITLE);
        mMaterialId = arguments.getInt(Constants.KEY_HOME_PAGER_MATERIAL_ID);
        if (mCategoryPagePresenter != null) {
            mCategoryPagePresenter.getContentByCategoryId(mMaterialId);
        }
        currentCategoryTitleTv.setText(title);
    }

    @Override
    public void onContentLoaded(List<HomePagerContent.DataDTO> contents) {
        // ????????????????????????
        mContentAdapter.setData(contents);
        setUpState(State.SUCCESS);
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
    public int getCategoryId() {
        return mMaterialId;
    }

    @Override
    public void onLoadMoreError() {
        if (mTwinklingRefreshLayout != null) {
            ToastUtils.showToast("??????????????????????????????");
            mTwinklingRefreshLayout.finishLoadmore();
        }
    }

    @Override
    public void onLoadMoreEmpty() {
        if (mTwinklingRefreshLayout != null) {
            ToastUtils.showToast("????????????????????????");
            mTwinklingRefreshLayout.finishLoadmore();
        }
    }

    @Override
    public void onLoadMoreLoaded(List<HomePagerContent.DataDTO> contents) {
        // ???????????????????????????????????????????????????
        mContentAdapter.addData(contents);
        // ?????????????????????????????????????????????
        if (mTwinklingRefreshLayout != null) {
            mTwinklingRefreshLayout.finishLoadmore();
            ToastUtils.showToast("????????????");
        }
    }

    @Override
    public void onLooperListLoaded(List<HomePagerContent.DataDTO> contents) {
        mLooperPagerAdapter.setData(contents);
        // ?????????????????????????????????????????????
        int dx = (Integer.MAX_VALUE / 2) % contents.size();
        int targetCenterPosition = (Integer.MAX_VALUE / 2) - dx;
        // ????????????????????????????????????
        looperPager.setCurrentItem(targetCenterPosition);

        // ???????????????
        LooperPointContainer.removeAllViews();
        Context context = getContext();
        for (int i = 0; i < contents.size(); i++) {
            View point = new View(context);
            int size = SizeUtils.dp2px(context, 8);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(size, size);
            layoutParams.leftMargin = SizeUtils.dp2px(context, 5);
            layoutParams.rightMargin = SizeUtils.dp2px(context, 5);
            point.setLayoutParams(layoutParams);
            if (i == 0) {
                point.setBackgroundResource(R.drawable.shape_indicator_point_selected);
            } else {
                point.setBackgroundResource(R.drawable.shape_indicator_point_normal);
            }
            LooperPointContainer.addView(point);
        }
    }

    @Override
    protected void release() {
        if (mCategoryPagePresenter != null) {
            mCategoryPagePresenter.unregisterViewCallback(this);
        }
    }

    @Override
    protected int getRootViewResId() {
        return R.layout.fragmeng_home_pager;
    }

    /**
     * ???????????????
     *
     * @param targetPosition
     */
    private void upDataLooperIndicator(int targetPosition) {
        for (int i = 0; i < LooperPointContainer.getChildCount(); i++) {
            View point = LooperPointContainer.getChildAt(i);
            if (i == targetPosition) {
                point.setBackgroundResource(R.drawable.shape_indicator_point_selected);
            } else {
                point.setBackgroundResource(R.drawable.shape_indicator_point_normal);
            }
        }
    }

    private void handleItemClick(HomePagerContent.DataDTO item) {
        // ????????????
        String title = item.getTitle();
        // ????????????????????????????????????????????????????????????????????????
        String url = item.getCoupon_click_url();
        if (TextUtils.isEmpty(url)) {
            url = item.getClick_url();
        }
        String cover = item.getPict_url();
        // ?????? ticketPresenter ???????????????
        ITicketPresenter ticketPresenter = PresenterManager.getInstance().getTicketPresenter();
        ticketPresenter.getTicket(title, url, cover);
        startActivity(new Intent(getContext(), TicketActivity.class));
    }
}

