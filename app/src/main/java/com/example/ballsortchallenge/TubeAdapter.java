package com.example.ballsortchallenge;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Stack;

public class TubeAdapter extends RecyclerView.Adapter<TubeAdapter.TubeViewHolder> {

    private Context context;
    private ArrayList<Stack<Integer>> tubes;
    private OnTubeClickListener listener;
    private int selectedPosition = -1;

    public interface OnTubeClickListener {
        void onTubeClick(int position);
    }

    public TubeAdapter(Context context, ArrayList<Stack<Integer>> tubes, OnTubeClickListener listener) {
        this.context = context;
        this.tubes = tubes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TubeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tube, parent, false);
        return new TubeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TubeViewHolder holder, int position) {
        holder.tubeLayout.removeAllViews();
        // Ensure vertical stacking: Lid -> Spacer -> Balls -> Buffer
        holder.tubeLayout.setOrientation(LinearLayout.VERTICAL);

        Stack<Integer> tube = tubes.get(position);

        // 1. Logic: Check if Tube is Sorted/Complete
        boolean isComplete = false;
        if (tube.size() == 4) {
            int firstColor = tube.get(0);
            isComplete = true;
            for (int ball : tube) {
                if (ball != firstColor) { isComplete = false; break; }
            }
        }

        // 2. Styling: Background Selection (U-Shape)
        if (position == selectedPosition) {
            holder.tubeLayout.setBackgroundResource(R.drawable.tube_border_selected);
        } else {
            holder.tubeLayout.setBackgroundResource(R.drawable.tube_open);
        }

        // 3. THE LID: Added first to stay at the very top
        if (isComplete) {
            View lid = new View(context);
            // Thick Lid: 12dp height
            LinearLayout.LayoutParams lidParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(12));
            lidParams.setMargins(dpToPx(4), dpToPx(2), dpToPx(4), 0);

            // High-contrast chrome gradient
            GradientDrawable silver = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF333333, 0xFFFFFFFF, 0xFF333333});
            silver.setCornerRadius(dpToPx(6));
            lid.setBackground(silver);

            holder.tubeLayout.addView(lid, lidParams);
        }

        // 4. THE SPACER: Takes up all empty space to push balls to bottom
        View spacer = new View(context);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        holder.tubeLayout.addView(spacer, spacerParams);

        // 5. DRAW BALLS (Top to Bottom order in Layout)
        int ballSize = dpToPx(40);
        int margin = dpToPx(4);

        for (int i = tube.size() - 1; i >= 0; i--) {
            TextView ball = new TextView(context);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(getBallColor(tube.get(i)));
            ball.setBackground(shape);

            ball.setGravity(Gravity.CENTER);
            ball.setText(getEmoticon(tube.get(i)));
            ball.setTextSize(18);
            ball.setTextColor(Color.WHITE);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ballSize, ballSize);
            params.gravity = Gravity.CENTER_HORIZONTAL;
            params.topMargin = margin;
            ball.setLayoutParams(params);

            // Selection Lift Animation
            if (position == selectedPosition && i == tube.size() - 1) {
                ball.setTranslationY(dpToPx(-22));
            } else {
                ball.setTranslationY(0f);
            }
            holder.tubeLayout.addView(ball);
        }

        // 6. THE BOTTOM BUFFER: The "Shelf"
        // This view forces the balls to stay slightly above the tube's base
        View bottomBuffer = new View(context);
        LinearLayout.LayoutParams bufferParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(14)); // 14dp gap from bottom
        holder.tubeLayout.addView(bottomBuffer, bufferParams);

        holder.tubeLayout.setOnClickListener(v -> listener.onTubeClick(position));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private int getBallColor(int id) {
        int[] colors = {0xFFF44336, 0xFF2196F3, 0xFF4CAF50, 0xFFFFEB3B, 0xFF9C27B0, 0xFFFF9800, 0xFF00BCD4, 0xFF795548, 0xFFE91E63};
        return colors[(id - 1) % colors.length];
    }

    private String getEmoticon(int id) {
        String[] emojis = {"😊", "😎", "😡", "😱", "😴", "🤔", "🤩", "🤮", "🤡"};
        return emojis[(id - 1) % emojis.length];
    }

    @Override
    public int getItemCount() {
        return tubes.size();
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    static class TubeViewHolder extends RecyclerView.ViewHolder {
        LinearLayout tubeLayout;
        TubeViewHolder(View itemView) {
            super(itemView);
            tubeLayout = itemView.findViewById(R.id.tubeLayout);
        }
    }
}