package com.liskovsoft.smartyoutubetv2.tv.ui.browse.video;

import android.os.Bundle;
import android.view.View;

import androidx.leanback.widget.VerticalGridView;

import com.liskovsoft.smartyoutubetv2.tv.R;

public class ShortsGridFragment extends VideoGridFragment {
    @Override
    protected boolean isShorts() {
        return true;
    }

    /**
     * NovaTV: shorts cards are taller than half the screen, so the stock alignment (selected row centered)
     * pushed them up over the clock and date. The selected row now starts right below the header.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        VerticalGridView grid = getBrowseGrid();
        if (grid != null) {
            grid.setItemAlignmentOffset(0);
            grid.setItemAlignmentOffsetPercent(VerticalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
            grid.setWindowAlignmentOffset(getResources().getDimensionPixelSize(R.dimen.nova_shorts_grid_top));
            grid.setWindowAlignmentOffsetPercent(VerticalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
            grid.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
        }
    }
}
