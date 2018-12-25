package info.bilingo.bilingoclientapp;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.api.services.vision.v1.model.EntityAnnotation;

import java.util.List;

public class ImageLabelAdapter extends RecyclerView.Adapter<ImageLabelAdapter.ItemHolder> {
    private Context mContext;
    private List<EntityAnnotation> mList;

    public static class ItemHolder extends RecyclerView.ViewHolder {
        public ItemHolder(View v) {
            super(v);
        }

        public void bindAction(EntityAnnotation entityAnnotation) {

        }
    }

    public ImageLabelAdapter() {

    }

    public void setList(List<EntityAnnotation> list) {
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
        EntityAnnotation currentItem = mList.get(position);
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
