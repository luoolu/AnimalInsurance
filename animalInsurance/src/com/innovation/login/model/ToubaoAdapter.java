package com.innovation.login.model;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.innovation.animalInsurance.R;
import com.innovation.bean.BaodanBean;
import com.innovation.bean.QueryBaodanBean;
import com.innovation.biz.Insured.LocaleInsuredSaveListener;
import com.innovation.base.Model;
import com.innovation.data.source.InsuredNos;
import com.innovation.location.AlertDialogManager;
import com.innovation.login.DatabaseHelper;
import com.innovation.login.ToubaoDetailActivity;
import com.innovation.login.view.HomeActivity;
import com.innovation.login.view.ISExist;
import com.innovation.utils.ConstUtils;
import com.innovation.utils.HttpRespObject;
import com.innovation.utils.HttpUtils;

import org.tensorflow.demo.DetectorActivity;
import com.innovation.base.GlobalConfigure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import okhttp3.FormBody;
import okhttp3.RequestBody;

import static android.support.constraint.Constraints.TAG;
import static com.innovation.base.InnApplication.getStringTouboaExtra;
import static com.innovation.login.model.MyUIUTILS.getString;
import static com.innovation.login.view.HomeActivity.isOPen;
import static org.tensorflow.demo.CameraConnectionFragment.collectNumberHandler;

public class ToubaoAdapter extends RecyclerView.Adapter<ToubaoAdapter.ViewHolder> {
    private DatabaseHelper databaseHelper;
    private ArrayList<QueryBaodanBean> newsBeanArrayList;
    private Context mContext;
    private RecyclerViewOnItemClickListener mOnItemClickListener;
    private RecyclerViewOnItemClickListener mOnItemClickListenerZiliao;
    //保单保存到本地的回调
    private LocaleInsuredSaveListener mLocaleInsuredSaveListener;

    private String result;
    private ToubaoTask mToubaoTask;
    private BaodanBean insurresp;
    private String errStr;
    Handler mHandler;
    private View view;
    private ISExist isExist;
    private String mtoubao_date;
    private String type;

    public ToubaoAdapter(ArrayList<QueryBaodanBean> newsBeanArrayList, Context mContext, Handler handler, DatabaseHelper databaseHelper) {
        this.newsBeanArrayList = newsBeanArrayList;
        this.mContext = mContext;
        this.databaseHelper = databaseHelper;
        mHandler = handler;
    }

    //创建新View，被LayoutManager所调用
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.toubao_item, viewGroup, false);
        ViewHolder vh = new ViewHolder(view);
        return vh;
    }

    //将数据与界面进行绑定的操作
    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
// TODO: 2018/8/9 By:LuoLu
//        if (position <= 0){
//            Log.i("TBadapter bViewHolder","position 为0");
//            return;
//        }

        viewHolder.toubao_num.setText(newsBeanArrayList.get(position).yanBiaoName.equals("")
                ? newsBeanArrayList.get(position).baodanNo :
                newsBeanArrayList.get(position).yanBiaoName);
