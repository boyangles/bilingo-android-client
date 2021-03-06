package info.bilingo.bilingoclientapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.translate.Translate;
import com.google.api.services.translate.TranslateRequestInitializer;
import com.google.api.services.vision.v1.model.LocalizedObjectAnnotation;

import java.util.List;

public class ImageLabelAdapter extends RecyclerView.Adapter<ImageLabelAdapter.ItemHolder> {
    private Context mContext;
    private List<LocalizedObjectAnnotation> mList;
    private MainActivity mActivity;

    public static class ItemHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView itemName;
        TextView itemAccuracy;
        TextView itemBounds;
        MainActivity mainActivity;

        public ItemHolder(View v, MainActivity a) {
            super(v);
            mainActivity = a;

            cv = v.findViewById(R.id.cv);
            itemName = v.findViewById(R.id.item_name);
            itemAccuracy = v.findViewById(R.id.item_accuracy);
            itemBounds = v.findViewById(R.id.item_bounds);
        }

        public void bindAction(LocalizedObjectAnnotation localizedObjectAnnotation, int position) {
            itemName.setText(localizedObjectAnnotation.getName());
            itemAccuracy.setText(localizedObjectAnnotation.getScore().toString());

            if (mainActivity != null && mainActivity.mTranslations != null) {
                String sourceVal =
                        mainActivity.mTranslations.get(mainActivity.mSourceLang + ":::" + localizedObjectAnnotation.getName());
                if (sourceVal != null) {
                    itemName.setText(sourceVal);
                }

                String targetVal =
                        mainActivity.mTranslations.get(mainActivity.mTargetLang + ":::" + localizedObjectAnnotation.getName());
                if (targetVal != null) {
                    itemBounds.setText(targetVal);
                }
            }

            cv.setOnClickListener((View v)->{
                if (mainActivity != null && mainActivity.mBitmaps != null) {
                    Bitmap bm = mainActivity.mBitmaps.get("BM_OVERLAY:::" + localizedObjectAnnotation.getMid() + "___" + position);
                    if (bm != null && mainActivity.mMainImage != null) {
                        mainActivity.mMainImage.setImageBitmap(bm);
                    }
                }
            });
        }
    }

    public ImageLabelAdapter(MainActivity mainActivity) {
        mActivity = mainActivity;
    }

    public void setList(List<LocalizedObjectAnnotation> list) {
        mList = list;
        notifyDataSetChanged();
    }

    @Override
    public ImageLabelAdapter.ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        return new ItemHolder(LayoutInflater.from(mContext).inflate(R.layout.item_row, parent, false), mActivity);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {
        LocalizedObjectAnnotation currentItem = mList.get(position);
        holder.bindAction(currentItem, position);
    }

    @Override
    public int getItemCount() {
        if (mList == null) {
            return 0;
        }

        return mList.size();
    }
}
