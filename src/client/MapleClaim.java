/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.util.Date;
import server.quest.MapleQuest;

/**
 *
 * @author cccv
 */
public class MapleClaim {

    public MapleCharacter chr;
    public MapleQuestStatus ban, day, totalcount, count;
    public int daymaxcount, weekmaxcount;

    public MapleClaim(final MapleCharacter c) {
        this.chr = c;
        ban = chr.getQuestNAdd(MapleQuest.getInstance(6574890)); // 허위신고 제재가 되었는가.
        day = chr.getQuestNAdd(MapleQuest.getInstance(6574891)); // 하루
        totalcount = chr.getQuestNAdd(MapleQuest.getInstance(6574892)); // 토탈하루
        count = chr.getQuestNAdd(MapleQuest.getInstance(6574893)); // 하루 횟수
        daymaxcount = 15;
        weekmaxcount = 45;
    }

    public final void ClearClaim() {
        ban.setCustomData("0");
        day.setCustomData("8");
        count.setCustomData("0");
        totalcount.setCustomData("0");
    }

    public final void ResetClaim() {
        if (ban.getCustomData() == null) {
            ban.setCustomData("0");
        }
        if (day.getCustomData() == null) {
            day.setCustomData("8");
        }
        if (count.getCustomData() == null) {
            count.setCustomData("0");
        }
        if (totalcount.getCustomData() == null) {
            totalcount.setCustomData("0");
        }

        Date time = new Date(System.currentTimeMillis());

        if (Integer.parseInt(day.getCustomData()) != time.getDay()) {
            day.setCustomData(time.getDay() + "");
            if (time.getDay() == 0) {
                totalcount.setCustomData("0");
            }
        }
    }

    public final boolean banCheckClaim() {
        return (Integer.parseInt(ban.getCustomData()) != 0);
    }

    public final boolean ClaimDayMaxCount() {
        return (Integer.parseInt(count.getCustomData()) < daymaxcount);
    }

    public final boolean ClaimWeekMaxCount() {
        return (Integer.parseInt(totalcount.getCustomData()) < weekmaxcount);
    }

    public final int ClaimDayCount() {
        return Integer.parseInt(count.getCustomData());
    }

    public final int ClaimWeekCount() {
        return Integer.parseInt(totalcount.getCustomData());
    }

    public final void PlusClaimCount() {
        totalcount.setCustomData(Integer.parseInt(totalcount.getCustomData()) + 1 + "");
        count.setCustomData(Integer.parseInt(count.getCustomData()) + 1 + "");
    }

}