//        viewHolder.toubao_num.setText(newsBeanArrayList.get(position).baodanNo);
//        Log.i(TAG,newsBeanArrayList.get(position).baodanNo);
        viewHolder.toubao_name.setText(newsBeanArrayList.get(position).name);
        QueryBaodanBean bean = newsBeanArrayList.get(viewHolder.getAdapterPosition());
        if (!newsBeanArrayList.get(position).createtime.equals("")) {
            mtoubao_date = newsBeanArrayList.get(position).createtime;
           Log.i(TAG,mtoubao_date);
            viewHolder.toubao_date.setText(mtoubao_date);
        }
        if (newsBeanArrayList.get(position).cardNo != null) {
            viewHolder.toubao_idcard.setText(newsBeanArrayList.get(position).cardNo);
        }
        //查询数据库，如果存在则隐藏添加到离线的图标
        if (newsBeanArrayList.size() > 0 && newsBeanArrayList.get(position).baodanNo != null) {
            List<LocalModelNongxian> localModels = databaseHelper.queryLocalDataFromBaodanNo(newsBeanArrayList.get(position).baodanNo);
            if (localModels != null&&localModels.size()>0) {
                viewHolder.selectedTag.setVisibility(View.GONE);
            } else {
                viewHolder.selectedTag.setVisibility(View.VISIBLE);
            }
        }
        viewHolder.toubao_detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra("baodanNumber", newsBeanArrayList.get(position).baodanNo);
                intent.putExtra("baodanId", String.valueOf(newsBeanArrayList.get(position).id));
                getStringTouboaExtra = newsBeanArrayList.get(position).baodanNo;
                intent.setClass(mContext, ToubaoDetailActivity.class);
                mContext.startActivity(intent);


            }
        });

        viewHolder.toubao_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOPen(mContext)) {
                    TreeMap query = new TreeMap<String, String>();
                    query.put("baodanNo", newsBeanArrayList.get(position).baodanNo);
                    getStringTouboaExtra = newsBeanArrayList.get(position).baodanNo;
                    mToubaoTask = new ToubaoTask(HttpUtils.INSUR_QUERY_URL, query);
                    mToubaoTask.execute((Void) null);
                }else {
//                    openGPS(mContext);
                    AlertDialogManager.showMessageDialog(mContext, "提示", getString(R.string.locationwarning), new AlertDialogManager.DialogInterface() {
                        @Override
                        public void onPositive() {
                            openGPS1(mContext);
                        }

                        @Override
                        public void onNegative() {

                        }
                    });

                }



            }
        });
        viewHolder.selectedTag.setOnClickListener((View v) -> {
            Log.i("====", newsBeanArrayList.size() + "");
            if (newsBeanArrayList.size() > 0) {
                //点击离线缓存添加到数据库
                int animalType = newsBeanArrayList.get(position).animalType;
                if (animalType == ConstUtils.ANIMAL_TYPE_CATTLE) {
                    type="牛";
                } else if (animalType== ConstUtils.ANIMAL_TYPE_DONKEY) {
                    type="驴";
                } else if (animalType == ConstUtils.ANIMAL_TYPE_PIG) {
                    type="猪";
                }
                Log.i("==animalType==", type+ "");
                if (!newsBeanArrayList.get(position).baodanTime.equals("")) {
                    mtoubao_date = newsBeanArrayList.get(position).baodanTime.substring(0, newsBeanArrayList.get(position).baodanTime.trim().length() - 11);
                }

//                LocalModel localModel = new LocalModel(newsBeanArrayList.get(position).baodanNo,
//                        newsBeanArrayList.get(position).name, newsBeanArrayList.get(position).cardNo, mtoubao_date,type);
//                databaseHelper.addLocalData(localModel);

                LocalModelNongxian localModelNongxian = new LocalModelNongxian(
                        newsBeanArrayList.get(position).baodanNo,
                        newsBeanArrayList.get(position).name,
                        newsBeanArrayList.get(position).cardNo,
                        mtoubao_date,type,newsBeanArrayList.get(position).yanBiaoName);
                databaseHelper.addLocalNongxianData(localModelNongxian);
                isExist.isexist(true);
                viewHolder.selectedTag.setVisibility(View.GONE);
            }
        });


    }

    InsuredNos insuredNos = new InsuredNos();

    //获取数据的数量
    @Override
    public int getItemCount() {
        // TODO: 2018/8/9 By:LuoLu  newsBeanArrayList.size()
        return newsBeanArrayList.size();
//        return 2;
    }

    public void setListner(ISExist isExist) {
        this.isExist = isExist;
    }

    //自定义的ViewHolder，持有每个Item的的所有界面元素
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView toubao_num;
        public TextView toubao_name;
        public TextView isnot_lipei;
        public TextView toubao_date;
        public TextView toubao_detail;
        public TextView toubao_continue;
        public TextView toubao_idcard;
        public TextView selectedTag;

        public ViewHolder(View view) {
            super(view);
            toubao_num = (TextView) view.findViewById(R.id.toubao_num);
            toubao_name = (TextView) view.findViewById(R.id.toubao_name);
            toubao_date = (TextView) view.findViewById(R.id.toubao_date);
            toubao_idcard = (TextView) view.findViewById(R.id.toubao_idcard);
            isnot_lipei = (TextView) view.findViewById(R.id.isnot_lipei);
            toubao_detail = (TextView) view.findViewById(R.id.toubao_upload);
            toubao_continue = (TextView) view.findViewById(R.id.toubao_continue);
            selectedTag = view.findViewById(R.id.selectedTag);
        }
    }


    /**
     * 设置点击事件
     */
    public void setRecyclerViewOnItemClickListener(RecyclerViewOnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    /**
     * 设置点击事件
     */
    public void setRecyclerViewOnItemClickListenerZiliao(RecyclerViewOnItemClickListener onItemClickListener) {
        this.mOnItemClickListenerZiliao = onItemClickListener;
    }

    /**
     * 点击事件接口
     */
    public interface RecyclerViewOnItemClickListener {
        void onItemClickListener(View view, int position);
    }

    public class ToubaoTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUrl;
        private final TreeMap<String, String> mQueryMap;

        ToubaoTask(String url, TreeMap map) {
            mUrl = url;
            mQueryMap = map;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            //  attempt authentication against a network service.
            try {
                FormBody.Builder builder = new FormBody.Builder();
                // Add Params to Builder
                for (TreeMap.Entry<String, String> entry : mQueryMap.entrySet()) {
                    builder.add(entry.getKey(), entry.getValue());
                }
                // Create RequestBody
                RequestBody formBody = builder.build();

                String response = HttpUtils.post(mUrl, formBody);
                Log.d(TAG, "response:" + response);

                if (HttpUtils.INSUR_QUERY_URL.equalsIgnoreCase(mUrl)) {
                    insurresp = (BaodanBean) HttpUtils.processResp_insurInfo(response, mUrl);
                    if (insurresp == null) {
//                        errStr = getString(R.string.error_newwork);
                        errStr = "请求错误！";
                        return false;
                    }
                    if (insurresp.status != HttpRespObject.STATUS_OK) {
                        errStr = insurresp.msg;
                        return false;
                    }
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                errStr = "服务器错误！";
                return false;
            }
            //  register the new account here.

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mToubaoTask = null;
            if (success & HttpUtils.INSUR_QUERY_URL.equalsIgnoreCase(mUrl)) {
                if (!insurresp.ibaodanNoReal.equals("")) {
                    Toast.makeText(mContext, "保单已审核，不能继续录入", Toast.LENGTH_SHORT).show();
                } else {
                    GlobalConfigure.model = Model.BUILD.value();
                    Intent intent = new Intent();
                    intent.putExtra("ToubaoTempNumber", insurresp.ibaodanNo);
                    intent.setClass(mContext, DetectorActivity.class);
                    mContext.startActivity(intent);
                    collectNumberHandler.sendEmptyMessage(2);
                }

            } else if (!success) {
                //  显示失败
                Log.d(TAG, errStr);
//                tv_info.setText(errStr);
            }
        }

        @Override
        protected void onCancelled() {
            mToubaoTask = null;
        }
    }
    private void openGPS1(Context mContext) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        HomeActivity activity = (HomeActivity) mContext;
        activity.startActivityForResult(intent, 1315);
    }

}
