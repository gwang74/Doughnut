package com.doughnut.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.android.jtblk.client.bean.AccountRelations;
import com.android.jtblk.client.bean.Line;
import com.doughnut.R;
import com.doughnut.adapter.BaseRecyclerViewHolder;
import com.doughnut.config.AppConfig;
import com.doughnut.utils.GsonUtil;
import com.doughnut.utils.Util;
import com.doughnut.utils.ViewUtil;
import com.doughnut.view.RecyclerViewSpacesItemDecoration;
import com.doughnut.view.TitleBar;
import com.doughnut.wallet.WConstant;
import com.doughnut.wallet.WalletManager;
import com.doughnut.wallet.WalletSp;
import com.jccdex.rpc.base.JCallback;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.doughnut.config.AppConfig.getContext;


public class TransferTokenActivity extends BaseActivity implements TitleBar.TitleBarClickListener {

    private TitleBar mTitleBar;
    private EditText mEdtSearch;
    private RecyclerView mRecyclerView;
    private String mCurrentWallet;
    private TransferTokenAdapter mAdapter;
    private ArrayList<Line> dataList;
    private ArrayList<Line> dataListCopy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_currency);
        dataList = new ArrayList<>();
        dataListCopy = new ArrayList<>();
        mCurrentWallet = WalletSp.getInstance(this, "").getCurrentWallet();
        initView();
    }

    private void initView() {
        mTitleBar = findViewById(R.id.title_bar);
        mTitleBar.setLeftDrawable(R.drawable.ic_back);
        mTitleBar.setLeftTextColor(R.color.white);
        mTitleBar.setTitleTextColor(R.color.color_detail_address);
        mTitleBar.setBackgroundColor(getResources().getColor(R.color.common_blue));
        mTitleBar.setTitle(getString(R.string.title_token_choose));
        mTitleBar.setTitleBarClickListener(this);

        mEdtSearch = findViewById(R.id.edit_search);

        final SpannableString ss = new SpannableString(getString(R.string.tv_token_search));
        Drawable d = getResources().getDrawable(R.drawable.ic_search);
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
        ss.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        mEdtSearch.setHint(ss);
        mEdtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                dataList.clear();
                Pattern p = Pattern.compile(s.toString().toUpperCase());
                for (int i = 0; i < dataListCopy.size(); i++) {
                    Line line = dataListCopy.get(i);
                    Matcher matcher = p.matcher(line.getCurrency().toUpperCase());
                    if (matcher.find()) {
                        dataList.add(line);
                    }
                }
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mAdapter = new TransferTokenAdapter();
        mRecyclerView = findViewById(R.id.view_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new RecyclerViewSpacesItemDecoration(this, 5));
        getTransferToken();
    }

    @Override
    public void onLeftClick(View v) {
        finish();
    }


    @Override
    public void onRightClick(View v) {

    }

    @Override
    public void onMiddleClick(View v) {

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, TransferTokenActivity.class);
        intent.addFlags(context instanceof BaseActivity ? 0 : Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    class TransferTokenAdapter extends RecyclerView.Adapter<TransferTokenAdapter.VH> {
        class VH extends BaseRecyclerViewHolder {
            LinearLayout mLayoutItem;
            ImageView mImgTokenIcon;
            TextView mTvTokenName;
            TextView mTvCNY, mTvTokenCount, mTvTokenFreeze;
            String tokenName;

            public VH(View itemView) {
                super(itemView);
                mLayoutItem = itemView.findViewById(R.id.layout_item);
                mLayoutItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setTransferToken(tokenName);
                        TransferTokenActivity.this.finish();
                    }
                });
                mImgTokenIcon = itemView.findViewById(R.id.token_icon);
                mTvTokenName = itemView.findViewById(R.id.token_name);
                mTvCNY = itemView.findViewById(R.id.tv_balance_cny);
                mTvTokenCount = itemView.findViewById(R.id.token_count);
                mTvTokenFreeze = itemView.findViewById(R.id.token_freeze);
            }
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = ViewUtil.inflatView(parent.getContext(), parent, R.layout.wallet_token_item_view, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            Line item = dataList.get(position);
            String currency = item.getCurrency();
            holder.mTvTokenName.setText(currency);
            holder.tokenName = currency;
            ViewUtil.EllipsisTextView(holder.mTvTokenName);
            holder.mImgTokenIcon.setImageResource(Util.getTokenIcon(currency));
            try {
                String balance = Util.formatAmount(item.getBalance(), 2);
                String balanceFreeze = Util.formatAmount(item.getLimit(), 2);
                BigDecimal sum = new BigDecimal(balance).add(new BigDecimal(balanceFreeze));
                if (TextUtils.equals(WConstant.CURRENCY_CNY, currency)) {
                    holder.mTvCNY.setText(Util.formatAmount(sum.stripTrailingZeros().toPlainString(), 2));
                } else {
                    WalletManager.getInstance(getContext()).getTokenPrice(currency, new JCallback() {
                        @Override
                        public void onResponse(String code, String response) {
                            if (TextUtils.equals(code, WConstant.SUCCESS_CODE)) {
                                GsonUtil res = new GsonUtil(response);
                                GsonUtil data = res.getArray("data");
                                if (data.isValid()) {
                                    // SWT当前价
                                    BigDecimal cur = new BigDecimal(data.getString(1, "0"));
                                    // 计算SWT总价值
                                    BigDecimal value = sum.multiply(cur, new MathContext(2));
                                    AppConfig.postOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            holder.mTvCNY.setText(String.format("%.2f", value));
                                        }
                                    });
                                }
                            } else {
                                AppConfig.postOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        holder.mTvCNY.setText("0.00");
                                    }
                                });
                            }
                        }

                        @Override
                        public void onFail(Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                holder.mTvTokenCount.setText(balance);
                holder.mTvTokenFreeze.setText(balanceFreeze);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            if (dataList != null) {
                return dataList.size();
            }
            return 0;
        }

    }

    /**
     * 获取可转账的token
     */
    private void getTransferToken() {
        WalletManager walletManager = WalletManager.getInstance(TransferTokenActivity.this);
        // 取得钱包资产
        AccountRelations accountRelations = walletManager.getBalance(mCurrentWallet);
        if (accountRelations != null) {
            dataList.clear();
            dataList.addAll(accountRelations.getLines());
        }

        // 排除余额为零的token
        try {
            for (int i = dataList.size() - 1; i >= 0; i--) {
                Line line = dataList.get(i);
                BigDecimal balance = new BigDecimal(line.getBalance());
                if (balance.compareTo(BigDecimal.ZERO) == 0) {
                    dataList.remove(i);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.sort(dataList, new Comparator<Line>() {
            @Override
            public int compare(Line o1, Line o2) {
                String cur1 = o1.getCurrency();
                String cur2 = o2.getCurrency();
                boolean b1 = Util.isStartWithNumber(cur1);
                boolean b2 = Util.isStartWithNumber(cur2);
                if (b1 && !b2) {
                    return 1;
                } else if (!b1 && b2) {
                    return -1;
                } else {
                    return cur1.compareTo(cur2);
                }
            }
        });

        dataListCopy = (ArrayList<Line>) dataList.clone();
    }

    /**
     * 保存选择的币种
     *
     * @param token
     */
    public void setTransferToken(String token) {
        // 本地保存tokens
        String fileName = getPackageName() + "_tokens";
        SharedPreferences sharedPreferences = getSharedPreferences(fileName, Context.MODE_PRIVATE);
        String tokens = sharedPreferences.getString("tokens", "");
        if (TextUtils.isEmpty(tokens)) {
            return;
        }
        Map<String, String> tokenMap = JSON.parseObject(tokens, Map.class);
        fileName = getPackageName() + "_transfer_token";
        sharedPreferences = getSharedPreferences(fileName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", token);
        editor.putString("issue", tokenMap.get(token));
        editor.apply();
    }
}