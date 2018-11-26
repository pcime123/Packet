package com.sscctv.packetgenerator;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ItemViewHolder> {
    private ArrayList<ReceivePacket> mItems;

    RecyclerAdapter(ArrayList<ReceivePacket> items){
        mItems = items;
    }

    // 새로운 뷰 홀더 생성
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_receive,parent,false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {

        holder.mNum.setText(mItems.get(position).getNum());
        holder.mTime.setText(mItems.get(position).getTime());
        holder.mSourece.setText(mItems.get(position).getSource());
        holder.mDest.setText(mItems.get(position).getDest());
        holder.mProtocol.setText(mItems.get(position).getProtocol());
        holder.mLength.setText(mItems.get(position).getLength());
        holder.mData.setText(mItems.get(position).getData());

    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder{
        private TextView mNum;
        private TextView mTime;
        private TextView mSourece;
        private TextView mDest;
        private TextView mProtocol;
        private TextView mLength;
        private TextView mData;

        ItemViewHolder(View itemView) {
            super(itemView);
            mNum = itemView.findViewById(R.id.num);
            mTime = itemView.findViewById(R.id.time);
            mSourece = itemView.findViewById(R.id.source);
            mDest = itemView.findViewById(R.id.dest);
            mProtocol = itemView.findViewById(R.id.protocol);
            mLength = itemView.findViewById(R.id.length);
            mData = itemView.findViewById(R.id.data);
        }
    }

}
