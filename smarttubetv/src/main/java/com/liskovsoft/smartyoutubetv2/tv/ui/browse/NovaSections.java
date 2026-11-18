package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;

/**
 * NovaTV: splits the existing browse sections into navigation (side rail) and categories (chips).<br/>
 * Category sections are still regular sections (same data, same settings); they are just shown
 * as chips under the header instead of in the rail.
 */
public final class NovaSections {
    private static final int[] CHIP_SECTIONS = {
            MediaGroup.TYPE_MUSIC,
            MediaGroup.TYPE_LIVE,
            MediaGroup.TYPE_GAMING,
            MediaGroup.TYPE_NEWS,
            MediaGroup.TYPE_SPORTS,
            MediaGroup.TYPE_KIDS_HOME,
            MediaGroup.TYPE_SHORTS,
    };

    private NovaSections() {
    }

    /**
     * Section shown as a chip and hidden from the side rail.
     */
    public static boolean isChipSection(long sectionId) {
        for (int id : CHIP_SECTIONS) {
            if (id == sectionId) {
                return true;
            }
        }

        return false;
    }

    /**
     * The "All" chip. Home stays in the rail too.
     */
    public static boolean isAllChip(long sectionId) {
        return sectionId == MediaGroup.TYPE_HOME;
    }

    /**
     * Chips row is visible while one of these sections is open.
     */
    public static boolean showsChips(long sectionId) {
        return isAllChip(sectionId) || isChipSection(sectionId);
    }
}
