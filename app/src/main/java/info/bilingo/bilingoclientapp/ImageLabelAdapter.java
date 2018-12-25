package info.bilingo.bilingoclientapp;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.api.services.vision.v1.model.LocalizedObjectAnnotation;

import java.util.List;

public class ImageLabelAdapter extends RecyclerView.Adapter<ImageLabelAdapter.ItemHolder> {
    private Context mContext;
    private List<LocalizedObjectAnnotation> mList;

    public static class ItemHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView itemName;
        TextView itemAccuracy;

        public ItemHolder(View v) {
            super(v);
            cv = v.findViewById(R.id.cv);
            itemName = v.findViewById(R.id.item_name);
            itemAccuracy = v.findViewById(R.id.item_accuracy);
        }

        public void bindAction(LocalizedObjectAnnotation localizedObjectAnnotation) {
            itemName.setText(localizedObjectAnnotation.getName());
            itemAccuracy.setText(localizedObjectAnnotation.getScore().toString());
        }
    }

    public ImageLabelAdapter() {

    }

    public void setList(List<LocalizedObjectAnnotation> list) {
        mList = list;
        notifyDataSetChanged();
    }

    @Override
    public ImageLabelAdapter.ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        return new ItemHolder(LayoutInflater.from(mContext).inflate(R.layout.item_row, parent, false));
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {
        LocalizedObjectAnnotation currentItem = mList.get(position);
        holder.bindAction(currentItem);
    }

    @Override
    public int getItemCount() {
        if (mList == null) {
            return 0;
        }

        return mList.size();
    }
}
