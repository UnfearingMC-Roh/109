/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

import client.MapleCharacter;
import client.PlayerStats;
import static client.PlayerStats.getSkillByJob;
import client.Skill;
import client.SkillFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import provider.MapleData;
import provider.MapleDataTool;
import provider.MapleDataType;

/**
 *
 * @author jmk50
 */
public class CustomSetItemInfo {
    private final int[] 
            bossDAMrSkillID = {200, 210},
            ignoreTargetDEFSkillID = {201, 211}
    ;
    
    private int setEffectID;
    private String setItemName;
    private int setCompleteCount;
    private boolean weaponDesc;
    private List<Integer> itemID;
    private Map<Integer, CustomSetEffect> effect;
    
    public CustomSetItemInfo(MapleData data) {
        this.setEffectID = Integer.parseInt(data.getName());
        this.setItemName = MapleDataTool.getString("setItemName", data, null);
        this.setCompleteCount = MapleDataTool.getIntConvert("completeCount", data, 0);
        this.weaponDesc = MapleDataTool.getString("weapon", data.getChildByPath("Desc"), null) != null;
        
        this.itemID = new ArrayList<>();
        MapleData itemData = data.getChildByPath("ItemID");
        if (itemData != null) {
            for (MapleData item : itemData) {
                try {
                    if (item.getType() != MapleDataType.INT) {
                        for (MapleData leve : item) {
                            this.itemID.add(MapleDataTool.getInt(leve));
                        }
                    } else {
                        this.itemID.add(MapleDataTool.getInt(item));
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        this.effect = new HashMap<>();
        MapleData effectData = data.getChildByPath("Effect");
        if (effectData != null) {
            for (MapleData effect : effectData) {
                try {
                    int count = Integer.parseInt(effect.getName());
                    int bossDAMr = MapleDataTool.getIntConvert("bossDAMr", effect, 0);
                    int ignoreTargetDEF = MapleDataTool.getIntConvert("ignoreDEF", effect, 0);
                    CustomSetEffect setEffect = new CustomSetEffect(bossDAMr, ignoreTargetDEF);
                    this.effect.put(count, setEffect);
                } catch (NumberFormatException e) {
                }
            }
        }
    }
    
    public int getSetItemCount(MapleCharacter chr) {
        int equippedCount = 0;
        for (int setItemID : this.itemID) {
            if (chr.hasEquipped(setItemID)) {
                equippedCount++;
            }
        }
        return equippedCount;
    }
    
    public boolean checkSetItemInfo(int equippedCount) {
        int completeCount = this.weaponDesc && this.setCompleteCount > 0 ? this.setCompleteCount : this.itemID.size();
        return equippedCount == completeCount;
    }
    
    public void setItemEffect(MapleCharacter chr) {
        int equippedCount = getSetItemCount(chr);
        if (equippedCount > 0) {
            int curCount = 0;
            int bossDAMr = 0, ignoreTargetDEF = 0;
            for (Entry<Integer, CustomSetEffect> entry : this.effect.entrySet()) {
                int count = entry.getKey();
                if (equippedCount >= count && count >= curCount) {
                    curCount = count;
                    CustomSetEffect eff = entry.getValue();
                    bossDAMr = eff.getBossDAMr();
                    ignoreTargetDEF = eff.getIgnoreTargetDEF();
                }
            }
            boolean teachSkill = false;
            for (int i = 0; i < bossDAMrSkillID.length; i++) {
                if (teachSkill) break;
                int bossSkillID = getSkillByJob(this.bossDAMrSkillID[i], chr.getJob());
                int defSkillID = getSkillByJob(this.ignoreTargetDEFSkillID[i], chr.getJob());
                
                if (bossDAMr > 0) {
                    if (chr.getStat().currentSetEffectSkillID.contains(bossSkillID))
                        continue;
                        
                    Skill skill = SkillFactory.getSkill(bossSkillID);
                    if (skill != null) {
                        MapleStatEffect skillEffect = skill.getEffect(bossDAMr);
                        chr.changeSkillLevel_Skip(skill, (byte) bossDAMr, (byte) bossDAMr, true);
                        chr.getStat().currentSetEffectSkillID.add(bossSkillID);
                        teachSkill = true;
                        if (skillEffect != null) {
                            chr.getStat().bossdam_r += skillEffect.getDAMRate();
                        }
                    }
                } else {
                    if (!chr.getStat().currentSetEffectID.contains(setEffectID) && chr.getStat().currentSetEffectSkillID.contains(bossSkillID))
                        continue;
                    
                    chr.changeSkillLevel_Skip(SkillFactory.getSkill(bossSkillID), (byte) -1, (byte) -1, true);
                }
                if (ignoreTargetDEF > 0) {
                    if (chr.getStat().currentSetEffectSkillID.contains(defSkillID))
                        continue;
                    
                    Skill skill = SkillFactory.getSkill(defSkillID);
                    if (skill != null) {
                        MapleStatEffect skillEffect = skill.getEffect(ignoreTargetDEF);
                        chr.changeSkillLevel_Skip(skill, (byte) ignoreTargetDEF, (byte) ignoreTargetDEF, true);
                        chr.getStat().currentSetEffectSkillID.add(defSkillID);
                        teachSkill = true;
                        if (skillEffect != null) {
                            chr.getStat().ignoreTargetDEF += skillEffect.getIgnoreMob();
                        }
                    }
                } else {
                    if (!chr.getStat().currentSetEffectID.contains(setEffectID) && chr.getStat().currentSetEffectSkillID.contains(defSkillID))
                        continue;
                        
                    chr.changeSkillLevel_Skip(SkillFactory.getSkill(defSkillID), (byte) -1, (byte) -1, true);
                }
                if (teachSkill)
                    chr.getStat().currentSetEffectID.add(setEffectID);
            }
//            System.err.println(Arrays.toString(chr.getStat().currentSetEffectID.toArray()));
//            System.err.println(Arrays.toString(chr.getStat().currentSetEffectSkillID.toArray()));
        }
    }
}
